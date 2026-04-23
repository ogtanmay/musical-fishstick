package com.vivoios.emojichanger.engine

import android.content.Context
import android.util.Log
import com.vivoios.emojichanger.db.EmojiPackDao
import com.vivoios.emojichanger.model.DownloadState
import com.vivoios.emojichanger.model.EmojiPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Handles downloading of iOS emoji packs from Google Drive.
 *
 * The two provided Google Drive file IDs are used to construct direct download URLs.
 * Google Drive direct download uses:
 * https://drive.google.com/uc?export=download&id=FILE_ID
 */
class PackDownloader(
    private val context: Context,
    private val packDao: EmojiPackDao
) {

    private val TAG = "PackDownloader"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            // Handle Google Drive large file confirmation cookie
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.body?.contentType()?.subtype?.contains("html") == true) {
                val body = response.peekBody(Long.MAX_VALUE).string()
                val confirmToken = extractConfirmToken(body)
                if (confirmToken != null) {
                    response.close()
                    val newRequest = request.newBuilder()
                        .url(request.url.newBuilder()
                            .addQueryParameter("confirm", confirmToken)
                            .build())
                        .build()
                    chain.proceed(newRequest)
                } else {
                    response
                }
            } else {
                response
            }
        }
        .build()

    private val downloadDir: File
        get() = File(context.filesDir, "emoji_packs").also { it.mkdirs() }

    /**
     * Download an emoji pack from Google Drive.
     * @param pack The EmojiPack to download
     * @param onProgress Callback with (downloadedBytes, totalBytes)
     */
    suspend fun downloadPack(
        pack: EmojiPack,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting download for pack: ${pack.name} (${pack.driveFileId})")

            packDao.updateDownloadProgress(pack.id, DownloadState.DOWNLOADING.name, 0L)

            val url = buildDownloadUrl(pack.driveFileId)
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val error = "Download failed: HTTP ${response.code}"
                Log.e(TAG, error)
                packDao.updateDownloadProgress(pack.id, DownloadState.FAILED.name, 0L)
                return@withContext Result.failure(Exception(error))
            }

            val totalBytes = response.body?.contentLength() ?: -1L
            val destFile = File(downloadDir, "${pack.id}.zip")

            response.body?.byteStream()?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var downloadedBytes = 0L
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        onProgress(downloadedBytes, totalBytes)

                        // Update DB progress every 100KB
                        if (downloadedBytes % (100 * 1024) < bytesRead) {
                            packDao.updateDownloadProgress(
                                pack.id,
                                DownloadState.DOWNLOADING.name,
                                downloadedBytes
                            )
                        }
                    }
                }
            }

            Log.i(TAG, "Download complete: ${destFile.absolutePath}")
            packDao.updateDownloadProgress(pack.id, DownloadState.EXTRACTING.name, totalBytes)

            // Extract the zip file
            val extractDir = extractPack(destFile, pack.id)
            destFile.delete() // Remove zip after extraction

            packDao.updateDownloadComplete(
                pack.id,
                true,
                extractDir.absolutePath,
                DownloadState.READY.name
            )

            Result.success(extractDir)

        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            packDao.updateDownloadProgress(pack.id, DownloadState.FAILED.name, 0L)
            Result.failure(e)
        }
    }

    private fun extractPack(zipFile: File, packId: String): File {
        val extractDir = File(downloadDir, packId).also { it.mkdirs() }
        Log.i(TAG, "Extracting ${zipFile.name} to ${extractDir.absolutePath}")

        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryFile = File(extractDir, entry.name)
                // Security: prevent zip path traversal
                if (!entryFile.canonicalPath.startsWith(extractDir.canonicalPath)) {
                    Log.w(TAG, "Skipping suspicious zip entry: ${entry.name}")
                    entry = zis.nextEntry
                    continue
                }
                if (entry.isDirectory) {
                    entryFile.mkdirs()
                } else {
                    entryFile.parentFile?.mkdirs()
                    FileOutputStream(entryFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }

        return extractDir
    }

    private fun buildDownloadUrl(fileId: String): String {
        return "https://drive.google.com/uc?export=download&id=$fileId"
    }

    private fun extractConfirmToken(html: String): String? {
        val regex = Regex("""confirm=([0-9A-Za-z_\-]+)""")
        return regex.find(html)?.groupValues?.get(1)
    }

    /**
     * Delete a downloaded pack's files from storage.
     */
    fun deletePack(packId: String) {
        val packDir = File(downloadDir, packId)
        packDir.deleteRecursively()
        Log.i(TAG, "Deleted pack files for: $packId")
    }

    /**
     * Returns the local directory for a downloaded pack.
     */
    fun getPackDirectory(packId: String): File = File(downloadDir, packId)

    companion object {
        // The two Google Drive file IDs from the requirements
        const val DRIVE_FILE_ID_1 = "1Z1aDsevwbnav__cYBH5WtzvxetcLOj4h"
        const val DRIVE_FILE_ID_2 = "1P3vNJJUjCChcjrR8QKEWTDckHM69lY2I"

        val DEFAULT_PACKS = listOf(
            EmojiPack(
                id = "ios_emoji_pack_1",
                name = "iOS Emoji Pack — Primary",
                description = "Latest iOS emoji set with maximum system compatibility",
                driveFileId = DRIVE_FILE_ID_1,
                downloadUrl = "https://drive.google.com/uc?export=download&id=$DRIVE_FILE_ID_1",
                iosVersion = "17.x",
                emojiCount = 3600
            ),
            EmojiPack(
                id = "ios_emoji_pack_2",
                name = "iOS Emoji Pack — Enhanced",
                description = "Enhanced iOS emoji set optimised for Vivo/Funtouch OS",
                driveFileId = DRIVE_FILE_ID_2,
                downloadUrl = "https://drive.google.com/uc?export=download&id=$DRIVE_FILE_ID_2",
                iosVersion = "17.x",
                emojiCount = 3600
            )
        )
    }
}

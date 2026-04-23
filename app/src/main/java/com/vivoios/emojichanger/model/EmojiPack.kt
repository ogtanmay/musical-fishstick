package com.vivoios.emojichanger.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an iOS emoji pack that can be downloaded and applied.
 */
@Entity(tableName = "emoji_packs")
data class EmojiPack(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val driveFileId: String,
    val downloadUrl: String,
    val localPath: String = "",
    val sizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val iosVersion: String = "",
    val emojiCount: Int = 0,
    val compatibilityScore: Int = 0,
    val renderingQuality: Int = 0,
    val appSupportLevel: Int = 0,
    val stabilityScore: Int = 0,
    val performanceScore: Int = 0,
    val overallScore: Int = 0,
    val isDownloaded: Boolean = false,
    val isApplied: Boolean = false,
    val downloadState: DownloadState = DownloadState.NOT_STARTED,
    val createdAt: Long = System.currentTimeMillis()
)

enum class DownloadState {
    NOT_STARTED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED,
    EXTRACTING,
    READY
}

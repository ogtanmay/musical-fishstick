package com.vivoios.emojichanger.db

import androidx.room.*
import com.vivoios.emojichanger.model.EmojiPack
import kotlinx.coroutines.flow.Flow

@Dao
interface EmojiPackDao {

    @Query("SELECT * FROM emoji_packs ORDER BY overallScore DESC")
    fun getAllPacks(): Flow<List<EmojiPack>>

    @Query("SELECT * FROM emoji_packs WHERE id = :id")
    suspend fun getPackById(id: String): EmojiPack?

    @Query("SELECT * FROM emoji_packs WHERE isApplied = 1 LIMIT 1")
    suspend fun getAppliedPack(): EmojiPack?

    @Query("SELECT * FROM emoji_packs WHERE isDownloaded = 1 ORDER BY overallScore DESC")
    fun getDownloadedPacks(): Flow<List<EmojiPack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPack(pack: EmojiPack)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacks(packs: List<EmojiPack>)

    @Update
    suspend fun updatePack(pack: EmojiPack)

    @Query("UPDATE emoji_packs SET isApplied = 0")
    suspend fun clearAppliedPacks()

    @Query("UPDATE emoji_packs SET isApplied = 1 WHERE id = :id")
    suspend fun setApplied(id: String)

    @Query("UPDATE emoji_packs SET downloadState = :state, downloadedBytes = :bytes WHERE id = :id")
    suspend fun updateDownloadProgress(id: String, state: String, bytes: Long)

    @Query("UPDATE emoji_packs SET isDownloaded = :downloaded, localPath = :path, downloadState = :state WHERE id = :id")
    suspend fun updateDownloadComplete(id: String, downloaded: Boolean, path: String, state: String)

    @Query("UPDATE emoji_packs SET overallScore = :score, compatibilityScore = :compat, renderingQuality = :quality WHERE id = :id")
    suspend fun updateScores(id: String, score: Int, compat: Int, quality: Int)

    @Query("DELETE FROM emoji_packs WHERE id = :id")
    suspend fun deletePack(id: String)
}

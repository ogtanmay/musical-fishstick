package com.vivoios.emojichanger.engine

import android.util.Log
import com.vivoios.emojichanger.db.EmojiPackDao
import com.vivoios.emojichanger.model.EmojiPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Compares two iOS emoji packs and selects the best one for the current device.
 *
 * Scoring criteria:
 * - System-wide compatibility
 * - Rendering quality (inferred from file count and types)
 * - Newest emoji support (version info from manifest)
 * - App support level
 * - Stability
 * - Performance (file size / emoji count ratio)
 */
class PackSelector(
    private val packDao: EmojiPackDao,
    private val deviceInfo: DeviceDetector.DeviceInfo
) {

    private val TAG = "PackSelector"

    data class SelectionResult(
        val winner: EmojiPack,
        val scores: Map<String, PackScore>
    )

    data class PackScore(
        val packId: String,
        val compatibilityScore: Int,
        val renderingQuality: Int,
        val emojiCoverage: Int,
        val appSupportLevel: Int,
        val stability: Int,
        val performance: Int,
        val totalScore: Int
    )

    suspend fun selectBestPack(packs: List<EmojiPack>): SelectionResult =
        withContext(Dispatchers.IO) {
            require(packs.isNotEmpty()) { "Pack list cannot be empty" }

            val scores = packs.associate { pack ->
                pack.id to scorePack(pack)
            }

            val winner = packs.maxByOrNull { scores[it.id]?.totalScore ?: 0 }!!
            Log.i(TAG, "Selected best pack: ${winner.name} (score: ${scores[winner.id]?.totalScore})")

            // Persist scores to database
            scores.forEach { (id, score) ->
                packDao.updateScores(id, score.totalScore, score.compatibilityScore, score.renderingQuality)
            }

            SelectionResult(winner, scores)
        }

    private fun scorePack(pack: EmojiPack): PackScore {
        val dir = File(pack.localPath)
        if (!dir.exists() || !pack.isDownloaded) {
            return PackScore(pack.id, 0, 0, 0, 0, 0, 0, 0)
        }

        val emojiFiles = dir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in listOf("png", "webp", "svg") }
            .toList()

        val totalFiles = emojiFiles.size
        val averageSizeKb = if (totalFiles > 0) {
            emojiFiles.sumOf { it.length() / 1024 }.toInt() / totalFiles
        } else 0

        // Compatibility: higher emoji count → wider Unicode coverage
        val compatibilityScore = when {
            totalFiles > 3500 -> 100
            totalFiles > 3000 -> 90
            totalFiles > 2500 -> 80
            totalFiles > 2000 -> 70
            totalFiles > 1000 -> 60
            else -> 40
        }

        // Rendering quality: smaller average size can mean WebP (better quality/size ratio)
        val hasWebP = emojiFiles.any { it.extension.lowercase() == "webp" }
        val renderingQuality = when {
            hasWebP && averageSizeKb in 5..30 -> 100 // WebP optimal range
            hasWebP -> 90
            averageSizeKb in 10..50 -> 85 // PNG optimal range
            averageSizeKb > 50 -> 70 // Large PNGs
            else -> 60
        }

        // Emoji coverage: files count vs expected total
        val emojiCoverage = minOf(100, (totalFiles * 100) / 3600)

        // App support based on device
        val appSupportLevel = when {
            deviceInfo.isVivoDevice && deviceInfo.isFuntouchOs14 -> 95
            deviceInfo.isVivoDevice -> 85
            deviceInfo.androidVersion >= 29 -> 80
            else -> 70
        }

        // Stability: check for manifest or metadata
        val hasManifest = File(dir, "metadata.json").exists() ||
                File(dir, "manifest.json").exists() ||
                File(dir, "emoji_metadata.json").exists()
        val stability = if (hasManifest) 100 else 85

        // Performance: prefer smaller total size
        val totalSizeMb = emojiFiles.sumOf { it.length() } / (1024 * 1024)
        val performance = when {
            totalSizeMb < 20 -> 100
            totalSizeMb < 50 -> 90
            totalSizeMb < 100 -> 80
            totalSizeMb < 200 -> 70
            else -> 60
        }

        // Weighted total
        val totalScore = (
                compatibilityScore * 0.25 +
                        renderingQuality * 0.20 +
                        emojiCoverage * 0.20 +
                        appSupportLevel * 0.15 +
                        stability * 0.10 +
                        performance * 0.10
                ).toInt()

        Log.d(TAG, "Pack ${pack.id}: compat=$compatibilityScore, render=$renderingQuality, " +
                "coverage=$emojiCoverage, app=$appSupportLevel, stability=$stability, " +
                "perf=$performance, total=$totalScore")

        return PackScore(
            packId = pack.id,
            compatibilityScore = compatibilityScore,
            renderingQuality = renderingQuality,
            emojiCoverage = emojiCoverage,
            appSupportLevel = appSupportLevel,
            stability = stability,
            performance = performance,
            totalScore = totalScore
        )
    }
}

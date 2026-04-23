package com.vivoios.emojichanger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import com.vivoios.emojichanger.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class EmojiChangerApp : Application(), Configuration.Provider {

    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val downloadChannel = NotificationChannel(
            CHANNEL_DOWNLOAD,
            "Emoji Pack Download",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows progress while downloading iOS emoji packs"
        }

        val statusChannel = NotificationChannel(
            CHANNEL_STATUS,
            "Emoji Status",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows emoji application status and updates"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(downloadChannel)
        notificationManager.createNotificationChannel(statusChannel)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    companion object {
        const val CHANNEL_DOWNLOAD = "channel_download"
        const val CHANNEL_STATUS = "channel_status"
    }
}

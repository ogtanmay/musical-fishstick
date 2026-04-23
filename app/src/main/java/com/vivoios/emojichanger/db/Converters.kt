package com.vivoios.emojichanger.db

import androidx.room.TypeConverter
import com.vivoios.emojichanger.model.DownloadState

class Converters {
    @TypeConverter
    fun fromDownloadState(state: DownloadState): String = state.name

    @TypeConverter
    fun toDownloadState(name: String): DownloadState =
        DownloadState.valueOf(name)
}

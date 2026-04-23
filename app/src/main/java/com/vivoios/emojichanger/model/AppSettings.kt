package com.vivoios.emojichanger.model

/**
 * Represents the overall app settings.
 */
data class AppSettings(
    val selectedPackId: String? = null,
    val autoReapplyOnBoot: Boolean = true,
    val darkModeEnabled: Boolean = true,
    val keyboardEnabled: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val showEmojiPreviewOnKeyboard: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val autoUpdatePacks: Boolean = false,
    val lastUpdateCheck: Long = 0L
)

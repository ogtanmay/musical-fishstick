package com.vivoios.emojichanger.model

/**
 * Represents the current status of emoji application.
 */
data class ApplyStatus(
    val mode: ApplyMode = ApplyMode.NONE,
    val isActive: Boolean = false,
    val appliedPackId: String? = null,
    val appliedPackName: String? = null,
    val message: String = "",
    val supportedMethods: List<ApplyMethod> = emptyList(),
    val activeMethod: ApplyMethod? = null,
    val coveragePercent: Int = 0,
    val lastApplied: Long = 0L
)

enum class ApplyMode {
    NONE,
    FULL_SYSTEM,
    HIGH_COMPAT,
    PARTIAL,
    KEYBOARD_BACKUP
}

enum class ApplyMethod {
    VIVO_THEME_ENGINE,
    TTF_FONT_OVERRIDE,
    OVERLAY_RENDERING,
    ACCESSIBILITY_SERVICE,
    IME_KEYBOARD,
    WEBVIEW_INJECTION
}

/**
 * Represents a single emoji entry with codepoint and image path.
 */
data class EmojiEntry(
    val codepoint: String,
    val name: String,
    val category: EmojiCategory,
    val keywords: List<String> = emptyList(),
    val imagePath: String = "",
    val unicode: String = ""
)

enum class EmojiCategory(val displayName: String, val icon: String) {
    SMILEYS_PEOPLE("Smileys & People", "😀"),
    ANIMALS_NATURE("Animals & Nature", "🐶"),
    FOOD_DRINK("Food & Drink", "🍎"),
    ACTIVITIES("Activities", "⚽"),
    TRAVEL_PLACES("Travel & Places", "✈️"),
    OBJECTS("Objects", "💡"),
    SYMBOLS("Symbols", "♥️"),
    FLAGS("Flags", "🏳️"),
    RECENT("Recent", "🕐")
}

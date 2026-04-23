package com.vivoios.emojichanger.engine

import android.os.Build
import android.util.Log

/**
 * Detects the current device information, including Vivo/Funtouch OS specifics.
 */
object DeviceDetector {

    private const val TAG = "DeviceDetector"

    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val brand: String,
        val androidVersion: Int,
        val androidVersionName: String,
        val funtouchOsVersion: String?,
        val isVivoDevice: Boolean,
        val isFuntouchOs14: Boolean,
        val supportedMethods: List<String>,
        val deviceFingerprint: String
    )

    fun detect(): DeviceInfo {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val brand = Build.BRAND
        val androidVersion = Build.VERSION.SDK_INT
        val androidVersionName = Build.VERSION.RELEASE
        val isVivo = isVivoDevice(manufacturer, brand)
        val funtouchVersion = getFuntouchOsVersion()
        val isFuntouchOs14 = checkFuntouchOs14(funtouchVersion)

        val supportedMethods = detectSupportedMethods(isVivo, isFuntouchOs14, androidVersion)

        Log.i(TAG, "Device: $manufacturer $model, Android $androidVersionName, Funtouch: $funtouchVersion")

        return DeviceInfo(
            manufacturer = manufacturer,
            model = model,
            brand = brand,
            androidVersion = androidVersion,
            androidVersionName = androidVersionName,
            funtouchOsVersion = funtouchVersion,
            isVivoDevice = isVivo,
            isFuntouchOs14 = isFuntouchOs14,
            supportedMethods = supportedMethods,
            deviceFingerprint = Build.FINGERPRINT
        )
    }

    private fun isVivoDevice(manufacturer: String, brand: String): Boolean {
        return manufacturer.lowercase().contains("vivo") ||
                brand.lowercase().contains("vivo")
    }

    private fun getFuntouchOsVersion(): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java, String::class.java)
            // Common Vivo system properties for Funtouch OS version
            val props = listOf(
                "ro.vivo.os.version",
                "ro.vivo.os.build.display.id",
                "ro.funtouch.os.version",
                "persist.vivo.os.version"
            )
            for (prop in props) {
                val value = method.invoke(null, prop, "") as String
                if (value.isNotEmpty()) {
                    Log.d(TAG, "Found Funtouch version via $prop: $value")
                    return value
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Could not read Funtouch OS version: ${e.message}")
            null
        }
    }

    private fun checkFuntouchOs14(version: String?): Boolean {
        if (version == null) return false
        return try {
            val major = version.split(".").firstOrNull()?.toIntOrNull() ?: 0
            major >= 14
        } catch (e: Exception) {
            version.contains("14") || version.contains("15")
        }
    }

    private fun detectSupportedMethods(
        isVivo: Boolean,
        isFuntouchOs14: Boolean,
        androidVersion: Int
    ): List<String> {
        val methods = mutableListOf<String>()

        // Custom IME keyboard - always supported
        methods.add("IME_KEYBOARD")

        // Accessibility service - always supported on Android 5+
        if (androidVersion >= Build.VERSION_CODES.LOLLIPOP) {
            methods.add("ACCESSIBILITY_SERVICE")
        }

        // Vivo theme engine - only on actual Vivo devices
        if (isVivo && isFuntouchOs14) {
            methods.add("VIVO_THEME_ENGINE")
        }

        // TTF font override - Android 10+ supports font override API
        if (androidVersion >= Build.VERSION_CODES.Q) {
            methods.add("TTF_FONT_OVERRIDE")
        }

        // Overlay rendering via accessibility
        if (androidVersion >= Build.VERSION_CODES.O) {
            methods.add("OVERLAY_RENDERING")
        }

        // WebView injection - Android 7+
        if (androidVersion >= Build.VERSION_CODES.N) {
            methods.add("WEBVIEW_INJECTION")
        }

        return methods
    }

    fun getBestMethodDescription(methods: List<String>): String {
        return when {
            methods.contains("VIVO_THEME_ENGINE") -> "Vivo Theme Engine (Best coverage)"
            methods.contains("TTF_FONT_OVERRIDE") -> "System Font Override (High coverage)"
            methods.contains("ACCESSIBILITY_SERVICE") -> "Accessibility Service (Good coverage)"
            methods.contains("IME_KEYBOARD") -> "iOS Emoji Keyboard (Keyboard coverage)"
            else -> "Basic Support"
        }
    }

    fun getCoveragePercent(methods: List<String>): Int {
        return when {
            methods.contains("VIVO_THEME_ENGINE") -> 95
            methods.contains("TTF_FONT_OVERRIDE") -> 85
            methods.contains("ACCESSIBILITY_SERVICE") -> 70
            methods.contains("IME_KEYBOARD") -> 60
            else -> 30
        }
    }
}

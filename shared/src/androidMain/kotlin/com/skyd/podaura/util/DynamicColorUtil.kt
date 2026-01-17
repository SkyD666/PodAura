package com.skyd.podaura.util

import android.annotation.SuppressLint
import android.os.Build
import java.util.Locale

/**
 * Copied from https://github.com/material-components/material-components-android/blob/master/lib/java/com/google/android/material/color/DynamicColors.java
 */
object DynamicColorUtil {

    private val DEFAULT_DEVICE_SUPPORT_CONDITION = object : DeviceSupportCondition {
        override val isSupported: Boolean = true
    }

    private val SAMSUNG_DEVICE_SUPPORT_CONDITION = object : DeviceSupportCondition {
        private var version: Long? = null

        override val isSupported: Boolean
            @Suppress("SpellCheckingInspection")
            @SuppressLint("PrivateApi")
            get() {
                if (version == null) {
                    version = try {
                        Build::class.java.getDeclaredMethod("getLong", String::class.java)
                            .apply { isAccessible = true }
                            .invoke(null, "ro.build.version.oneui") as Long
                    } catch (_: Exception) {
                        -1L
                    }
                }
                return version!! >= 40100L
            }
    }

    @Suppress("SpellCheckingInspection")
    private val DYNAMIC_COLOR_SUPPORTED_MANUFACTURERS = mapOf(
        "fcnt" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "google" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "hmd global" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "infinix" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "infinix mobility limited" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "itel" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "kyocera" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "lenovo" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "lge" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "meizu" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "motorola" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "nothing" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "oneplus" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "oppo" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "realme" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "robolectric" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "samsung" to SAMSUNG_DEVICE_SUPPORT_CONDITION,
        "sharp" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "shift" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "sony" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "tcl" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "tecno" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "tecno mobile limited" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "vivo" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "wingtech" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "xiaomi" to DEFAULT_DEVICE_SUPPORT_CONDITION
    )

    private val DYNAMIC_COLOR_SUPPORTED_BRANDS = mapOf(
        "asus" to DEFAULT_DEVICE_SUPPORT_CONDITION,
        "jio" to DEFAULT_DEVICE_SUPPORT_CONDITION
    )

    fun isDynamicColorAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        var deviceSupportCondition =
            DYNAMIC_COLOR_SUPPORTED_MANUFACTURERS[Build.MANUFACTURER.lowercase(Locale.ROOT)]
        if (deviceSupportCondition == null) {
            deviceSupportCondition =
                DYNAMIC_COLOR_SUPPORTED_BRANDS[Build.BRAND.lowercase(Locale.ROOT)]
        }
        return deviceSupportCondition?.isSupported == true
    }

    private interface DeviceSupportCondition {
        val isSupported: Boolean
    }
}

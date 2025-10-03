package com.skyd.podaura.ext

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Window
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource

val Context.activity: Activity
    get() {
        return tryActivity ?: error("Can't find activity: $this")
    }

@get:JvmName("tryActivity")
val Context.tryActivity: Activity?
    get() {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return null
    }

val Context.tryWindow: Window?
    get() {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) {
                return ctx.window
            }
            ctx = ctx.baseContext
        }
        return null
    }

fun Context.getAppVersionName(): String {
    var appVersionName = ""
    try {
        val packageInfo = if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {
            packageManager.getPackageInfo(packageName, 0)
        }
        appVersionName = packageInfo.versionName.orEmpty()
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return appVersionName
}

fun Context.getAppVersionCode(): Long {
    var appVersionCode: Long = 0
    try {
        val packageInfo = applicationContext
            .packageManager
            .getPackageInfo(packageName, 0)
        appVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return appVersionCode
}

fun Context.getAppName(): String? {
    return try {
        val packageInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
        val labelRes: Int = packageInfo.applicationInfo?.labelRes ?: return null
        getString(labelRes)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun Context.inDarkMode(): Boolean {
    return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
}

fun Context.getString(resource: StringResource): String = runBlocking {
    org.jetbrains.compose.resources.getString(resource)
}

fun Context.getString(resource: StringResource, vararg formatArgs: Any): String = runBlocking {
    org.jetbrains.compose.resources.getString(resource, formatArgs)
}

fun Context.vibrator(): Vibrator {
    return if (SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
}
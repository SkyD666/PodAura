package com.skyd.anivu.ext

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.view.Window
import androidx.core.content.pm.PackageInfoCompat
import coil3.ImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import coil3.video.VideoFrameDecoder
import okhttp3.Interceptor
import okhttp3.OkHttpClient

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

fun Context.isWifi(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities =
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
}

fun Context.imageLoaderBuilder(): ImageLoader.Builder {
    return ImageLoader.Builder(this)
        .components {
            if (SDK_INT >= 28) {
                add(AnimatedImageDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
            add(SvgDecoder.Factory())
            add(VideoFrameDecoder.Factory())
        }
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = {
                OkHttpClient.Builder().addNetworkInterceptor(Interceptor { chain ->
                    chain.proceed(chain.request()).newBuilder()
                        .header("Cache-Control", "max-age=31536000,public")
                        .build()
                }).build()
            }))
        }
}
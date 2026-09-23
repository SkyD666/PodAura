package com.skyd.fundation.jna

import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native

/** Process-lifetime native notification service. The caller buffers activation until UI startup. */
object NativeArticleNotifications {
    private val macLibrary: MacNotificationLibrary by lazy {
        load("podaura_media_player", "libpodaura_media_player.dylib", MacNotificationLibrary::class.java)
    }
    private val windowsLibrary: WindowsNotificationLibrary by lazy {
        load("podaura_windows_media_player", "podaura_windows_media_player.dll", WindowsNotificationLibrary::class.java)
    }

    // A native delegate retains this function pointer for the lifetime of the process.
    private var activationCallback: NotificationActivationCallback? = null

    fun initialize(onActivation: (String) -> Unit): Boolean = when (platform) {
        Platform.macOS_Jvm -> {
            val callback = NotificationActivationCallback(onActivation)
            activationCallback = callback
            macLibrary.podaura_notifications_init(callback) != 0
        }
        Platform.Windows -> windowsResult(windowsLibrary.podaura_notifications_init(
            System.getProperty("jpackage.app-path"),
        ))
        else -> false
    }

    fun requestPermission() {
        if (platform == Platform.macOS_Jvm) {
            macLibrary.podaura_notifications_request_permission()
        }
    }

    fun send(id: String, title: String, body: String, activationUri: String): Boolean = when (platform) {
        Platform.macOS_Jvm -> macLibrary.podaura_notifications_send(id, title, body, activationUri) != 0
        Platform.Windows -> windowsResult(windowsLibrary.podaura_notifications_send(id, title, body, activationUri))
        else -> false
    }

    private fun windowsResult(result: Int): Boolean {
        check(result != 0) {
            windowsLibrary.podaura_windows_media_player_last_error() ?: "Windows notification operation failed"
        }
        return true
    }

    private fun <T : Library> load(baseName: String, fileName: String, type: Class<T>): T = Native.load(
        DesktopNativeLibrary.file(baseName, fileName).absolutePath,
        type,
        mapOf(Library.OPTION_STRING_ENCODING to Charsets.UTF_8.name()),
    )
}

internal fun interface NotificationActivationCallback : Callback {
    fun invoke(activationUri: String)
}

internal interface MacNotificationLibrary : Library {
    fun podaura_notifications_init(callback: NotificationActivationCallback): Int
    fun podaura_notifications_request_permission()
    fun podaura_notifications_send(id: String, title: String, body: String, activationUri: String): Int
}

internal interface WindowsNotificationLibrary : Library {
    fun podaura_notifications_init(applicationExecutable: String?): Int
    fun podaura_notifications_send(id: String, title: String, body: String, activationUri: String): Int
    fun podaura_windows_media_player_last_error(): String?
}

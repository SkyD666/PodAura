package com.skyd.podaura.ui.notification

import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.ui.window.WindowsPackageIdentity
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinError
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.ptr.IntByReference
import java.io.File

private const val APPMODEL_ERROR_NO_PACKAGE = 15700
private const val NOTIFICATION_PROTOCOL_KEY = "Software\\Classes\\podaura"

/**
 * Registers the installed jpackage launcher for podaura://notification/<uuid>.
 * Returns true for MSIX (whose manifest supplies registration) or a successful HKCU write.
 * Gradle/IDE launches without jpackage.app-path leave the installed handler untouched.
 */
internal fun registerWindowsNotificationProtocol(): Boolean {
    if (platform != Platform.Windows) return false
    return runCatching {
        val packageStatus = WindowsPackageIdentity.KernelAppModelApi.instance
            .GetCurrentPackageFullName(IntByReference(), null)
        when (packageStatus) {
            WinError.ERROR_INSUFFICIENT_BUFFER, WinError.ERROR_SUCCESS -> return@runCatching true
            APPMODEL_ERROR_NO_PACKAGE -> Unit
            // An unknown identity must not cause a packaged app to shadow its MSIX registration.
            else -> return@runCatching false
        }

        val applicationPath = System.getProperty("jpackage.app-path") ?: return@runCatching false
        if (applicationPath.isBlank() || applicationPath.any { it == '"' || it.code < 32 }) {
            return@runCatching false
        }
        val executable = File(applicationPath)
        if (!executable.isAbsolute || !executable.isFile ||
            !executable.extension.equals("exe", ignoreCase = true)
        ) {
            return@runCatching false
        }
        // No shell, java.exe fallback or environment expansion. Quote both the executable and URI.
        val command = "\"${executable.normalize().path}\" \"%1\""
        val root = WinReg.HKEY_CURRENT_USER
        val commandKey = "$NOTIFICATION_PROTOCOL_KEY\\shell\\open\\command"
        Advapi32Util.registryCreateKey(root, commandKey)
        Advapi32Util.registrySetStringValue(root, commandKey, "", command)
        Advapi32Util.registrySetStringValue(
            root,
            NOTIFICATION_PROTOCOL_KEY,
            "",
            "URL:PodAura Protocol"
        )
        Advapi32Util.registrySetStringValue(root, NOTIFICATION_PROTOCOL_KEY, "URL Protocol", "")
        true
    }.getOrDefault(false)
}

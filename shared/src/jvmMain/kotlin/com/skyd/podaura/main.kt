package com.skyd.podaura

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.skyd.compone.local.WindowController
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.ui.notification.DesktopArticleNotifications
import com.skyd.podaura.ui.window.CrashWindow
import com.skyd.podaura.ui.window.DesktopOpenFiles
import com.skyd.podaura.ui.window.DesktopWindowHost
import com.skyd.podaura.ui.window.DesktopWindowId
import com.skyd.podaura.ui.window.initWindowsAppIdentity
import com.skyd.podaura.ui.window.rememberDesktopAppState
import com.skyd.podaura.util.CrashHandler

fun main(args: Array<String>) {
    if (platform == Platform.macOS_Jvm) DesktopArticleNotifications.initialize()
    val openFiles = DesktopOpenFiles()
    val shutdownHook = Thread({ openFiles.close() }, "PodAura activation cleanup")
    Runtime.getRuntime().addShutdownHook(shutdownHook)
    try {
        if (openFiles.start(args)) runDesktopApplication(openFiles)
    } finally {
        openFiles.close()
        Runtime.getRuntime().removeShutdownHook(shutdownHook)
    }
}

private fun runDesktopApplication(openFiles: DesktopOpenFiles) {
    initWindowsAppIdentity()
    DesktopArticleNotifications.initialize()

    var crashMessage by mutableStateOf("")
    CrashHandler.init(onCrash = { crashMessage = it })

    // https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-desktop-swing-interoperability.html#experimental-interop-blending
    System.setProperty("compose.interop.blending", "true")

    initKoin()
    onAppStart()

    application {
        if (crashMessage.isBlank()) {
            val appState = rememberDesktopAppState()
            LaunchedEffect(appState) {
                DesktopArticleNotifications.activations.collect { uri ->
                    val deeplink = DesktopArticleNotifications.articleDeeplink(uri)
                    if (deeplink != null) appState.openMainPage(deeplink)
                    else appState.windowManager.activate(DesktopWindowId.Main)
                }
            }
            LaunchedEffect(appState) {
                openFiles.requests.collect { files ->
                    if (files.isEmpty()) {
                        appState.windowManager.activate(DesktopWindowId.Main)
                    } else {
                        appState.openFiles(files)
                    }
                }
            }
            val exitPodAura = {
                appState.destroySession()
                exitApplication()
            }
            val windowController = remember(appState) {
                WindowController(onClose = exitPodAura)
            }
            val mainWindowState = rememberWindowState(
                position = WindowPosition.Aligned(alignment = Alignment.Center),
                size = DpSize(1200.dp, 800.dp),
            )

            // The collector is tied to the normal application lifetime, not to whether the native
            // player window currently exists. This avoids consuming mediaInfos replay on reopen.
            LaunchedEffect(appState.playerWindowController) {
                appState.playerWindowController.collectMediaInfos()
            }

            appState.windowManager.windows.forEach { entry ->
                key(entry.id) {
                    DesktopWindowHost(
                        entry = entry,
                        appState = appState,
                        mainWindowState = mainWindowState,
                        mainWindowController = windowController,
                    )
                }
            }
        } else {
            CrashWindow(
                onCloseRequest = ::exitApplication,
                crashInfo = crashMessage,
            )
        }
    }
}

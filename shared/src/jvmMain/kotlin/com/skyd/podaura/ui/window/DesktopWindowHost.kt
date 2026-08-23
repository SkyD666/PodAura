package com.skyd.podaura.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.WindowState
import com.skyd.compone.local.LocalWindowController
import com.skyd.compone.local.WindowController
import com.skyd.podaura.ui.component.frame.WindowFrame
import com.skyd.podaura.ui.player.LocalPlayerSession
import com.skyd.podaura.ui.screen.AppEntrance
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.app_name

/**
 * The single rendering dispatch point for every normal JVM desktop window.
 */
@Composable
internal fun DesktopWindowHost(
    entry: DesktopWindowEntry,
    appState: DesktopAppState,
    mainWindowState: WindowState,
    mainWindowController: WindowController,
) {
    when (val spec = entry.spec) {
        DesktopWindowSpec.Main -> MainWindow(
            entry = entry,
            appState = appState,
            windowState = mainWindowState,
            windowController = mainWindowController,
        )

        DesktopWindowSpec.Player -> PlayerWindow(
            entry = entry,
            mainWindowState = mainWindowState,
            appState = appState,
        )

        is DesktopWindowSpec.ImagePreview -> ImagePreviewWindow(
            entry = entry,
            spec = spec,
            mainWindowState = mainWindowState,
            appState = appState,
        )
    }
}

@Composable
private fun MainWindow(
    entry: DesktopWindowEntry,
    appState: DesktopAppState,
    windowState: WindowState,
    windowController: WindowController,
) {
    BaseWindow(
        onCloseRequest = windowController.onClose,
        state = windowState,
        title = stringResource(Res.string.app_name),
    ) {
        LaunchedEffect(entry.activationToken) {
            windowState.isMinimized = false
            window.toFront()
            window.requestFocus()
        }
        CompositionLocalProvider(
            LocalDesktopAppState provides appState,
            LocalPlayerSession provides appState,
            LocalWindowController provides windowController,
        ) {
            WindowFrame(
                onCloseRequest = windowController.onClose,
                state = windowState,
                content = ::AppEntrance
            )
        }
    }
}

package com.skyd.podaura.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.FrameWindowScope
import co.touchlab.kermit.Logger
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.ui.player.media.DesktopMediaWindowRegistration
import com.skyd.podaura.ui.player.media.DesktopMediaWindowTooltips
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.pause
import podaura.shared.generated.resources.play
import podaura.shared.generated.resources.skip_next
import podaura.shared.generated.resources.skip_previous

@Composable
internal fun FrameWindowScope.WindowsMediaControlsEffect(
    controller: PlayerWindowController,
    isMainWindow: Boolean,
) {
    if (platform != Platform.Windows) return

    val coordinator = controller.coordinator ?: return
    val windowHandle = window.windowHandle
    val tooltips = DesktopMediaWindowTooltips(
        previous = stringResource(Res.string.skip_previous),
        play = stringResource(Res.string.play),
        pause = stringResource(Res.string.pause),
        next = stringResource(Res.string.skip_next),
    )
    var registration by remember(
        coordinator,
        windowHandle,
        isMainWindow,
    ) { mutableStateOf<DesktopMediaWindowRegistration?>(null) }
    val logger = remember { Logger.withTag("WindowsMediaControls") }

    DisposableEffect(coordinator, windowHandle, isMainWindow) {
        registration = runCatching {
            controller.attachDesktopMediaWindow(
                windowHandle = windowHandle,
                isMainWindow = isMainWindow,
                tooltips = tooltips,
            )
        }.onFailure { throwable ->
            logger.e(throwable = throwable) {
                "Could not attach Windows media controls to native window"
            }
        }.getOrNull()
        onDispose {
            registration?.let { attached ->
                runCatching { attached.close() }.onFailure { throwable ->
                    logger.w(throwable = throwable) {
                        "Could not detach Windows media controls from native window"
                    }
                }
            }
            registration = null
        }
    }

    LaunchedEffect(registration, tooltips) {
        registration?.let { attached ->
            runCatching { attached.updateTooltips(tooltips) }.onFailure { throwable ->
                logger.w(throwable = throwable) {
                    "Could not update Windows taskbar tooltips"
                }
            }
        }
    }
}

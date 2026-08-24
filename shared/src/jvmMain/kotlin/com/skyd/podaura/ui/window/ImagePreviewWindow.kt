package com.skyd.podaura.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import com.skyd.podaura.ui.screen.SettingsProvider
import com.skyd.podaura.ui.screen.image.ImagePreviewScreen
import com.skyd.podaura.ui.screen.image.LocalDesktopMagnificationFactors
import com.skyd.podaura.ui.screen.image.installJvmMagnificationListener
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.image_preview_description

private val imagePreviewWindowSize = DpSize(1024.dp, 768.dp)

@Composable
internal fun ImagePreviewWindow(
    entry: DesktopWindowEntry,
    spec: DesktopWindowSpec.ImagePreview,
    mainWindowState: WindowState,
    appState: DesktopAppState,
) {
    val closeImagePreview = { appState.closeImagePreview(spec.image) }
    val imagePreviewWindowState = rememberWindowState(
        size = imagePreviewWindowSize,
        position = (mainWindowState.position as? WindowPosition.Absolute)?.let { parent ->
            WindowPosition.Absolute(
                x = parent.x + (mainWindowState.size.width - imagePreviewWindowSize.width) / 2,
                y = parent.y + (mainWindowState.size.height - imagePreviewWindowSize.height) / 2,
            )
        } ?: WindowPosition.Aligned(alignment = Alignment.Center),
    )

    BaseWindow(
        onCloseRequest = closeImagePreview,
        state = imagePreviewWindowState,
        title = stringResource(Res.string.image_preview_description),
        resizable = true,
    ) {
        val magnificationFactors = remember {
            MutableSharedFlow<Float>(
                extraBufferCapacity = 64,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        }
        DisposableEffect(window, magnificationFactors) {
            val registration = installJvmMagnificationListener(window.rootPane) {
                magnificationFactors.tryEmit(it)
            }
            onDispose { registration.close() }
        }

        LaunchedEffect(entry.activationToken) {
            imagePreviewWindowState.isMinimized = false
            window.toFront()
            window.requestFocus()
        }
        CompositionLocalProvider(
            LocalDesktopAppState provides appState,
            LocalDesktopMagnificationFactors provides magnificationFactors,
        ) {
            SettingsProvider {
                ImagePreviewScreen(
                    image = spec.image,
                    title = spec.title,
                    onBack = closeImagePreview,
                )
            }
        }
    }
}

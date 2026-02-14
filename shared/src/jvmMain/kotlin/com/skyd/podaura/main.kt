package com.skyd.podaura

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.skyd.compone.local.LocalWindowController
import com.skyd.compone.local.WindowController
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.ui.screen.AppEntrance
import com.skyd.podaura.ui.window.CrashWindow
import com.skyd.podaura.util.CrashHandler
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.app_name

fun main() {
    var crashMessage by mutableStateOf("")
    CrashHandler.init(onCrash = { crashMessage = it })
    application {
        // https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-desktop-swing-interoperability.html#experimental-interop-blending
        System.setProperty("compose.interop.blending", "true")

        if (crashMessage.isBlank()) {
            val windowController = WindowController(onClose = ::exitApplication)
            initKoin()
            onAppStart()

            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(
                    position = WindowPosition.Aligned(alignment = Alignment.Center),
                    size = DpSize(1200.dp, 800.dp),
                ),
                title = stringResource(Res.string.app_name),
            ) {
                CompositionLocalProvider(
                    LocalWindowController provides windowController,
                ) {
                    AppEntrance()
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
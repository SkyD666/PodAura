package com.skyd.podaura

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.skyd.compone.local.LocalWindowController
import com.skyd.compone.local.WindowController
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.ui.screen.AppEntrance
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.app_name

fun main() = application {
    // https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-desktop-swing-interoperability.html#experimental-interop-blending
    System.setProperty("compose.interop.blending", "true")

    val windowController = WindowController(onClose = ::exitApplication)
    initKoin {}
    onAppStart()

    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.app_name),
    ) {
        CompositionLocalProvider(
            LocalWindowController provides windowController,
        ) {
            AppEntrance()
        }
    }
}
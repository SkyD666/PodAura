package com.skyd.podaura

import androidx.compose.ui.window.ComposeUIViewController
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.ui.screen.AppEntrance
import platform.UIKit.UIViewController

@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    initKoin()
    onAppStart()
    return ComposeUIViewController(
        configure = {
            parallelRendering = true
        },
        content = {
            AppEntrance()
        }
    )
}

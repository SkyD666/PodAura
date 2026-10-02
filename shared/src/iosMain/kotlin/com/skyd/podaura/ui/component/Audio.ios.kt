package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.outputVolume
import platform.CoreGraphics.CGRectMake
import platform.MediaPlayer.MPVolumeView
import platform.UIKit.UIApplication
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UISlider
import platform.UIKit.UIWindow

@Composable
actual fun rememberAudioController(): AudioController {
    val volumeView = remember { MPVolumeView(CGRectMake(-200.0, -200.0, 100.0, 40.0)) }
    DisposableEffect(volumeView) {
        (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)?.addSubview(volumeView)
        onDispose { volumeView.removeFromSuperview() }
    }
    return remember(volumeView) {
        object : AudioController {
            override val range = 0f..1f
            override var value: Float
                get() = AVAudioSession.sharedInstance().outputVolume
                set(value) {
                    volumeView.subviews.filterIsInstance<UISlider>().firstOrNull()?.let {
                        it.setValue(value.coerceIn(range), animated = false)
                        it.sendActionsForControlEvents(UIControlEventTouchUpInside)
                    }
                }
        }
    }
}

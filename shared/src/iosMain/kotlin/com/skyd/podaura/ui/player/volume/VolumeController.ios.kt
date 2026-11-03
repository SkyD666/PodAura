package com.skyd.podaura.ui.player.volume

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import com.skyd.podaura.ui.player.mpv.MPV
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.outputVolume
import platform.CoreGraphics.CGRectMake
import platform.MediaPlayer.MPVolumeView
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UISlider
import platform.UIKit.accessibilityElementsHidden

@Composable
actual fun rememberVolumeController(): VolumeController {
    val viewController = LocalUIViewController.current
    val volumeView = remember {
        MPVolumeView(CGRectMake(-200.0, -200.0, 100.0, 40.0)).apply {
            userInteractionEnabled = false
            accessibilityElementsHidden = true
        }
    }
    DisposableEffect(viewController, volumeView) {
        // Attach to this player's host, including presented players and multiple scenes.
        viewController.view.addSubview(volumeView)
        onDispose { volumeView.removeFromSuperview() }
    }
    return remember(volumeView) {
        object : VolumeController {
            override val range = 0f..1f
            override var value: Float
                get() = AVAudioSession.sharedInstance().outputVolume
                set(value) {
                    if (!value.isFinite()) return
                    // ponytail: the swipe UX depends on MPVolumeView's internal UISlider.
                    // If iOS changes it, replace this bridge with a visible MPVolumeView.
                    volumeView.subviews.filterIsInstance<UISlider>().firstOrNull()?.let { slider ->
                        slider.setValue(value.coerceIn(range), animated = false)
                        slider.sendActionsForControlEvents(
                            UIControlEventValueChanged or UIControlEventTouchUpInside
                        )
                    }
                }
        }
    }
}

internal actual fun createPlayerVolumeBackend(mpv: MPV): PlayerVolumeBackend = MpvVolumeBackend(mpv)

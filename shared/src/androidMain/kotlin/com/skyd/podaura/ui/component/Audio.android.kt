package com.skyd.podaura.ui.component

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberAudioController(): AudioController {
    val context = LocalContext.current
    return remember(context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val minVolume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        } else 0
        object : AudioController {
            override val range: ClosedFloatingPointRange<Float>
                get() = minVolume.toFloat()..maxVolume.toFloat()
            override var value: Float
                get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                set(value) {
                    val desiredVolume = value.toInt().coerceIn(minVolume..maxVolume)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, desiredVolume, 0)
                }
        }
    }
}
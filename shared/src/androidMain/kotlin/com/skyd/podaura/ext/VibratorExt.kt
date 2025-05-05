package com.skyd.podaura.ext

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

fun Vibrator.tickVibrate(duration: Long = 35) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.EFFECT_TICK))
    } else {
        @Suppress("DEPRECATION")
        vibrate(duration)
    }
}
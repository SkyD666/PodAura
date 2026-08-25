package com.skyd.podaura.ui.player.mpv

import android.view.KeyCharacterMap
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import com.skyd.fundation.di.get
import `is`.xyz.mpv.KeyMapping

actual fun MPV.initOptionsPlatform(logger: Logger) {
    // vo: set display fps as reported by android
    val disp = ContextCompat.getDisplayOrDefault(get())
    val refreshRate = disp.mode.refreshRate

    logger.v("Display ${disp.displayId} reports FPS of $refreshRate")
    option("display-fps-override", refreshRate.toString())
    option("gpu-context", "android")
    option("opengl-es", "yes")
}

actual fun copyAssetsForMpv(configDir: String) {
    com.skyd.podaura.ui.player.copyAssetsForMpv(get(), configDir)
}

internal actual fun mapPlayerKeyEvent(
    event: androidx.compose.ui.input.key.KeyEvent,
    logger: Logger,
): PlayerKeyInput? {
    val nativeEvent = event.nativeKeyEvent
    if (KeyEvent.isModifierKey(nativeEvent.keyCode)) {
        return null
    }

    var mapped = KeyMapping.map.get(nativeEvent.keyCode)
    if (mapped == null) {
        // Fallback to produced glyph
        if (!nativeEvent.isPrintingKey) {
            if (nativeEvent.repeatCount == 0) {
                logger.d("Unmapped non-printable key ${nativeEvent.keyCode}")
            }
            return null
        }

        val ch = nativeEvent.unicodeChar
        if (ch.and(KeyCharacterMap.COMBINING_ACCENT) != 0) {
            return null // dead key
        }
        mapped = ch.toChar().toString()
    }

    if (nativeEvent.repeatCount > 0) {
        return PlayerKeyInput(action = null, key = null)
    }

    val mod = mutableListOf<String>().apply {
        nativeEvent.isShiftPressed && add("shift")
        nativeEvent.isCtrlPressed && add("ctrl")
        nativeEvent.isAltPressed && add("alt")
        nativeEvent.isMetaPressed && add("meta")
        add(mapped)
    }

    val action = if (nativeEvent.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
    return PlayerKeyInput(action = action, key = mod.joinToString("+"))
}

package com.skyd.podaura.ui.player.mpv

import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import com.skyd.fundation.di.get
import `is`.xyz.mpv.KeyMapping

fun MPVPlayer.surfaceCallback(): SurfaceHolder.Callback {
    return object : SurfaceHolder.Callback {
        private val logger = Logger.withTag("surfaceCallback")
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            mpv.setPropertyString("android-surface-size", "${width}x$height")
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            logger.w("attaching surface")
            mpv.attachSurface(holder)
            // This forces mpv to render subs/osd/whatever into our surface even if it would ordinarily not
            mpv.option("force-window", "yes")
            // We disable video output when the context disappears, enable it back
            mpv.setPropertyString("vo", voInUse)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            if (this@surfaceCallback.initialized) {
                logger.w("detaching surface")
                mpv.setPropertyString("vo", "null")
                mpv.option("force-window", "no")
                mpv.detachSurface()
            }
        }
    }
}

actual fun MPV.initOptionsPlatform(logger: Logger) {
    // vo: set display fps as reported by android
    val disp = ContextCompat.getDisplayOrDefault(get())
    val refreshRate = disp.mode.refreshRate

    logger.v("Display ${disp.displayId} reports FPS of $refreshRate")
    option("display-fps-override", refreshRate.toString())
    option("gpu-context", "android")
}

actual fun copyAssetsForMpv(configDir: String) {
    com.skyd.podaura.ui.player.copyAssetsForMpv(get(), configDir)
}

actual fun MPV.onKey(event: androidx.compose.ui.input.key.KeyEvent, logger: Logger): Boolean {
    val nativeEvent = event.nativeKeyEvent
    if (KeyEvent.isModifierKey(nativeEvent.keyCode)) {
        return false
    }

    var mapped = KeyMapping.map.get(nativeEvent.keyCode)
    if (mapped == null) {
        // Fallback to produced glyph
        if (!nativeEvent.isPrintingKey) {
            if (nativeEvent.repeatCount == 0) {
                logger.d("Unmapped non-printable key ${nativeEvent.keyCode}")
            }
            return false
        }

        val ch = nativeEvent.unicodeChar
        if (ch.and(KeyCharacterMap.COMBINING_ACCENT) != 0) {
            return false // dead key
        }
        mapped = ch.toChar().toString()
    }

    if (nativeEvent.repeatCount > 0) {
        return true // consume event but ignore it, mpv has its own key repeat
    }

    val mod = mutableListOf<String>().apply {
        nativeEvent.isShiftPressed && add("shift")
        nativeEvent.isCtrlPressed && add("ctrl")
        nativeEvent.isAltPressed && add("alt")
        nativeEvent.isMetaPressed && add("meta")
        add(mapped)
    }

    val action = if (nativeEvent.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
    command(action, mod.joinToString("+"))

    return true
}
package com.skyd.podaura.ui.player.mpv

import coil3.Bitmap
import com.skyd.podaura.ui.PlatformSurfaceHolder

actual class MPV {
    actual fun initialize() {
    }

    actual fun addEventListener(listener: EventListener) {
    }

    actual fun removeEventListener(listener: EventListener) {
    }

    actual fun command(vararg command: String) {
    }

    actual fun option(key: String, value: String) {
    }

    actual fun getPropertyInt(name: String): Int {
        TODO("Not yet implemented")
    }

    actual fun getPropertyBoolean(name: String): Boolean {
        TODO("Not yet implemented")
    }

    actual fun getPropertyDouble(name: String): Double {
        TODO("Not yet implemented")
    }

    actual fun getPropertyString(name: String): String? {
        TODO("Not yet implemented")
    }

    actual fun setPropertyInt(name: String, value: Int) {
    }

    actual fun setPropertyBoolean(name: String, value: Boolean) {
    }

    actual fun setPropertyDouble(name: String, value: Double) {
    }

    actual fun setPropertyString(name: String, value: String) {
    }

    actual fun observeProperty(
        name: String,
        format: MPVFormat
    ) {
    }

    actual fun destroy() {
    }

    actual fun attachSurface(surfaceHolder: PlatformSurfaceHolder) {
    }

    actual fun detachSurface() {
    }

    actual fun grabThumbnail(dimension: Int): Bitmap? {
        TODO("Not yet implemented")
    }
}

actual fun platformMPV(): MPV {
    TODO("Not yet implemented")
}
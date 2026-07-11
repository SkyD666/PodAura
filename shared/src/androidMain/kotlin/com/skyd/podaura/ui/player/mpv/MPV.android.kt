package com.skyd.podaura.ui.player.mpv

import coil3.Bitmap
import com.skyd.fundation.di.get
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.MpvConfigDirPreference
import com.skyd.podaura.ui.PlatformSurfaceHolder
import `is`.xyz.mpv.MPVLib

actual class MPV {
    private val eventListeners = mutableMapOf<EventListener, MPVLib.EventObserver>()

    actual fun initialize() {
        MPVLib.create(get(), dataStore.getOrDefault(MpvConfigDirPreference))
        MPVLib.init()
    }

    actual fun destroy() {
        MPVLib.destroy()
    }

    actual fun attachSurface(surfaceHolder: PlatformSurfaceHolder) {
        MPVLib.attachSurface(surfaceHolder.surface)
    }

    actual fun detachSurface() {
        MPVLib.detachSurface()
    }

    actual fun addEventListener(listener: EventListener) {
        val eventObserver = object : MPVLib.EventObserver {
            override fun eventProperty(p0: String) {
                listener.onPropertyChange(p0)
            }

            override fun eventProperty(p0: String, p1: Long) {
                listener.onPropertyChange(p0, p1)
            }

            override fun eventProperty(p0: String, p1: Boolean) {
                listener.onPropertyChange(p0, p1)
            }

            override fun eventProperty(p0: String, p1: String) {
                listener.onPropertyChange(p0, p1)
            }

            override fun eventProperty(p0: String, p1: Double) {
                listener.onPropertyChange(p0, p1)
            }

            override fun event(p0: Int) {
                listener.onEvent(p0)
            }

            override fun efEvent(p0: String) {
            }

        }
        eventListeners += listener to eventObserver
        MPVLib.addObserver(eventObserver)
    }

    actual fun removeEventListener(listener: EventListener) {
        val eventObserver = eventListeners.remove(listener)
        if (eventObserver != null) {
            MPVLib.removeObserver(eventObserver)
        }
    }

    actual fun command(vararg command: String) {
        MPVLib.command(command)
    }

    actual fun option(key: String, value: String) {
        MPVLib.setOptionString(key, value)
    }

    actual fun getPropertyInt(name: String): Int = MPVLib.getPropertyInt(name)
    actual fun getPropertyBoolean(name: String): Boolean = MPVLib.getPropertyBoolean(name)
    actual fun getPropertyDouble(name: String): Double = MPVLib.getPropertyDouble(name)
    actual fun getPropertyString(name: String): String? = MPVLib.getPropertyString(name)

    actual fun setPropertyInt(name: String, value: Int) {
        MPVLib.setPropertyInt(name, value)
    }

    actual fun setPropertyBoolean(name: String, value: Boolean) {
        MPVLib.setPropertyBoolean(name, value)
    }

    actual fun setPropertyDouble(name: String, value: Double) {
        MPVLib.setPropertyDouble(name, value)
    }

    actual fun setPropertyString(name: String, value: String) {
        MPVLib.setPropertyString(name, value)
    }

    actual fun observeProperty(name: String, format: MPVFormat) {
        MPVLib.observeProperty(name, format.ordinal)
    }

    actual fun grabThumbnail(dimension: Int): Bitmap? = MPVLib.grabThumbnail(dimension)
}

actual fun platformMPV(): MPV = MPV()
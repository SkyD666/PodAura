package com.skyd.podaura.ui.player.mpv

import co.touchlab.kermit.Logger
import coil3.Bitmap
import com.skyd.fundation.di.get
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.MpvConfigDirPreference
import com.skyd.podaura.ui.PlatformSurfaceHolder
import `is`.xyz.mpv.MPVLib
import java.util.concurrent.ConcurrentHashMap

actual class MPV {
    private val logger = Logger.withTag("MPV")

    // Listeners are registered/unregistered from the UI thread while mpv dispatches on its own
    // thread, so this map must be concurrent.
    private val eventListeners = ConcurrentHashMap<EventListener, MPVLib.EventObserver>()

    actual fun initialize() {
        MPVLib.create(get(), dataStore.getOrDefault(MpvConfigDirPreference))
        MPVLib.init()
        option("vo", "null")
        option("ao", "audiotrack,opensles")
        option("gpu-context", "android")
    }

    actual fun destroy() {
        // Unregister before destroying, and drop our own bookkeeping so a later re-initialize
        // does not start out holding observers that mpv no longer knows about.
        eventListeners.values.forEach { MPVLib.removeObserver(it) }
        eventListeners.clear()
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
            // These callbacks are invoked by mpv's native event thread via JNI. An exception
            // escaping into native code becomes a pending JNI exception and aborts the entire
            // process, so never let one out of here.
            private inline fun guarded(block: () -> Unit) {
                runCatching(block).onFailure {
                    logger.e(throwable = it) { "Exception in mpv event callback" }
                }
            }

            override fun eventProperty(p0: String) = guarded {
                listener.onPropertyChange(p0)
            }

            override fun eventProperty(p0: String, p1: Long) = guarded {
                listener.onPropertyChange(p0, p1)
            }

            override fun eventProperty(p0: String, p1: Boolean) = guarded {
                listener.onPropertyChange(p0, p1)
            }

            override fun eventProperty(p0: String, p1: String) = guarded {
                listener.onPropertyChange(p0, p1)
            }

            override fun eventProperty(p0: String, p1: Double) = guarded {
                listener.onPropertyChange(p0, p1)
            }

            override fun event(p0: Int) = guarded {
                listener.onEvent(p0)
            }

            override fun efEvent(p0: String) {
            }

        }
        // putIfAbsent guards against registering the same listener twice (which would deliver
        // every event twice and leak the first observer, since the map only keeps one per key).
        val previous = eventListeners.putIfAbsent(listener, eventObserver)
        if (previous == null) {
            MPVLib.addObserver(eventObserver)
        }
    }

    actual fun removeEventListener(listener: EventListener) {
        eventListeners.remove(listener)?.let { MPVLib.removeObserver(it) }
    }

    actual fun command(vararg command: String) {
        MPVLib.command(command)
    }

    actual fun option(key: String, value: String) {
        MPVLib.setOptionString(key, value)
    }

    // MPVLib returns boxed types that are null while a property is unavailable (e.g. most
    // playback properties in idle mode). The unguarded delegation used to throw the Kotlin
    // non-null intrinsic NPE inside mpv's JNI event callback, which left a pending exception
    // in native code and aborted the whole process. Default like the JVM/mediamp backend,
    // whose JNI getters return primitive 0/false on unavailable properties.
    actual fun getPropertyInt(name: String): Int = MPVLib.getPropertyInt(name) ?: 0
    actual fun getPropertyBoolean(name: String): Boolean =
        MPVLib.getPropertyBoolean(name) ?: false

    actual fun getPropertyDouble(name: String): Double = MPVLib.getPropertyDouble(name) ?: 0.0
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
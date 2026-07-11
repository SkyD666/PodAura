@file:Suppress("INVISIBLE_REFERENCE")

package com.skyd.podaura.ui.player.mpv

import coil3.Bitmap
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.ui.PlatformSurfaceHolder
import kotlinx.coroutines.Dispatchers
import org.openani.mediamp.mpv.MPVHandle
import org.openani.mediamp.mpv.MpvMediampPlayer
import java.util.concurrent.CopyOnWriteArraySet

actual class MPV {
    val player by lazy {
        MpvMediampPlayer(Any(), Dispatchers.Default)
    }

    private val mpvHandle: MPVHandle
        get() = player.impl as MPVHandle

    // mediamp only supports a single EventListener, so one fan-out listener is registered and it
    // forwards to this set. CopyOnWriteArraySet because mpv dispatches on its own thread while
    // the UI thread adds/removes listeners.
    private val eventListeners = CopyOnWriteArraySet<EventListener>()

    private val fanOutEventListener = object : org.openani.mediamp.mpv.EventListener {
        override fun onPropertyChange(name: String) =
            eventListeners.forEach { it.onPropertyChange(name) }

        override fun onPropertyChange(name: String, value: Boolean) =
            eventListeners.forEach { it.onPropertyChange(name, value) }

        override fun onPropertyChange(name: String, value: Long) =
            eventListeners.forEach { it.onPropertyChange(name, value) }

        override fun onPropertyChange(name: String, value: Double) =
            eventListeners.forEach { it.onPropertyChange(name, value) }

        override fun onPropertyChange(name: String, value: String) =
            eventListeners.forEach { it.onPropertyChange(name, value) }

        override fun onEndFile(reason: Int, mpvError: Int) {
            // Intentionally not forwarded: mediamp also raises MPVEvent.END_FILE through
            // onEvent(), and bridging here as well would deliver it twice (double savePosition).
        }

        override fun onEvent(event: Int) = eventListeners.forEach { it.onEvent(event) }
    }

    actual fun initialize() {
        mpvHandle.initialize()
        mpvHandle.setEventListener(fanOutEventListener)
        option("vo", "libmpv")

        if (platform == Platform.macOS_Jvm) {
            option("ao", "coreaudio")
        } else if (platform == Platform.Windows) {
            option("ao", "wasapi")
        }
    }

    actual fun destroy() {
        mpvHandle.destroy()
        eventListeners.clear()
    }

    actual fun attachSurface(surfaceHolder: PlatformSurfaceHolder) {
        org.openani.mediamp.mpv.attachSurface(mpvHandle.ptr, Any())
    }

    actual fun detachSurface() {
        org.openani.mediamp.mpv.detachSurface(mpvHandle.ptr)
    }

    actual fun addEventListener(listener: EventListener) {
        eventListeners += listener
    }

    actual fun removeEventListener(listener: EventListener) {
        eventListeners -= listener
    }

    actual fun command(vararg command: String) {
        mpvHandle.command(*command)
    }

    actual fun option(key: String, value: String) {
        mpvHandle.option(key, value)
    }

    actual fun getPropertyInt(name: String): Int = mpvHandle.getPropertyInt(name)
    actual fun getPropertyBoolean(name: String): Boolean = mpvHandle.getPropertyBoolean(name)
    actual fun getPropertyDouble(name: String): Double = mpvHandle.getPropertyDouble(name)
    actual fun getPropertyString(name: String): String? = mpvHandle.getPropertyString(name)
    actual fun setPropertyInt(name: String, value: Int) {
        mpvHandle.setPropertyInt(name, value)
    }

    actual fun setPropertyBoolean(name: String, value: Boolean) {
        mpvHandle.setPropertyBoolean(name, value)
    }

    actual fun setPropertyDouble(name: String, value: Double) {
        mpvHandle.setPropertyDouble(name, value)
    }

    actual fun setPropertyString(name: String, value: String) {
        mpvHandle.setPropertyString(name, value)
    }

    actual fun observeProperty(name: String, format: MPVFormat) {
        mpvHandle.observeProperty(name, format.toMediampFormat())
    }

    // Match by name first: a mediamp update that reorders or inserts an entry would make the plain
    // `entries[ordinal]` lookup silently observe the wrong format, or throw IndexOutOfBounds.
    private fun MPVFormat.toMediampFormat(): org.openani.mediamp.mpv.MPVFormat {
        val entries = org.openani.mediamp.mpv.MPVFormat.entries
        return entries.firstOrNull { it.name == name }
            ?: entries.getOrNull(ordinal)
            ?: entries.first()
    }

    actual fun grabThumbnail(dimension: Int): Bitmap? = null
}

actual fun platformMPV(): MPV = MPV()
@file:Suppress("INVISIBLE_REFERENCE")

package com.skyd.podaura.ui.player.mpv

import coil3.Bitmap
import com.skyd.podaura.ui.PlatformSurfaceHolder
import kotlinx.coroutines.Dispatchers
import org.openani.mediamp.mpv.MPVHandle
import org.openani.mediamp.mpv.MpvMediampPlayer

actual class MPV {
    val player by lazy {
        MpvMediampPlayer(Any(), Dispatchers.Default)
    }

    private val mpvHandle: MPVHandle
        get() = player.impl as MPVHandle

    private val eventListeners =
        mutableMapOf<EventListener, org.openani.mediamp.mpv.EventListener>()
    private val defaultEventListener by lazy {
        object : org.openani.mediamp.mpv.EventListener {
            override fun onPropertyChange(name: String) {
                eventListeners.forEach { (listener, _) -> listener.onPropertyChange(name) }
            }

            override fun onPropertyChange(name: String, value: Boolean) {
                eventListeners.forEach { (listener, _) -> listener.onPropertyChange(name, value) }
            }

            override fun onPropertyChange(name: String, value: Long) {
                eventListeners.forEach { (listener, _) -> listener.onPropertyChange(name, value) }
            }

            override fun onPropertyChange(name: String, value: Double) {
                eventListeners.forEach { (listener, _) -> listener.onPropertyChange(name, value) }
            }

            override fun onPropertyChange(name: String, value: String) {
                eventListeners.forEach { (listener, _) -> listener.onPropertyChange(name, value) }
            }

            override fun onEndFile(reason: Int, mpvError: Int) {
            }

            override fun onEvent(event: Int) {
                eventListeners.forEach { (listener, _) -> listener.onEvent(event) }
            }
        }
    }

    actual fun initialize() {
        mpvHandle.initialize()
        mpvHandle.setEventListener(defaultEventListener)
    }

    actual fun destroy() {
        mpvHandle.destroy()
    }

    actual fun attachSurface(surfaceHolder: PlatformSurfaceHolder) {
        org.openani.mediamp.mpv.attachSurface(mpvHandle.ptr, Any())
    }

    actual fun detachSurface() {
        org.openani.mediamp.mpv.detachSurface(mpvHandle.ptr)
    }

    actual fun addEventListener(listener: EventListener) {
        eventListeners += listener to object : org.openani.mediamp.mpv.EventListener {
            override fun onPropertyChange(name: String) {
                listener.onPropertyChange(name)
            }

            override fun onPropertyChange(name: String, value: Boolean) {
                listener.onPropertyChange(name, value)
            }

            override fun onPropertyChange(name: String, value: Long) {
                listener.onPropertyChange(name, value)
            }

            override fun onPropertyChange(name: String, value: Double) {
                listener.onPropertyChange(name, value)
            }

            override fun onPropertyChange(name: String, value: String) {
                listener.onPropertyChange(name, value)
            }

            override fun onEndFile(reason: Int, mpvError: Int) {
            }

            override fun onEvent(event: Int) {
                listener.onEvent(event)
            }
        }
    }

    actual fun removeEventListener(listener: EventListener) {
        eventListeners.remove(listener)
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
        mpvHandle.observeProperty(name, org.openani.mediamp.mpv.MPVFormat.entries[format.ordinal])
    }

    actual fun grabThumbnail(dimension: Int): Bitmap? = null
}

actual fun platformMPV(): MPV = MPV()
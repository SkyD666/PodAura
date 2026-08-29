@file:Suppress("INVISIBLE_REFERENCE")

package com.skyd.podaura.ui.player.mpv

import coil3.Bitmap
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.ui.PlatformSurfaceHolder
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Image
import org.jetbrains.skiko.SkiaLayer
import org.openani.mediamp.mpv.MPVHandle
import org.openani.mediamp.mpv.RenderUpdateListener
import org.openani.mediamp.mpv.internal.MpvRenderContextHost
import org.openani.mediamp.mpv.internal.MpvRenderContextLifecycle
import org.openani.mediamp.mpv.internal.MpvSurfaceBackend
import org.openani.mediamp.mpv.internal.MpvSurfaceConsumer
import org.openani.mediamp.mpv.internal.currentSurfaceBackend
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean
import org.openani.mediamp.mpv.EventListener as MediampEventListener

actual class MPV {
    // MPVHandle's constructor only creates mpv. Unlike MpvMediampPlayer's constructor it
    // deliberately does not call mpv_initialize(), allowing common configuration to be applied
    // first.
    private val mpvHandle = MPVHandle(Any())
    private val ringBackend: MpvSurfaceBackend? = currentSurfaceBackend()
    private val surfaceRing: MpvSurfaceConsumer? = ringBackend?.createSurfaceConsumer(mpvHandle.ptr)
    private val pendingCommandsLock = Any()
    private val pendingCommands = mutableListOf<Array<out String>>()
    private val closed = AtomicBoolean(false)
    private val playbackSessionActive = AtomicBoolean(false)

    internal val renderContextLifecycle: MpvRenderContextLifecycle? =
        ringBackend?.createRenderContextLifecycle(
            object : MpvRenderContextHost {
                override val handle: MPVHandle get() = mpvHandle
                override fun hasActivePlaybackSession(): Boolean = playbackSessionActive.get()
                override fun onRenderContextReady() = flushPendingCommands()
                override fun invalidateSurfaceRingForEnvironmentChange() {
                    surfaceRing?.invalidateForRenderEnvironmentChange()
                }
            },
        )

    // mediamp only supports a single EventListener, so one fan-out listener is registered and it
    // forwards to this set. CopyOnWriteArraySet because mpv dispatches on its own thread while
    // the UI thread adds/removes listeners.
    private val eventListeners = CopyOnWriteArraySet<EventListener>()

    private val fanOutEventListener = object : MediampEventListener {
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

        override fun onStartFile(playlistEntryId: Long) {
        }

        override fun onEndFile(reason: Int, mpvError: Int, playlistEntryId: Long) {
            eventListeners.forEach { it.onEndFile(reason, mpvError, playlistEntryId) }
        }

        override fun onEvent(event: Int) {
            // mediamp sends both callbacks for END_FILE. The rich callback above is the canonical
            // one so saving playback position and state reduction happen exactly once.
            if (event != MPVEvent.END_FILE) eventListeners.forEach { it.onEvent(event) }
        }
    }

    actual fun initialize() {
        option("vo", "libmpv")
        option("gpu-dumb-mode", "no")
        option("target-prim", "bt.709")
        option("target-trc", "srgb")
        option("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")
        option("volume-max", "200")
        option("vd-lavc-film-grain", "cpu")

        when (platform) {
            Platform.macOS_Jvm -> {
                option("ao", "coreaudio")
            }
            Platform.Windows -> {
                option("ao", "wasapi")
            }
            Platform.Linux -> {
                option("ao", "pulse,alsa")
            }
            else -> {}
        }
        // Match mediamp's initialization sequence: install its event bridge before
        // mpv_initialize(), then apply the post-init lifecycle options before creating the
        // platform render context. Creating the render context first and changing VO lifecycle
        // options afterwards leaves the macOS surface alive but without renderable frames.
        mpvHandle.setEventListener(fanOutEventListener)
        check(mpvHandle.initialize()) { "Failed to initialize mpv" }
        option("save-position-on-quit", "no")
        option("force-window", "no")
        option("idle", "yes")
        option("keep-open", "always")
        renderContextLifecycle?.initialize()
    }

    actual fun destroy() {
        if (!closed.compareAndSet(false, true)) return
        synchronized(pendingCommandsLock) {
            pendingCommands.clear()
        }
        playbackSessionActive.set(false)
        // The surface ring keeps the raw handle pointer, so release it before finalizing the
        // handle. Its composable may dispose one snapshot later and will see closed == true.
        surfaceRing?.release()
        mpvHandle.destroy()
        // destroy() terminates mpv; close() releases the native wrapper itself.
        mpvHandle.close()
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
        if (closed.get()) return
        synchronized(pendingCommandsLock) {
            if (pendingCommands.isNotEmpty()) {
                pendingCommands += command
                return
            }
            val commandName = command.firstOrNull()
            if ((commandName == "loadfile" || commandName == "loadlist") &&
                renderContextLifecycle?.ensureReadyForLoad() == false
            ) {
                pendingCommands += command
                return
            }
        }
        updatePlaybackSession(command)
        mpvHandle.command(*command)
    }

    private fun flushPendingCommands() {
        val commands = synchronized(pendingCommandsLock) {
            pendingCommands.toList().also { pendingCommands.clear() }
        }
        if (closed.get()) return
        commands.forEach {
            updatePlaybackSession(it)
            mpvHandle.command(*it)
        }
    }

    private fun updatePlaybackSession(command: Array<out String>) {
        when (command.firstOrNull()) {
            "loadfile", "loadlist" -> playbackSessionActive.set(true)
            "stop" -> playbackSessionActive.set(false)
        }
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

    // Do not expose mediamp's internal SkiaRenderDeviceInterop as this method's return type.
    // With INVISIBLE_REFERENCE suppression Kotlin 2.4 otherwise emits a bogus checkcast Void at
    // the call site, which turns every valid SkiaMetalInterop into ClassCastException.
    internal fun createSkiaInterop(layer: SkiaLayer): Any? {
        if (closed.get()) return null
        return ringBackend?.createSkiaInterop(layer)
    }

    internal fun requestSurface(width: Int, height: Int, devicePtr: Long): Boolean =
        if (closed.get()) false
        else surfaceRing?.requestSurface(width, height, devicePtr) ?: false

    internal fun refreshDeviceIfChanged(devicePtr: Long) {
        if (!closed.get()) surfaceRing?.refreshDeviceIfChanged(devicePtr)
    }

    internal fun currentFrameImage(directContext: DirectContext): Image? =
        if (closed.get()) null else surfaceRing?.currentFrameImage(directContext)

    internal fun setRenderUpdateListener(listener: (() -> Unit)?): Boolean {
        if (closed.get()) return false
        val nativeListener: RenderUpdateListener? = listener?.let { callback ->
            object : RenderUpdateListener {
                override fun onRenderUpdate() = callback()
            }
        }
        return mpvHandle.setRenderUpdateListener(nativeListener)
    }

    internal fun releaseSurface() {
        if (!closed.get()) surfaceRing?.release()
    }
}

actual fun platformMPV(): MPV = MPV()

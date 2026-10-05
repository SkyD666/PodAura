package com.skyd.podaura.ui.player.mpv

import co.touchlab.kermit.Logger
import coil3.Bitmap
import com.skyd.podaura.libmpv.MPV_END_FILE_REASON_ERROR
import com.skyd.podaura.libmpv.MPV_ERROR_VO_INIT_FAILED
import com.skyd.podaura.libmpv.MPV_EVENT_END_FILE
import com.skyd.podaura.libmpv.MPV_EVENT_FILE_LOADED
import com.skyd.podaura.libmpv.MPV_EVENT_HOOK
import com.skyd.podaura.libmpv.MPV_EVENT_NONE
import com.skyd.podaura.libmpv.MPV_EVENT_PROPERTY_CHANGE
import com.skyd.podaura.libmpv.MPV_EVENT_VIDEO_RECONFIG
import com.skyd.podaura.libmpv.MPV_FORMAT_DOUBLE
import com.skyd.podaura.libmpv.MPV_FORMAT_FLAG
import com.skyd.podaura.libmpv.MPV_FORMAT_INT64
import com.skyd.podaura.libmpv.MPV_FORMAT_NODE
import com.skyd.podaura.libmpv.MPV_FORMAT_NODE_ARRAY
import com.skyd.podaura.libmpv.MPV_FORMAT_STRING
import com.skyd.podaura.libmpv.mpv_command
import com.skyd.podaura.libmpv.mpv_create
import com.skyd.podaura.libmpv.mpv_error_string
import com.skyd.podaura.libmpv.mpv_event_end_file
import com.skyd.podaura.libmpv.mpv_event_hook
import com.skyd.podaura.libmpv.mpv_event_property
import com.skyd.podaura.libmpv.mpv_free
import com.skyd.podaura.libmpv.mpv_get_property
import com.skyd.podaura.libmpv.mpv_get_property_string
import com.skyd.podaura.libmpv.mpv_hook_add
import com.skyd.podaura.libmpv.mpv_hook_continue
import com.skyd.podaura.libmpv.mpv_initialize
import com.skyd.podaura.libmpv.mpv_node
import com.skyd.podaura.libmpv.mpv_node_list
import com.skyd.podaura.libmpv.mpv_observe_property
import com.skyd.podaura.libmpv.mpv_set_option_string
import com.skyd.podaura.libmpv.mpv_set_property
import com.skyd.podaura.libmpv.mpv_set_property_string
import com.skyd.podaura.libmpv.mpv_set_wakeup_callback
import com.skyd.podaura.libmpv.mpv_terminate_destroy
import com.skyd.podaura.libmpv.mpv_wait_event
import com.skyd.podaura.ui.PlatformSurfaceHolder
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.Foundation.NSRecursiveLock

actual class MPV {
    private val lock = NSRecursiveLock()
    private var handle = checkNotNull(mpv_create()) { "Unable to create libmpv" }
    private var closed = false
    private var initialized = false
    private val listeners = mutableSetOf<EventListener>()
    private val wakeups = Channel<Unit>(Channel.CONFLATED)
    private val callback = StableRef.create(wakeups)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var renderer: AppleMpvRenderer? = null
    private var requestedVideoTrack = "auto"
    private var requestHeaders: Map<String, Map<String, String>> = emptyMap()

    internal fun updateRequestHeaders(headers: Map<String, Map<String, String>>) =
        locked { requestHeaders = headers }

    private fun <T> locked(block: () -> T): T {
        lock.lock()
        return try {
            block()
        } finally {
            lock.unlock()
        }
    }

    actual fun initialize(): Unit = locked {
        check(!closed)
        // These options belong to the host, even when a user config selects a desktop output.
        option("vo", "null")
        option("ao", appleMpvAudioOutput)
        option("gpu-api", "vulkan")
        option("gpu-context", appleMpvGpuContext)
        option("video-sync", "audio")
        option("input-default-bindings", "yes")
        checkResult(mpv_initialize(handle))
        initialized = true
        requestedVideoTrack = getPropertyString("vid") ?: "auto"
        checkResult(mpv_hook_add(handle, 0u, "on_load", 0))
        mpv_set_wakeup_callback(handle, staticCFunction { context ->
            context?.asStableRef<Channel<Unit>>()?.get()?.trySend(Unit)
            Unit
        }, callback.asCPointer())
        scope.launch {
            for (ignored in wakeups) {
                while (isActive && locked { !closed && readEvent() }) {
                }
            }
        }
    }

    actual fun addEventListener(listener: EventListener) {
        locked { listeners += listener }
    }

    actual fun removeEventListener(listener: EventListener) {
        locked { listeners -= listener }
    }

    actual fun command(vararg command: String) = locked {
        if (closed) return@locked
        memScoped {
            val args = allocArray<CPointerVar<ByteVar>>(command.size + 1)
            command.forEachIndexed { index, argument ->
                val value = if (command.firstOrNull() == "cycle-values" && index >= 2) {
                    platformOptionValue(command[1], argument)
                } else argument
                args[index] = value.cstr.ptr
            }
            args[command.size] = null
            report(mpv_command(handle, args), command.firstOrNull().orEmpty())
        }
    }

    actual fun option(key: String, value: String) = locked {
        if (!closed) {
            if (initialized) setPropertyString(key, value)
            else report(mpv_set_option_string(handle, key, platformOptionValue(key, value)), key)
        }
    }

    actual fun getPropertyInt(name: String): Int = locked {
        if (closed) return@locked 0
        memScoped {
            val result = alloc<LongVar>()
            if (mpv_get_property(handle, name, MPV_FORMAT_INT64, result.ptr) < 0) 0
            else result.value.toInt()
        }
    }

    actual fun getPropertyBoolean(name: String): Boolean = locked {
        if (closed) return@locked false
        memScoped {
            val result = alloc<IntVar>()
            mpv_get_property(handle, name, MPV_FORMAT_FLAG, result.ptr) >= 0 && result.value != 0
        }
    }

    actual fun getPropertyDouble(name: String): Double = locked {
        if (closed) return@locked 0.0
        memScoped {
            val result = alloc<DoubleVar>()
            if (mpv_get_property(handle, name, MPV_FORMAT_DOUBLE, result.ptr) < 0) 0.0
            else result.value
        }
    }

    actual fun getPropertyString(name: String): String? = locked {
        if (closed) return@locked null
        val value = mpv_get_property_string(handle, name) ?: return@locked null
        try {
            value.toKString()
        } finally {
            mpv_free(value)
        }
    }

    actual fun setPropertyInt(name: String, value: Int) = setPropertyString(name, value.toString())
    actual fun setPropertyBoolean(name: String, value: Boolean) =
        setPropertyString(name, if (value) "yes" else "no")

    actual fun setPropertyDouble(name: String, value: Double) =
        setPropertyString(name, value.toString())

    actual fun setPropertyString(name: String, value: String) = locked {
        if (!closed) {
            val actualValue = platformOptionValue(name, value)
            val restoreVideo = name == "vo" && actualValue == "gpu-next" &&
                    getPropertyString("vo") != "gpu-next"
            // Keep the last stream selected: vid=no without audio produces artificial EOF
            // and can rewind a keep-open file or advance a multi-file queue.
            val canSuspendVideo = name == "vo" && getPropertyInt("aid") > 0
            if (name == "vo" && (actualValue == "null" || restoreVideo) && canSuspendVideo) {
                report(mpv_set_property_string(handle, "vid", "no"), "suspend video")
            }
            report(mpv_set_property_string(handle, name, actualValue), name)
            if (restoreVideo) {
                // The decoder must see the GPU output on its first hardware-decoding probe.
                report(mpv_set_property_string(handle, "vid", requestedVideoTrack), "restore video")
                if (!canSuspendVideo) {
                    // A video-only decoder stays selected while headless. Reprobe hardware
                    // without deselecting its last stream now that the GPU is available.
                    val hwdec = getPropertyString("hwdec")
                    if (hwdec != null && hwdec != "no") {
                        report(mpv_set_property_string(handle, "hwdec", "no"), "reset decoder")
                        report(mpv_set_property_string(handle, "hwdec", hwdec), "restore decoder")
                    }
                }
            }
        }
    }

    actual fun observeProperty(name: String, format: MPVFormat) = locked {
        if (!closed) report(mpv_observe_property(handle, 0u, name, format.ordinal.toUInt()), name)
    }

    actual fun attachSurface(surfaceHolder: PlatformSurfaceHolder) = locked {
        if (closed || !surfaceHolder.isMpvSurfaceActive()) return@locked
        val output = renderer ?: AppleMpvRenderer(handle).also { renderer = it }
        output.videoSize(
            getPropertyInt("dwidth"),
            getPropertyInt("dheight"),
            getPropertyInt("video-out-params/rotate")
        )
        output.attach(surfaceHolder)
    }

    internal fun resizeRenderingSurface(): Unit = locked { renderer?.resize() }

    internal fun setRenderingActive(active: Boolean): Unit = locked { renderer?.setActive(active) }

    actual fun detachSurface(): Unit = locked {
        if (!closed) setPropertyString("vo", "null")
        renderer?.close()
        renderer = null
    }

    actual fun destroy() = locked {
        if (closed) return@locked
        setPropertyString("vo", "null")
        closed = true
        listeners.clear()
        mpv_set_wakeup_callback(handle, null, null)
        renderer?.close()
        renderer = null
        mpv_terminate_destroy(handle)
        callback.dispose()
        wakeups.close()
        scope.cancel()
    }

    // Thumbnail extraction and screenshots are outside the first Apple release.
    actual fun grabThumbnail(dimension: Int): Bitmap? = null

    private fun readEvent(): Boolean {
        if (renderer?.failed == true) {
            setPropertyBoolean("pause", true)
            detachSurface()
            listeners.toList().forEach {
                it.onEndFile(MPV_END_FILE_REASON_ERROR.toInt(), MPV_ERROR_VO_INIT_FAILED, -1L)
            }
        }
        val event = mpv_wait_event(handle, 0.0)?.pointed ?: return false
        if (event.event_id == MPV_EVENT_NONE) return false
        val current = listeners.toList()
        if (event.event_id == MPV_EVENT_VIDEO_RECONFIG) {
            renderer?.videoSize(
                getPropertyInt("dwidth"),
                getPropertyInt("dheight"),
                getPropertyInt("video-out-params/rotate")
            )
        }
        when (event.event_id) {
            MPV_EVENT_PROPERTY_CHANGE -> {
                val property = event.data?.reinterpret<mpv_event_property>()?.pointed ?: return true
                val name = property.name?.toKString() ?: return true
                val data = property.data
                current.forEach { listener ->
                    when {
                        data == null -> listener.onPropertyChange(name)
                        property.format == MPV_FORMAT_FLAG -> listener.onPropertyChange(
                            name,
                            data.reinterpret<IntVar>().pointed.value != 0
                        )

                        property.format == MPV_FORMAT_INT64 -> listener.onPropertyChange(
                            name,
                            data.reinterpret<LongVar>().pointed.value
                        )

                        property.format == MPV_FORMAT_DOUBLE -> listener.onPropertyChange(
                            name,
                            data.reinterpret<DoubleVar>().pointed.value
                        )

                        property.format == MPV_FORMAT_STRING -> data.reinterpret<CPointerVar<ByteVar>>().pointed.value?.toKString()
                            ?.let { listener.onPropertyChange(name, it) }

                        else -> listener.onPropertyChange(name)
                    }
                }
            }

            MPV_EVENT_HOOK -> {
                val hook = event.data?.reinterpret<mpv_event_hook>()?.pointed ?: return true
                try {
                    applyRequestHeaders()
                    // A previous detach may have disabled video. Every new file must be
                    // allowed to load before the UI can create its first surface.
                    report(
                        mpv_set_property_string(handle, "vid", requestedVideoTrack),
                        "select video for loading"
                    )
                } finally {
                    mpv_hook_continue(handle, hook.id)
                }
            }

            MPV_EVENT_FILE_LOADED -> {
                // Only suspend after mpv has actually selected an audio stream. Disabling
                // video before this point can leave video-only files with nothing to play.
                if (renderer == null && getPropertyInt("aid") > 0) {
                    report(mpv_set_property_string(handle, "vid", "no"), "suspend video")
                }
                current.forEach { it.onEvent(event.event_id.toInt()) }
            }

            MPV_EVENT_END_FILE -> {
                val end = event.data?.reinterpret<mpv_event_end_file>()?.pointed ?: return true
                current.forEach {
                    it.onEndFile(
                        end.reason.toInt(),
                        end.error,
                        end.playlist_entry_id
                    )
                }
            }

            else -> current.forEach { it.onEvent(event.event_id.toInt()) }
        }
        return true
    }

    private fun applyRequestHeaders() = memScoped {
        val path = getPropertyString("path") ?: return@memScoped
        val headers = requestHeaders[path].orEmpty()
            .filter { (key, value) -> key.isNotEmpty() && key.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in "!#$%&'*+-.^_`|~" } && value.none { it == '\r' || it == '\n' || it == '\u0000' } }
        if (headers.isEmpty()) return@memScoped
        val values = allocArray<mpv_node>(headers.size)
        headers.entries.forEachIndexed { index, (name, value) ->
            values[index].format = MPV_FORMAT_STRING
            values[index].u.string = "$name: $value".cstr.ptr
        }
        val list = alloc<mpv_node_list>()
        list.num = headers.size
        list.values = values
        list.keys = null
        val node = alloc<mpv_node>()
        node.format = MPV_FORMAT_NODE_ARRAY
        node.u.list = list.ptr
        report(
            mpv_set_property(
                handle,
                "file-local-options/http-header-fields",
                MPV_FORMAT_NODE,
                node.ptr
            ), "request headers"
        )
    }

    private fun platformOptionValue(name: String, value: String): String = when {
        name == "vo" && value != "null" ->
            if (renderer != null) "gpu-next" else "null"

        name == "gpu-api" -> "vulkan"
        name == "gpu-context" -> appleMpvGpuContext

        name == "hwdec" && value == "auto" -> "videotoolbox"
        name == "vid" -> {
            requestedVideoTrack = value
            if (initialized && renderer == null && getPropertyInt("aid") > 0) "no" else value
        }

        else -> value
    }

    private fun checkResult(result: Int) {
        check(result >= 0) { mpv_error_string(result)?.toKString() ?: "libmpv error $result" }
    }

    private fun report(result: Int, operation: String) {
        if (result < 0) Logger.w(
            "libmpv $operation: ${mpv_error_string(result)?.toKString()}",
            tag = "MPV"
        )
    }
}

actual fun platformMPV(): MPV = MPV()

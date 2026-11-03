package com.skyd.podaura.ui.player.volume.windows

import co.touchlab.kermit.Logger
import com.skyd.podaura.ui.player.mpv.MPV
import com.skyd.podaura.ui.player.mpv.MPVFormat
import com.skyd.podaura.ui.player.volume.PlayerVolumeBackend
import com.skyd.podaura.ui.player.volume.VolumeState
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.COM.COMUtils
import com.sun.jna.platform.win32.COM.Unknown
import com.sun.jna.platform.win32.Guid.CLSID
import com.sun.jna.platform.win32.Guid.IID
import com.sun.jna.platform.win32.Guid.REFIID
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.platform.win32.WTypes
import com.sun.jna.platform.win32.WinNT.HRESULT
import com.sun.jna.ptr.FloatByReference
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.collections.get

/** Own-process, non-cross-process WASAPI sessions; never opens IAudioEndpointVolume. */
internal class WindowsVolumeBackend(private val mpv: MPV) : PlayerVolumeBackend {
    override val needsPolling = true

    // The player's serial coroutine dispatcher can change OS threads. COM objects must not.
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "PodAura session volume").apply { isDaemon = true }
    }
    private val sessions = linkedMapOf<String, AudioSession>()
    private var enumerator: DeviceEnumerator? = null
    private var target = VolumeState()
    private var adoptedSystemVolume = false
    private var closed = false
    private var lastError: String? = null
    private val route = WindowsAudioRoute(
        bind = { mpv.setPropertyString("audio-device", it) },
        reload = { mpv.command("ao-reload") },
    )

    override fun initialize(saved: VolumeState) {
        target = saved
        mpv.observeProperty("current-ao", MPVFormat.MPV_FORMAT_STRING)
        mpv.observeProperty("audio-device", MPVFormat.MPV_FORMAT_STRING)
        mpv.observeProperty("audio-exclusive", MPVFormat.MPV_FORMAT_FLAG)
        read()
    }

    private fun supportsSessionVolume(): Boolean =
        mpv.getPropertyString("current-ao").let { it == null || it == "wasapi" || it == "null" } &&
                !mpv.getPropertyBoolean("audio-exclusive") &&
                mpv.getPropertyString("audio-out-params/format")?.startsWith("spdif") != true

    override fun read(): VolumeState? {
        if (!supportsSessionVolume()) return null
        return route.refresh(
            mpv.getPropertyString("audio-device"),
            mpv.getPropertyString("current-ao")
        ) {
            val device = resolveDeviceName(it)
            onWorker { refresh(device) }
        }
    }

    override fun apply(value: VolumeState) {
        if (!supportsSessionVolume()) return
        route.refresh(mpv.getPropertyString("audio-device"), mpv.getPropertyString("current-ao")) {
            val device = resolveDeviceName(it)
            onWorker {
                target = value.copy(volume = value.volume.coerceIn(0f, 100f))
                adoptedSystemVolume = true
                val ready = refresh(device, readSystemChanges = false)
                sessions.values.forEach { it.apply(target) }
                ready?.copy(volume = target)
            }
        }
    }

    // Also accept mpv's friendly-name configuration; IDs and numeric selectors are handled below.
    private fun resolveDeviceName(requested: String?): String? {
        val name = requested?.removePrefix("wasapi/") ?: return null
        repeat(mpv.getPropertyInt("audio-device-list/count")) { index ->
            if (mpv.getPropertyString("audio-device-list/$index/description") == name) {
                val id = mpv.getPropertyString("audio-device-list/$index/name")
                if (id?.startsWith("wasapi/") == true) return id
            }
        }
        return requested
    }

    private fun refresh(device: String?, readSystemChanges: Boolean = true): PreparedWindowsAudio? {
        val devices = checkNotNull(enumerator)
        val connected = devices.endpoints()
        try {
            val endpoints = connected.associateBy { it.id }
            val selectedId =
                resolveWindowsAudioDevice(device, endpoints.keys.toList(), devices.defaultId())
            if (readSystemChanges && sessions.keys.singleOrNull() != selectedId) {
                // Capture a last-moment mixer change on the old output before moving it.
                // An unplugged endpoint may already be invalid; retain the last known value then.
                sessions.values.singleOrNull()?.let { previous ->
                    runCatching { previous.read() }.getOrNull()?.let { target = it }
                }
            }
            sessions.keys.filterNot { it == selectedId }.forEach { sessions.remove(it)?.close() }
            val endpoint = endpoints[selectedId] ?: return null
            val selected = sessions.getOrPut(checkNotNull(selectedId)) {
                endpoint.openSession().also { session ->
                    try {
                        // Windows persists mixer changes while the app is closed. Adopt once;
                        // subsequent routes receive the current setting before mpv can use them.
                        if (!adoptedSystemVolume) {
                            target = session.read()
                            adoptedSystemVolume = true
                        }
                        session.apply(target)
                    } catch (error: Throwable) {
                        session.close()
                        throw error
                    }
                }
            }
            val actual = selected.read()
            if (readSystemChanges) target = actual
            return PreparedWindowsAudio(selectedId, actual)
        } finally {
            connected.forEach { it.close() }
        }
    }

    private fun <T> onWorker(action: () -> T): T? {
        if (closed) return null
        return try {
            worker.submit(Callable {
                if (enumerator == null) {
                    COMUtils.checkRC(
                        Ole32.INSTANCE.CoInitializeEx(
                            null,
                            Ole32.COINIT_MULTITHREADED
                        )
                    )
                    try {
                        enumerator = DeviceEnumerator.create()
                    } catch (error: Throwable) {
                        Ole32.INSTANCE.CoUninitialize()
                        throw error
                    }
                }
                action()
            }).get().also { lastError = null }
        } catch (error: Exception) {
            val cause = error.cause ?: error
            if (lastError != cause.message) {
                Logger.w(
                    throwable = cause,
                    tag = "WindowsVolume"
                ) { "Could not access the app audio session" }
                lastError = cause.message
            }
            // An invalidated endpoint will be opened afresh on the next refresh.
            worker.submit {
                sessions.values.forEach { it.close() }
                sessions.clear()
            }.get()
            null
        }
    }

    override fun close() {
        if (closed) return
        try {
            worker.submit {
                sessions.values.forEach { it.close() }
                sessions.clear()
                enumerator?.close()
                if (enumerator != null) Ole32.INSTANCE.CoUninitialize()
                enumerator = null
            }.get()
        } finally {
            closed = true
            worker.shutdown()
        }
    }
}

private open class AudioCom(pointer: Pointer) : Unknown(pointer), AutoCloseable {
    fun call(index: Int, vararg args: Any?) {
        COMUtils.checkRC(
            _invokeNativeObject(
                index,
                arrayOf(pointer, *args),
                HRESULT::class.java
            ) as HRESULT
        )
    }

    fun reference(index: Int, vararg args: Any?): Pointer {
        val out = PointerByReference()
        call(index, *args, out)
        return checkNotNull(out.value)
    }

    override fun close() {
        Release()
    }
}

private class DeviceEnumerator(pointer: Pointer) : AudioCom(pointer) {
    fun endpoints(): List<AudioEndpoint> = AudioCom(reference(3, 0, 1)).use { collection ->
        // eRender, DEVICE_STATE_ACTIVE. GetSimpleAudioVolume below opens only our session.
        val count = IntByReference()
        collection.call(3, count)
        val result = mutableListOf<AudioEndpoint>()
        try {
            repeat(count.value) { result += AudioEndpoint(collection.reference(4, it)) }
            result
        } catch (error: Throwable) {
            result.forEach { it.close() }
            throw error
        }
    }

    fun defaultId(): String? = runCatching {
        // eRender, eMultimedia, matching mpv's WASAPI default endpoint.
        AudioEndpoint(reference(4, 0, 1)).use { it.id }
    }.getOrNull()

    companion object {
        fun create(): DeviceEnumerator {
            val out = PointerByReference()
            COMUtils.checkRC(
                Ole32.INSTANCE.CoCreateInstance(
                    CLSID("{BCDE0395-E52F-467C-8E3D-C4579291692E}"), null, WTypes.CLSCTX_ALL,
                    IID("{A95664D2-9614-4F35-A746-DE8DB63617E6}"), out,
                )
            )
            return DeviceEnumerator(checkNotNull(out.value))
        }
    }
}

private class AudioEndpoint(pointer: Pointer) : AudioCom(pointer) {
    val id: String
        get() {
            val text = reference(5)
            return try {
                text.getWideString(0)
            } finally {
                Ole32.INSTANCE.CoTaskMemFree(text)
            }
        }

    fun openSession(): AudioSession = AudioCom(
        reference(
            3,
            REFIID(IID("{BFA971F1-4D5E-40BB-935E-967039BFBEE4}")),
            WTypes.CLSCTX_ALL,
            Pointer.NULL,
        )
    ).use { manager ->
        // NULL GUID = the process's default session, also used by mpv. FALSE excludes
        // cross-process sessions; another app's session and endpoint volume are unreachable.
        val control = AudioCom(manager.reference(3, Pointer.NULL, 0))
        try {
            AudioSession(control, AudioCom(manager.reference(4, Pointer.NULL, 0)))
        } catch (error: Throwable) {
            control.close()
            throw error
        }
    }
}

private class AudioSession(private val control: AudioCom, private val volume: AudioCom) :
    AutoCloseable {
    fun read(): VolumeState {
        val level = FloatByReference().also { volume.call(4, it) }.value
        val muted = IntByReference().also { volume.call(6, it) }.value != 0
        return VolumeState(volume = level * 100f, muted = muted)
    }

    fun apply(value: VolumeState) {
        if (value.muted) volume.call(5, 1, Pointer.NULL)
        volume.call(3, value.volume.coerceIn(0f, 100f) / 100f, Pointer.NULL)
        if (!value.muted) volume.call(5, 0, Pointer.NULL)
    }

    override fun close() {
        volume.close()
        control.close()
    }
}

package com.skyd.podaura.ui.player.volume.windows

import com.skyd.podaura.ui.player.volume.VolumeState

internal data class PreparedWindowsAudio(val deviceId: String, val volume: VolumeState)

/** Keep mpv on a prepared endpoint so it cannot start a new route before its session is muted. */
internal class WindowsAudioRoute(
    private val bind: (String) -> Unit,
    private val reload: () -> Unit,
) {
    private var requested: String? = null
    private var bound: String? = null

    fun refresh(
        configured: String?,
        currentAo: String?,
        prepare: (String?) -> PreparedWindowsAudio?,
    ): VolumeState? {
        if (configured != bound) requested = configured
        val ready = prepare(requested)
        // An unavailable route must not fall back to an unprepared system default.
        val next = "wasapi/${ready?.deviceId ?: "PodAura-unavailable-endpoint"}"
        if (configured != next) bind(next)
        else if (ready != null && currentAo == "null") reload()
        bound = next
        return ready?.volume
    }
}

internal fun resolveWindowsAudioDevice(
    requested: String?,
    deviceIds: List<String>,
    defaultId: String?,
): String? {
    val selector = requested?.removePrefix("wasapi/").orEmpty()
    if (selector in listOf("", "auto", "wasapi")) return defaultId?.takeIf(deviceIds::contains)
    // mpv lists shortened IDs, while IMMDevice::GetId returns the full endpoint ID.
    fun String.shortId() = removePrefix("{0.0.0.00000000}.")
    return deviceIds.firstOrNull { it.shortId() == selector.shortId() }
        ?: selector.toIntOrNull()?.let(deviceIds::getOrNull)
}

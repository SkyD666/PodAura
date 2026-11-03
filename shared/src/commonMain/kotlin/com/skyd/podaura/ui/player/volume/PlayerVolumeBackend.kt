package com.skyd.podaura.ui.player.volume

import com.skyd.podaura.ui.player.mpv.MPV
import com.skyd.podaura.ui.player.mpv.MPVFormat

/** Accessed only by the player's serialized command/lifecycle dispatcher. */
internal interface PlayerVolumeBackend {
    val needsPolling: Boolean get() = false
    fun initialize(saved: VolumeState)
    fun read(): VolumeState?
    fun apply(value: VolumeState)
    fun close() {}
}

internal expect fun createPlayerVolumeBackend(mpv: MPV): PlayerVolumeBackend

internal class MpvVolumeBackend(private val mpv: MPV) : PlayerVolumeBackend {
    override fun initialize(saved: VolumeState) {
        apply(saved)
        mpv.observeProperty("volume", MPVFormat.MPV_FORMAT_DOUBLE)
        mpv.observeProperty("mute", MPVFormat.MPV_FORMAT_FLAG)
    }

    override fun read(): VolumeState? {
        val volume = mpv.getPropertyString("volume")?.toFloatOrNull()
            ?.takeIf { it.isFinite() && it >= 0f } ?: return null
        val muted = when (mpv.getPropertyString("mute")) {
            "yes" -> true
            "no" -> false
            else -> return null
        }
        return VolumeState(volume = volume, muted = muted)
    }

    override fun apply(value: VolumeState) {
        // Set the gain before unmuting; preserve externally configured values above 100%.
        if (value.muted) mpv.setPropertyBoolean("mute", true)
        mpv.setPropertyDouble("volume", value.volume.toDouble())
        if (!value.muted) mpv.setPropertyBoolean("mute", false)
    }
}

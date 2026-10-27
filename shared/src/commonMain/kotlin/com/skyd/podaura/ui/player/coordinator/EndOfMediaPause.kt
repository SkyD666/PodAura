package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.player.LoopMode
import com.skyd.podaura.ui.player.mpv.MPVPlayer

/** Session-owned overrides that stop at EOF before native looping or playlist advancement. */
internal class EndOfMediaPause(private val player: MPVPlayer) {
    private var originalOptions: MutableMap<String, String>? = null
    private var pendingExternalQueue: Boolean? = null
    val enabled: Boolean get() = originalOptions != null

    fun setEnabled(enabled: Boolean) {
        if (enabled == this.enabled) return
        if (enabled) {
            val overrides = mapOf(
                "keep-open" to "always", "keep-open-pause" to "yes",
                "loop-file" to "no", "loop-playlist" to "no",
            )
            originalOptions = overrides.keys.associateWith {
                player.mpv.getPropertyString(it) ?: if (it == "keep-open-pause") "yes" else "no"
            }.toMutableMap()
            overrides.forEach { (name, value) -> player.mpv.setPropertyString(name, value) }
        } else {
            val options = originalOptions
            originalOptions = null
            options?.forEach { (name, value) -> player.mpv.setPropertyString(name, value) }
            pendingExternalQueue?.let(::setExternalQueue)
            pendingExternalQueue = null
        }
    }

    fun setLoopMode(mode: LoopMode) {
        val options = originalOptions
        if (options != null) {
            options["loop-playlist"] = if (mode == LoopMode.LoopPlaylist) "inf" else "no"
            options["loop-file"] = if (mode == LoopMode.LoopFile) "inf" else "no"
        } else when (mode) {
            LoopMode.LoopPlaylist -> player.loopPlaylist()
            LoopMode.LoopFile -> player.loopFile()
            LoopMode.None -> player.loopNo()
        }
    }

    fun skip(next: Boolean, play: Boolean, loopPlaylist: Boolean) {
        if (!enabled) {
            if (next) player.playlistNext(play) else player.playlistPrev(play)
            return
        }
        val count = player.playlistCount
        if (count == 0) return
        val position = if (player.isIdling) count - 1 else player.playlistPos
        val target = position + if (next) 1 else -1
        val index = if (loopPlaylist) (target + count) % count else target
        if (index !in 0 until count) return
        // Native looping is disabled by the guard; select the requested entry explicitly.
        player.mpv.command("playlist-play-index", index.toString())
        player.paused = !play
    }

    fun setExternalQueue(enabled: Boolean) {
        // Defer keep-open changes so the EOF guard stays in place throughout reconfiguration.
        if (this.enabled) pendingExternalQueue = enabled
        else player.setExternalQueue(enabled)
    }

    fun seek(position: Int) {
        if (enabled && player.duration > 0 && position >= player.duration) {
            // The UI reports whole seconds; reach the fractional EOF even while paused.
            player.mpv.setPropertyDouble("time-pos", player.mpv.getPropertyDouble("duration"))
        } else {
            player.seek(position)
        }
    }
}

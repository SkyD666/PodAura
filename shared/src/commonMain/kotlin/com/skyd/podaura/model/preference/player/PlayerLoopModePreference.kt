package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference
import com.skyd.podaura.ui.player.LoopMode
import kotlinx.coroutines.CoroutineScope

@Preference
object PlayerLoopModePreference : BasePreference<String>() {
    private const val PLAYER_LOOP_MODE = "playerLoopMode"

    const val LOOP_PLAYLIST = "LoopPlaylist"
    const val LOOP_FILE = "LoopFile"
    const val NONE = "None"

    val values = arrayOf(LOOP_PLAYLIST, LOOP_FILE, NONE)

    override val default = LOOP_PLAYLIST
    override val key = stringPreferencesKey(PLAYER_LOOP_MODE)

    fun toLoopMode(value: String): LoopMode = when (value) {
        LOOP_PLAYLIST -> LoopMode.LoopPlaylist
        LOOP_FILE -> LoopMode.LoopFile
        NONE -> LoopMode.None
        else -> LoopMode.None
    }

    fun fromLoopMode(value: LoopMode): String = when (value) {
        LoopMode.LoopPlaylist -> LOOP_PLAYLIST
        LoopMode.LoopFile -> LOOP_FILE
        LoopMode.None -> NONE
    }

    fun put(scope: CoroutineScope, value: LoopMode) {
        put(scope, fromLoopMode(value))
    }
}

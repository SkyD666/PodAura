package com.skyd.anivu.model.preference.player

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.ui.mpv.LoopMode
import com.skyd.ksp.preference.Preference
import kotlinx.coroutines.CoroutineScope

@Preference
object PlayerLoopModePreference : BasePreference<String> {
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

    fun put(context: Context, scope: CoroutineScope, value: LoopMode) {
        put(context, scope, fromLoopMode(value))
    }
}

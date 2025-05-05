package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.BasePreference
import com.skyd.podaura.model.preference.dataStore
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.player_backward_forward
import podaura.shared.generated.resources.player_backward_pause_forward
import podaura.shared.generated.resources.player_pause
import podaura.shared.generated.resources.unknown

@Preference
object PlayerDoubleTapPreference : BasePreference<String>() {
    private const val PLAYER_DOUBLE_TAP = "playerDoubleTap"

    const val PAUSE_PLAY = "PausePlay"
    const val BACKWARD_FORWARD = "BackwardForward"
    const val BACKWARD_PAUSE_PLAY_FORWARD = "BackwardPausePlayForward"

    val values = arrayOf(PAUSE_PLAY, BACKWARD_FORWARD, BACKWARD_PAUSE_PLAY_FORWARD)

    override val default = PAUSE_PLAY
    override val key = stringPreferencesKey(PLAYER_DOUBLE_TAP)

    suspend fun toDisplayName(
        value: String = dataStore.getOrDefault(this),
    ): String = when (value) {
        PAUSE_PLAY -> getString(Res.string.player_pause)
        BACKWARD_FORWARD -> getString(Res.string.player_backward_forward)
        BACKWARD_PAUSE_PLAY_FORWARD -> getString(Res.string.player_backward_pause_forward)
        else -> getString(Res.string.unknown)
    }
}

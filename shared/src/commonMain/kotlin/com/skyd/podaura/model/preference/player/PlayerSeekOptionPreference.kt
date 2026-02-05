package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.BasePreference
import com.skyd.podaura.model.preference.dataStore
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.player_seek_exact
import podaura.shared.generated.resources.player_seek_fast
import podaura.shared.generated.resources.unknown

@Preference
object PlayerSeekOptionPreference : BasePreference<String>() {
    private const val PLAYER_SEEK_OPTION = "playerSeekOption"

    const val FAST = "Fast"
    const val EXACT = "Exact"

    val values = arrayOf(FAST, EXACT)

    override val default = FAST
    override val key = stringPreferencesKey(PLAYER_SEEK_OPTION)

    fun isPrecise(
        value: String = dataStore.getOrDefault(this),
    ): Boolean = value == EXACT

    suspend fun toDisplayName(
        value: String = dataStore.getOrDefault(this),
    ): String = when (value) {
        FAST -> getString(Res.string.player_seek_fast)
        EXACT -> getString(Res.string.player_seek_exact)
        else -> getString(Res.string.unknown)
    }
}

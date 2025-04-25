package com.skyd.anivu.model.preference.proxy

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.anivu.model.preference.dataStore
import com.skyd.ksp.preference.Preference
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.proxy_mode_auto
import podaura.shared.generated.resources.proxy_mode_manual
import podaura.shared.generated.resources.unknown

@Preference
object ProxyModePreference : BasePreference<String>() {
    private const val PROXY_MODE = "proxyMode"

    const val AUTO_MODE = "Auto"
    const val MANUAL_MODE = "Manual"

    override val default = AUTO_MODE
    override val key = stringPreferencesKey(PROXY_MODE)

    val values = listOf(AUTO_MODE, MANUAL_MODE)

    suspend fun toDisplayName(
        value: String = dataStore.getOrDefault(this),
    ): String = when (value) {
        AUTO_MODE -> getString(Res.string.proxy_mode_auto)
        MANUAL_MODE -> getString(Res.string.proxy_mode_manual)
        else -> getString(Res.string.unknown)
    }
}

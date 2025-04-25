package com.skyd.anivu.model.preference.proxy

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object UseProxyPreference : BasePreference<Boolean>() {
    private const val USE_PROXY = "useProxy"

    override val default = true
    override val key = booleanPreferencesKey(USE_PROXY)
}

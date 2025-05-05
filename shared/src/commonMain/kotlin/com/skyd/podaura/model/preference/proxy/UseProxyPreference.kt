package com.skyd.podaura.model.preference.proxy

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object UseProxyPreference : BasePreference<Boolean>() {
    private const val USE_PROXY = "useProxy"

    override val default = true
    override val key = booleanPreferencesKey(USE_PROXY)
}

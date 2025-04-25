package com.skyd.anivu.model.preference.proxy

import androidx.datastore.preferences.core.intPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object ProxyPortPreference : BasePreference<Int>() {
    private const val PROXY_PORT = "proxyPort"

    override val default = 8080
    override val key = intPreferencesKey(PROXY_PORT)
}

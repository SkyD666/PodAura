package com.skyd.podaura.model.preference.proxy

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ProxyPasswordPreference : BasePreference<String>() {
    private const val PROXY_PASSWORD = "proxyPassword"

    override val default = ""
    override val key = stringPreferencesKey(PROXY_PASSWORD)
}

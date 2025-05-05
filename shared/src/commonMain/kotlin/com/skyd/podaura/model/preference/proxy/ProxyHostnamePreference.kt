package com.skyd.podaura.model.preference.proxy

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ProxyHostnamePreference : BasePreference<String>() {
    private const val PROXY_HOSTNAME = "proxyHostname"

    override val default = "127.0.0.1"
    override val key = stringPreferencesKey(PROXY_HOSTNAME)
}

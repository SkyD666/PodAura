package com.skyd.podaura.model.preference.proxy

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ProxyUsernamePreference : BasePreference<String>() {
    private const val PROXY_USERNAME = "proxyUsername"

    override val default = ""
    override val key = stringPreferencesKey(PROXY_USERNAME)
}

package com.skyd.anivu.model.preference.proxy

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.base.BasePreference

object ProxyUsernamePreference : BasePreference<String> {
    private const val PROXY_USERNAME = "proxyUsername"

    override val default = ""
    override val key = stringPreferencesKey(PROXY_USERNAME)
}

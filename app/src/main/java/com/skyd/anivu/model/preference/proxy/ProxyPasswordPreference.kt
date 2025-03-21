package com.skyd.anivu.model.preference.proxy

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.base.BasePreference

object ProxyPasswordPreference : BasePreference<String> {
    private const val PROXY_PASSWORD = "proxyPassword"

    override val default = ""
    override val key = stringPreferencesKey(PROXY_PASSWORD)
}

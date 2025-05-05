package com.skyd.podaura.model.preference.proxy

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ProxyTypePreference : BasePreference<String>() {
    private const val PROXY_TYPE = "proxyType"

    const val HTTP = "HTTP"
    const val SOCKS4 = "Socks4"
    const val SOCKS5 = "Socks5"

    override val default = HTTP
    override val key = stringPreferencesKey(PROXY_TYPE)

    val values = listOf(HTTP, SOCKS4, SOCKS5)
}

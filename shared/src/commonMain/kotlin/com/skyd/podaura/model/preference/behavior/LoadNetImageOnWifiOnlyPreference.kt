package com.skyd.podaura.model.preference.behavior

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object LoadNetImageOnWifiOnlyPreference : BasePreference<Boolean>() {
    private const val LOAD_NET_IMAGE_ON_WIFI_ONLY = "loadNetImageOnWifiOnly"

    override val default = false
    override val key = booleanPreferencesKey(LOAD_NET_IMAGE_ON_WIFI_ONLY)
}
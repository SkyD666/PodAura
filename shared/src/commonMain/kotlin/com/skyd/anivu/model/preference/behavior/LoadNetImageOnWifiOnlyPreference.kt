package com.skyd.anivu.model.preference.behavior

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object LoadNetImageOnWifiOnlyPreference : BasePreference<Boolean>() {
    private const val LOAD_NET_IMAGE_ON_WIFI_ONLY = "loadNetImageOnWifiOnly"

    override val default = false
    override val key = booleanPreferencesKey(LOAD_NET_IMAGE_ON_WIFI_ONLY)
}
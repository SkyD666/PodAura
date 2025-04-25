package com.skyd.anivu.model.preference.rss

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object RssSyncWifiConstraintPreference : BasePreference<Boolean>() {
    private const val RSS_SYNC_WIFI_CONSTRAINT = "rssSyncWifiConstraint"

    override val default = false
    override val key = booleanPreferencesKey(RSS_SYNC_WIFI_CONSTRAINT)
}

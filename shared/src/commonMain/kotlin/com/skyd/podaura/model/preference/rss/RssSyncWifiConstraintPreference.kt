package com.skyd.podaura.model.preference.rss

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object RssSyncWifiConstraintPreference : BasePreference<Boolean>() {
    private const val RSS_SYNC_WIFI_CONSTRAINT = "rssSyncWifiConstraint"

    override val default = false
    override val key = booleanPreferencesKey(RSS_SYNC_WIFI_CONSTRAINT)
}

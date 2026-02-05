package com.skyd.podaura.model.preference.rss

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object RssSyncChargingConstraintPreference : BasePreference<Boolean>() {
    private const val RSS_SYNC_CHARGING_CONSTRAINT = "rssSyncChargingConstraint"

    override val default = false
    override val key = booleanPreferencesKey(RSS_SYNC_CHARGING_CONSTRAINT)
}

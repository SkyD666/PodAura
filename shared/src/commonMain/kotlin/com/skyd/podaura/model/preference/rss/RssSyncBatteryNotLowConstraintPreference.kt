package com.skyd.podaura.model.preference.rss

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object RssSyncBatteryNotLowConstraintPreference : BasePreference<Boolean>() {
    private const val RSS_SYNC_BATTERY_NOT_LOW_CONSTRAINT = "rssSyncBatteryNotLowConstraint"

    override val default = true
    override val key = booleanPreferencesKey(RSS_SYNC_BATTERY_NOT_LOW_CONSTRAINT)
}

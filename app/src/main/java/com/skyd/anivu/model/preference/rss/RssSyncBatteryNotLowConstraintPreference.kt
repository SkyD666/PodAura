package com.skyd.anivu.model.preference.rss

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object RssSyncBatteryNotLowConstraintPreference : BasePreference<Boolean> {
    private const val RSS_SYNC_BATTERY_NOT_LOW_CONSTRAINT = "rssSyncBatteryNotLowConstraint"

    override val default = true
    override val key = booleanPreferencesKey(RSS_SYNC_BATTERY_NOT_LOW_CONSTRAINT)
}

package com.skyd.anivu.model.preference.transmission

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object SeedingWhenCompletePreference : BasePreference<Boolean> {
    private const val SEEDING_WHEN_COMPLETE = "seedingWhenComplete"

    override val default = true
    override val key = booleanPreferencesKey(SEEDING_WHEN_COMPLETE)
}

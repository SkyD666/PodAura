package com.skyd.podaura.model.preference.transmission

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object SeedingWhenCompletePreference : BasePreference<Boolean>() {
    private const val SEEDING_WHEN_COMPLETE = "seedingWhenComplete"

    override val default = true
    override val key = booleanPreferencesKey(SEEDING_WHEN_COMPLETE)
}

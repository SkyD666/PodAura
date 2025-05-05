package com.skyd.podaura.model.preference.appearance.search

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object SearchListTonalElevationPreference : BasePreference<Float>() {
    private const val SEARCH_LIST_TONAL_ELEVATION = "searchListTonalElevation"

    override val default = 2f
    override val key = floatPreferencesKey(SEARCH_LIST_TONAL_ELEVATION)
}
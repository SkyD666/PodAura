package com.skyd.podaura.model.preference.appearance.search

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object SearchTopBarTonalElevationPreference : BasePreference<Float>() {
    private const val SEARCH_TOP_BAR_TONAL_ELEVATION = "searchTopBarTonalElevation"

    override val default = 2f
    override val key = floatPreferencesKey(SEARCH_TOP_BAR_TONAL_ELEVATION)
}
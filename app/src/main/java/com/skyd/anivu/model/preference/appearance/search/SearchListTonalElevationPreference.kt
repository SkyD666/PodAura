package com.skyd.anivu.model.preference.appearance.search

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.anivu.base.BasePreference

object SearchListTonalElevationPreference : BasePreference<Float> {
    private const val SEARCH_LIST_TONAL_ELEVATION = "searchListTonalElevation"

    override val default = 2f
    override val key = floatPreferencesKey(SEARCH_LIST_TONAL_ELEVATION)
}
package com.skyd.anivu.model.preference.appearance.search

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object SearchItemMinWidthPreference : BasePreference<Float> {
    private const val SEARCH_ITEM_MIN_WIDTH = "searchItemMinWidth"

    override val default = 360f
    override val key = floatPreferencesKey(SEARCH_ITEM_MIN_WIDTH)
}
package com.skyd.podaura.model.preference.appearance.search

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object SearchItemMinWidthPreference : BasePreference<Float>() {
    private const val SEARCH_ITEM_MIN_WIDTH = "searchItemMinWidth"

    override val default = 360f
    override val key = floatPreferencesKey(SEARCH_ITEM_MIN_WIDTH)
}

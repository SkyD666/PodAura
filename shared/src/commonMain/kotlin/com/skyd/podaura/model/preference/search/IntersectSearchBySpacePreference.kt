package com.skyd.podaura.model.preference.search

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object IntersectSearchBySpacePreference : BasePreference<Boolean>() {
    private const val INTERSECT_SEARCH_BY_SPACE = "intersectSearchBySpace"

    override val default = true
    override val key = booleanPreferencesKey(INTERSECT_SEARCH_BY_SPACE)
}

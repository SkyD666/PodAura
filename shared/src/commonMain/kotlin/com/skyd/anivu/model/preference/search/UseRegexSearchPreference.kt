package com.skyd.anivu.model.preference.search

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object UseRegexSearchPreference : BasePreference<Boolean>() {
    private const val USE_REGEX_SEARCH = "useRegexSearch"

    override val default = false
    override val key = booleanPreferencesKey(USE_REGEX_SEARCH)
}
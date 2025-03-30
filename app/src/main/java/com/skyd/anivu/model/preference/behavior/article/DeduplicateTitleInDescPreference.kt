package com.skyd.anivu.model.preference.behavior.article

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object DeduplicateTitleInDescPreference : BasePreference<Boolean> {
    private const val DEDUPLICATE_TITLE_IN_DESC = "deduplicateTitleInDesc"

    override val default = true
    override val key = booleanPreferencesKey(DEDUPLICATE_TITLE_IN_DESC)
}
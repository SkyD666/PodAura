package com.skyd.podaura.model.preference.behavior.article

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object DeduplicateTitleInDescPreference : BasePreference<Boolean>() {
    private const val DEDUPLICATE_TITLE_IN_DESC = "deduplicateTitleInDesc"

    override val default = true
    override val key = booleanPreferencesKey(DEDUPLICATE_TITLE_IN_DESC)
}
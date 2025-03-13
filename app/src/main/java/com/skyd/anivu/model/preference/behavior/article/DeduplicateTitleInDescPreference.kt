package com.skyd.anivu.model.preference.behavior.article

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object DeduplicateTitleInDescPreference : BasePreference<Boolean> {
    private const val DEDUPLICATE_TITLE_IN_DESC = "deduplicateTitleInDesc"

    override val default = true
    override val key = booleanPreferencesKey(DEDUPLICATE_TITLE_IN_DESC)
}
package com.skyd.podaura.model.preference.appearance.article

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ArticleListTonalElevationPreference : BasePreference<Float>() {
    private const val ARTICLE_LIST_TONAL_ELEVATION = "articleListTonalElevation"

    override val default = 2f
    override val key = floatPreferencesKey(ARTICLE_LIST_TONAL_ELEVATION)
}

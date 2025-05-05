package com.skyd.podaura.model.preference.appearance.article

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ArticleTopBarTonalElevationPreference : BasePreference<Float>() {
    private const val ARTICLE_TOP_BAR_TONAL_ELEVATION = "articleTopBarTonalElevation"

    override val default = 2f
    override val key = floatPreferencesKey(ARTICLE_TOP_BAR_TONAL_ELEVATION)
}
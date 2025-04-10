package com.skyd.anivu.model.preference.appearance.article

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object ArticleTopBarTonalElevationPreference : BasePreference<Float> {
    private const val ARTICLE_TOP_BAR_TONAL_ELEVATION = "articleTopBarTonalElevation"

    override val default = 2f
    override val key = floatPreferencesKey(ARTICLE_TOP_BAR_TONAL_ELEVATION)
}
package com.skyd.anivu.model.preference.appearance.article

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.anivu.base.BasePreference

object ArticleItemTonalElevationPreference : BasePreference<Float> {
    private const val ARTICLE_ITEM_TONAL_ELEVATION = "articleItemTonalElevation"

    override val default = -2f
    override val key = floatPreferencesKey(ARTICLE_ITEM_TONAL_ELEVATION)
}
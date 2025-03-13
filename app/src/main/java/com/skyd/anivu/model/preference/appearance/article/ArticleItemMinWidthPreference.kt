package com.skyd.anivu.model.preference.appearance.article

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.anivu.base.BasePreference

object ArticleItemMinWidthPreference : BasePreference<Float> {
    private const val ARTICLE_ITEM_MIN_WIDTH = "articleItemMinWidth"

    override val default = 360f
    override val key = floatPreferencesKey(ARTICLE_ITEM_MIN_WIDTH)
}
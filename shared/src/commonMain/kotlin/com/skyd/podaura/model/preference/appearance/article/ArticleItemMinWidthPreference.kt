package com.skyd.podaura.model.preference.appearance.article

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ArticleItemMinWidthPreference : BasePreference<Float>() {
    private const val ARTICLE_ITEM_MIN_WIDTH = "articleItemMinWidth"

    override val default = 360f
    override val key = floatPreferencesKey(ARTICLE_ITEM_MIN_WIDTH)
}
package com.skyd.podaura.model.preference.appearance.article

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ArticleItemTonalElevationPreference : BasePreference<Float>() {
    private const val ARTICLE_ITEM_TONAL_ELEVATION = "articleItemTonalElevation"

    override val default = -2f
    override val key = floatPreferencesKey(ARTICLE_ITEM_TONAL_ELEVATION)
}
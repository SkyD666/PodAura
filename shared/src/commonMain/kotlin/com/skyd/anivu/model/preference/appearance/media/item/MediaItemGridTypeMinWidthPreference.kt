package com.skyd.anivu.model.preference.appearance.media.item

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object MediaItemGridTypeMinWidthPreference : BasePreference<Float>() {
    private const val MEDIA_ITEM_GRID_TYPE_MIN_WIDTH = "mediaItemGridTypeMinWidth"

    override val default = 120f

    val range = 60f..1000f

    override val key = floatPreferencesKey(MEDIA_ITEM_GRID_TYPE_MIN_WIDTH)
}
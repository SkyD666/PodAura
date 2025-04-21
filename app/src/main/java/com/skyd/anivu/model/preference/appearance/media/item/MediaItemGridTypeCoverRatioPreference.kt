package com.skyd.anivu.model.preference.appearance.media.item

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object MediaItemGridTypeCoverRatioPreference : BasePreference<Float> {
    private const val MEDIA_ITEM_GRID_TYPE_COVER_RATIO = "mediaItemGridTypeCoverRatio"

    override val default = 1f

    val range = 0.5f..2.5f

    override val key = floatPreferencesKey(MEDIA_ITEM_GRID_TYPE_COVER_RATIO)
}
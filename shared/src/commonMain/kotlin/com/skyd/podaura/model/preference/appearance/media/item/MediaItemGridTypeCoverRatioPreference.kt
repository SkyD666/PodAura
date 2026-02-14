package com.skyd.podaura.model.preference.appearance.media.item

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object MediaItemGridTypeCoverRatioPreference : BasePreference<Float>() {
    private const val MEDIA_ITEM_GRID_TYPE_COVER_RATIO = "mediaItemGridTypeCoverRatio"

    override val default = 1f

    val range = 0.5f..2.5f

    override val key = floatPreferencesKey(MEDIA_ITEM_GRID_TYPE_COVER_RATIO)
}

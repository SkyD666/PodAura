package com.skyd.podaura.model.preference.appearance.media.item

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object MediaItemGridTypeMinWidthPreference : BasePreference<Float>() {
    private const val MEDIA_ITEM_GRID_TYPE_MIN_WIDTH = "mediaItemGridTypeMinWidth"

    override val default = 120f

    val range = 60f..1000f

    override val key = floatPreferencesKey(MEDIA_ITEM_GRID_TYPE_MIN_WIDTH)
}

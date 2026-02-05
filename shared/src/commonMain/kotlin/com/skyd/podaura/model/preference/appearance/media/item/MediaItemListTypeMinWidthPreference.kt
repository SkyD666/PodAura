package com.skyd.podaura.model.preference.appearance.media.item

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object MediaItemListTypeMinWidthPreference : BasePreference<Float>() {
    private const val MEDIA_ITEM_LIST_TYPE_MIN_WIDTH = "mediaItemListTypeMinWidth"

    override val default = 360f

    val range = 200f..1000f

    override val key = floatPreferencesKey(MEDIA_ITEM_LIST_TYPE_MIN_WIDTH)
}

package com.skyd.podaura.model.preference.appearance.feed

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.fundation.ext.format
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object FeedListTonalElevationPreference : BasePreference<Float>() {
    private const val FEED_LIST_TONAL_ELEVATION = "feedListTonalElevation"

    override val default = 0f
    override val key = floatPreferencesKey(FEED_LIST_TONAL_ELEVATION)
}

object TonalElevationPreferenceUtil {
    fun toDisplay(value: Float): String {
        return value.format(2) + "dp"
    }
}

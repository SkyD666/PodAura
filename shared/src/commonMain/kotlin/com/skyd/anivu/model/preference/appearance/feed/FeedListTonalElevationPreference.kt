package com.skyd.anivu.model.preference.appearance.feed

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object FeedListTonalElevationPreference : BasePreference<Float>() {
    private const val FEED_LIST_TONAL_ELEVATION = "feedListTonalElevation"

    override val default = 0f
    override val key = floatPreferencesKey(FEED_LIST_TONAL_ELEVATION)
}

object TonalElevationPreferenceUtil {
    fun toDisplay(value: Float): String {
        return "%.2fdp".format(value)
    }
}
package com.skyd.podaura.model.preference.appearance.feed

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object FeedTopBarTonalElevationPreference : BasePreference<Float>() {
    private const val FEED_TOP_BAR_TONAL_ELEVATION = "feedTopBarTonalElevation"

    override val default = 0f
    override val key = floatPreferencesKey(FEED_TOP_BAR_TONAL_ELEVATION)
}

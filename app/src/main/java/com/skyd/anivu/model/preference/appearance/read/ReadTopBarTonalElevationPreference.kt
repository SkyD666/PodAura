package com.skyd.anivu.model.preference.appearance.read

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.anivu.base.BasePreference

object ReadTopBarTonalElevationPreference : BasePreference<Float> {
    private const val READ_TOP_BAR_TONAL_ELEVATION = "readTopBarTonalElevation"

    override val default = 0f
    override val key = floatPreferencesKey(READ_TOP_BAR_TONAL_ELEVATION)
}
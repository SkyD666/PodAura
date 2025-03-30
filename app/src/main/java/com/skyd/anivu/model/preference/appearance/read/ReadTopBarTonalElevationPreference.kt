package com.skyd.anivu.model.preference.appearance.read

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object ReadTopBarTonalElevationPreference : BasePreference<Float> {
    private const val READ_TOP_BAR_TONAL_ELEVATION = "readTopBarTonalElevation"

    override val default = 0f
    override val key = floatPreferencesKey(READ_TOP_BAR_TONAL_ELEVATION)
}
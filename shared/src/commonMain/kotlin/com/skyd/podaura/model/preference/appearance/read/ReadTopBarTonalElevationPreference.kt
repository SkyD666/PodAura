package com.skyd.podaura.model.preference.appearance.read

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ReadTopBarTonalElevationPreference : BasePreference<Float>() {
    private const val READ_TOP_BAR_TONAL_ELEVATION = "readTopBarTonalElevation"

    override val default = 0f
    override val key = floatPreferencesKey(READ_TOP_BAR_TONAL_ELEVATION)
}
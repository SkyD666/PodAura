package com.skyd.podaura.model.preference.appearance.read

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ReadContentTonalElevationPreference : BasePreference<Float>() {
    private const val READ_CONTENT_TONAL_ELEVATION = "readContentTonalElevation"

    override val default = 0f
    override val key = floatPreferencesKey(READ_CONTENT_TONAL_ELEVATION)
}
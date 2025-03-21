package com.skyd.anivu.model.preference.appearance.read

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.anivu.base.BasePreference

object ReadContentTonalElevationPreference : BasePreference<Float> {
    private const val READ_CONTENT_TONAL_ELEVATION = "readContentTonalElevation"

    override val default = 0f
    override val key = floatPreferencesKey(READ_CONTENT_TONAL_ELEVATION)
}
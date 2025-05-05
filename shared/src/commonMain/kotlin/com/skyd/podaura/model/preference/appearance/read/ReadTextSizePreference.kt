package com.skyd.podaura.model.preference.appearance.read

import androidx.datastore.preferences.core.floatPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object ReadTextSizePreference : BasePreference<Float>() {
    private const val READ_TEXT_SIZE = "readTextSize"

    override val default = 16f
    override val key = floatPreferencesKey(READ_TEXT_SIZE)
}
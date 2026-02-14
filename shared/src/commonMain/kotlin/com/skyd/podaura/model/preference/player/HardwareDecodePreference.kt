package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object HardwareDecodePreference : BasePreference<Boolean>() {
    private const val HARDWARE_DECODE = "hardwareDecode"

    override val default = true
    override val key = booleanPreferencesKey(HARDWARE_DECODE)
}

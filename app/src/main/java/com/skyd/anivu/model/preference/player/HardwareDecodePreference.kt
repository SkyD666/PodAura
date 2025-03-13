package com.skyd.anivu.model.preference.player

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference

object HardwareDecodePreference : BasePreference<Boolean> {
    private const val HARDWARE_DECODE = "hardwareDecode"

    override val default = true
    override val key = booleanPreferencesKey(HARDWARE_DECODE)
}
package com.skyd.anivu.model.preference.player

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.config.Const
import com.skyd.anivu.config.MPV_CONFIG_DIR
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object MpvConfigDirPreference : BasePreference<String>() {
    private const val MPV_CONFIG_DIR = "mpvConfigDir"

    override val default: String = Const.MPV_CONFIG_DIR
    override val key = stringPreferencesKey(MPV_CONFIG_DIR)
}
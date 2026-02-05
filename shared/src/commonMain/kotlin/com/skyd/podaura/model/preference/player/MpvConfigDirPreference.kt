package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.MPV_CONFIG_DIR
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object MpvConfigDirPreference : BasePreference<String>() {
    private const val MPV_CONFIG_DIR = "mpvConfigDir"

    override val default: String = Const.MPV_CONFIG_DIR
    override val key = stringPreferencesKey(MPV_CONFIG_DIR)
}

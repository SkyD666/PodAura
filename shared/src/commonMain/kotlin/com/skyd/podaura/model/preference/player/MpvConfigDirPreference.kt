package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.Preferences
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.MPV_CONFIG_DIR
import com.skyd.podaura.model.preference.BasePreference

abstract class BaseMpvConfigDirPreference : BasePreference<String>() {

    companion object {
        protected const val MPV_CONFIG_DIR = "mpvConfigDir"
    }

    override val default: String = Const.MPV_CONFIG_DIR
}

expect object MpvConfigDirPreference : BaseMpvConfigDirPreference {
    override val key: Preferences.Key<String>?
}

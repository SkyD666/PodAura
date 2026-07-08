package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.Preferences
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.MPV_CACHE_DIR
import com.skyd.podaura.model.preference.BasePreference

abstract class BaseMpvCacheDirPreference : BasePreference<String>() {

    companion object {
        protected const val MPV_CACHE_DIR = "mpvCacheDir"
    }

    override val default: String = Const.MPV_CACHE_DIR
}

expect object MpvCacheDirPreference : BaseMpvCacheDirPreference {
    override val key: Preferences.Key<String>?
}

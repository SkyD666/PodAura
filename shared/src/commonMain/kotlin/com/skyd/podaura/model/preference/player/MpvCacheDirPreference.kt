package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.MPV_CACHE_DIR
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object MpvCacheDirPreference : BasePreference<String>() {
    private const val MPV_CACHE_DIR = "mpvCacheDir"

    override val default: String = Const.MPV_CACHE_DIR
    override val key = stringPreferencesKey(MPV_CACHE_DIR)
}

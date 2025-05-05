package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.config.Const
import com.skyd.podaura.config.MPV_CACHE_DIR
import com.skyd.podaura.model.preference.BasePreference

@Preference
object MpvCacheDirPreference : BasePreference<String>() {
    private const val MPV_CACHE_DIR = "mpvCacheDir"

    override val default: String = Const.MPV_CACHE_DIR
    override val key = stringPreferencesKey(MPV_CACHE_DIR)
}
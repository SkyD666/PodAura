package com.skyd.anivu.model.preference.player

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.config.Const
import com.skyd.ksp.preference.Preference

@Preference
object MpvCacheDirPreference : BasePreference<String> {
    private const val MPV_CACHE_DIR = "mpvCacheDir"

    override val default: String = Const.MPV_CACHE_DIR.path
    override val key = stringPreferencesKey(MPV_CACHE_DIR)
}
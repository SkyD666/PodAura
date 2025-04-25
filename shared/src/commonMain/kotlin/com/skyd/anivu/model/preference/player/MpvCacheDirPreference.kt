package com.skyd.anivu.model.preference.player

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.config.Const
import com.skyd.anivu.config.MPV_CACHE_DIR
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object MpvCacheDirPreference : BasePreference<String>() {
    private const val MPV_CACHE_DIR = "mpvCacheDir"

    override val default: String = Const.MPV_CACHE_DIR
    override val key = stringPreferencesKey(MPV_CACHE_DIR)
}
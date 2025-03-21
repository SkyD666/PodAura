package com.skyd.anivu.model.preference.player

import androidx.datastore.preferences.core.longPreferencesKey
import com.skyd.anivu.base.BasePreference

object PlayerMaxCacheSizePreference : BasePreference<Long> {
    private const val PLAYER_MAX_CACHE_SIZE = "playerMaxCacheSize"

    override val default = 10L * 1024 * 1024    // 10 MB
    override val key = longPreferencesKey(PLAYER_MAX_CACHE_SIZE)
}
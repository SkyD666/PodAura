package com.skyd.anivu.model.preference.player

import androidx.datastore.preferences.core.longPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object PlayerMaxBackCacheSizePreference : BasePreference<Long> {
    private const val PLAYER_MAX_CACHE_SIZE = "playerMaxBackCacheSize"

    override val default = 20L * 1024 * 1024    // 20 MB
    override val key = longPreferencesKey(PLAYER_MAX_CACHE_SIZE)
}
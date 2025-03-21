package com.skyd.anivu.model.preference

import androidx.datastore.preferences.core.longPreferencesKey
import com.skyd.anivu.base.BasePreference

object IgnoreUpdateVersionPreference : BasePreference<Long> {
    private const val IGNORE_UPDATE_VERSION = "ignoreUpdateVersion"

    override val default = 0L
    override val key = longPreferencesKey(IGNORE_UPDATE_VERSION)
}
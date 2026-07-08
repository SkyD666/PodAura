package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.annotation.Preference

@Preference
actual object MpvCacheDirPreference : BaseMpvCacheDirPreference() {
    actual override val key: Preferences.Key<String>? = stringPreferencesKey(MPV_CACHE_DIR)
}

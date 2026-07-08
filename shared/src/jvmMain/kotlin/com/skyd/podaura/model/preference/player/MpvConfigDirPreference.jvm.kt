package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.annotation.Preference

@Preference
actual object MpvConfigDirPreference : BaseMpvConfigDirPreference() {
    actual override val key: Preferences.Key<String>? = stringPreferencesKey(MPV_CONFIG_DIR)
}

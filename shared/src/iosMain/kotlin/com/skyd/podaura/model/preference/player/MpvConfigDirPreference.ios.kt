package com.skyd.podaura.model.preference.player

import androidx.datastore.preferences.core.Preferences
import com.skyd.ksp.annotation.Preference

@Preference
actual object MpvConfigDirPreference : BaseMpvConfigDirPreference() {
    actual override val key: Preferences.Key<String>? = null
}

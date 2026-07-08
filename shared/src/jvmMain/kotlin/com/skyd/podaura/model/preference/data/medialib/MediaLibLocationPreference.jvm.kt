package com.skyd.podaura.model.preference.data.medialib

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.annotation.Preference

@Preference
actual object MediaLibLocationPreference : BaseMediaLibLocationPreference() {
    actual override val key: Preferences.Key<String>? = stringPreferencesKey(MEDIA_LIB_LOCATION)
}

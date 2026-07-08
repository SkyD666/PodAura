package com.skyd.podaura.model.preference.data.medialib

import androidx.datastore.preferences.core.Preferences
import com.skyd.ksp.annotation.Preference

@Preference
actual object MediaLibLocationPreference : BaseMediaLibLocationPreference() {
    actual override val key: Preferences.Key<String>? = null
}

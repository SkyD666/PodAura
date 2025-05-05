package com.skyd.podaura.model.preference.behavior

import androidx.datastore.preferences.core.Preferences
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
actual object PickImageMethodPreference : BasePreference<String>() {
    actual val methodList = arrayOf<String>()

    actual override val default: String = TODO()
    actual override val key: Preferences.Key<String> = TODO()

    actual suspend fun toDisplayName(method: String): String = TODO()
}
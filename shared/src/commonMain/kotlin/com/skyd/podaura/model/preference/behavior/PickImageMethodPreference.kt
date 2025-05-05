package com.skyd.podaura.model.preference.behavior

import androidx.datastore.preferences.core.Preferences
import com.skyd.podaura.model.preference.BasePreference

expect object PickImageMethodPreference : BasePreference<String> {
    val methodList: Array<String>

    override val default: String
    override val key: Preferences.Key<String>

    suspend fun toDisplayName(method: String): String
}
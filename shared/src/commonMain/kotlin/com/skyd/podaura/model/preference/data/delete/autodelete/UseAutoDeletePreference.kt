package com.skyd.podaura.model.preference.data.delete.autodelete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object UseAutoDeletePreference : BasePreference<Boolean>() {
    private const val USE_AUTO_DELETE = "useAutoDelete"

    override val default = true
    override val key = booleanPreferencesKey(USE_AUTO_DELETE)
}

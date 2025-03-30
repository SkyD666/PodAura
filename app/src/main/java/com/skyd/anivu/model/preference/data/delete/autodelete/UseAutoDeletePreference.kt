package com.skyd.anivu.model.preference.data.delete.autodelete

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.anivu.base.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object UseAutoDeletePreference : BasePreference<Boolean> {
    private const val USE_AUTO_DELETE = "useAutoDelete"

    override val default = true
    override val key = booleanPreferencesKey(USE_AUTO_DELETE)
}

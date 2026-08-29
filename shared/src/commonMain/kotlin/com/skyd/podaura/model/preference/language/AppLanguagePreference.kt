package com.skyd.podaura.model.preference.language

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.ext.put
import com.skyd.podaura.model.preference.BasePreference
import com.skyd.podaura.model.preference.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Preference
object AppLanguagePreference : BasePreference<String>() {
    override val key = stringPreferencesKey("appLanguage")
    override val default: String = AppLanguage.System.preferenceValue

    fun put(scope: CoroutineScope, language: AppLanguage) {
        put(scope, language.preferenceValue)
    }

    override fun put(scope: CoroutineScope, value: String) {
        val language = AppLanguage.fromPreferenceValue(value)
        require(language.preferenceValue.equals(value, ignoreCase = true)) {
            "Unsupported app language: $value"
        }
        scope.launch(Dispatchers.IO) {
            dataStore.put(key, language.preferenceValue)
            withContext(Dispatchers.Main) {
                setPlatformAppLanguage(language)
            }
        }
    }
}

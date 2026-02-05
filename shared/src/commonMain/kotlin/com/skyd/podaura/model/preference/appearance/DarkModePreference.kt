package com.skyd.podaura.model.preference.appearance

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.skyd.podaura.ext.put
import com.skyd.podaura.model.preference.BasePreference
import com.skyd.podaura.model.preference.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.dark_mode_dark
import podaura.shared.generated.resources.dark_mode_follow_system
import podaura.shared.generated.resources.dark_mode_light
import podaura.shared.generated.resources.unknown


abstract class BaseDarkModePreference : BasePreference<Int>() {
    companion object {
        private const val DARK_MODE = "darkMode"

        const val MODE_NIGHT_FOLLOW_SYSTEM = 0
        const val MODE_NIGHT_NO = 1
        const val MODE_NIGHT_YES = 2

        suspend fun toDisplayName(value: Int): String = getString(
            when (value) {
                MODE_NIGHT_NO -> Res.string.dark_mode_light
                MODE_NIGHT_YES -> Res.string.dark_mode_dark
                MODE_NIGHT_FOLLOW_SYSTEM -> Res.string.dark_mode_follow_system
                else -> Res.string.unknown
            }
        )
    }

    override val key = intPreferencesKey(DARK_MODE)

    override fun put(scope: CoroutineScope, value: Int) {
        if (value != MODE_NIGHT_YES &&
            value != MODE_NIGHT_NO &&
            value != MODE_NIGHT_FOLLOW_SYSTEM
        ) {
            throw IllegalArgumentException("darkMode value invalid!!!")
        }
        scope.launch(Dispatchers.IO) {
            dataStore.put(key, value)
            withContext(Dispatchers.Main) {
                onChangeNightMode(value)
            }
        }
    }

    override fun fromPreferences(preferences: Preferences): Int {
        val scope = CoroutineScope(context = Dispatchers.Main)
        val value = preferences[key] ?: DarkModePreference.default
        scope.launch(Dispatchers.Main) {
            onChangeNightMode(value)
        }
        return value
    }

    abstract fun onChangeNightMode(mode: Int)

    @Composable
    @ReadOnlyComposable
    fun inDark(value: Int) = when (value) {
        MODE_NIGHT_YES -> true
        MODE_NIGHT_NO -> false
        else -> isSystemInDarkTheme()
    }
}

expect object DarkModePreference : BaseDarkModePreference {
    val values: List<Int>
    override val default: Int
    override fun onChangeNightMode(mode: Int)
}
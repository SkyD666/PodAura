package com.skyd.anivu.model.preference.appearance

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.skyd.anivu.R
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.put
import com.skyd.ksp.preference.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Preference
object DarkModePreference : BasePreference<Int> {
    private const val DARK_MODE = "darkMode"

    val values: List<Int> = buildList {
        add(AppCompatDelegate.MODE_NIGHT_NO)
        add(AppCompatDelegate.MODE_NIGHT_YES)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    override val default = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    } else {
        AppCompatDelegate.MODE_NIGHT_NO
    }

    override val key = intPreferencesKey(DARK_MODE)

    fun toDisplayName(context: Context, value: Int): String = context.getString(
        when (value) {
            AppCompatDelegate.MODE_NIGHT_NO -> R.string.dark_mode_light
            AppCompatDelegate.MODE_NIGHT_YES -> R.string.dark_mode_dark
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> R.string.dark_mode_follow_system
            else -> R.string.unknown
        }
    )

    override fun put(context: Context, scope: CoroutineScope, value: Int) {
        if (value != AppCompatDelegate.MODE_NIGHT_YES &&
            value != AppCompatDelegate.MODE_NIGHT_NO &&
            value != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ) {
            throw IllegalArgumentException("darkMode value invalid!!!")
        }
        scope.launch(Dispatchers.IO) {
            context.dataStore.put(key, value)
            withContext(Dispatchers.Main) {
                AppCompatDelegate.setDefaultNightMode(value)
            }
        }
    }

    override fun fromPreferences(preferences: Preferences): Int {
        val scope = CoroutineScope(context = Dispatchers.Main)
        val value = preferences[key] ?: default
        scope.launch(Dispatchers.Main) {
            AppCompatDelegate.setDefaultNightMode(value)
        }
        return value
    }

    @Composable
    @ReadOnlyComposable
    fun inDark(value: Int) = when (value) {
        AppCompatDelegate.MODE_NIGHT_YES -> true
        AppCompatDelegate.MODE_NIGHT_NO -> false
        else -> isSystemInDarkTheme()
    }
}
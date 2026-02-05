package com.skyd.podaura.model.preference.appearance

import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.stringPreferencesKey
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
import podaura.shared.generated.resources.theme_blue
import podaura.shared.generated.resources.theme_dynamic
import podaura.shared.generated.resources.theme_green
import podaura.shared.generated.resources.theme_mahiro
import podaura.shared.generated.resources.theme_pink
import podaura.shared.generated.resources.theme_purple
import podaura.shared.generated.resources.theme_red
import podaura.shared.generated.resources.theme_yellow
import podaura.shared.generated.resources.unknown

abstract class BaseThemePreference : BasePreference<String>() {
    companion object {
        private const val THEME = "theme"

        const val DYNAMIC = "Dynamic"
        const val BLUE = "Blue"
        const val PINK = "Pink"
        const val YELLOW = "Yellow"
        const val RED = "Red"
        const val GREEN = "Green"
        const val PURPLE = "Purple"
        const val MAHIRO = "Mahiro"
    }

    val basicValues = arrayOf(BLUE, PINK, YELLOW, RED, GREEN, PURPLE, MAHIRO)
    override val key = stringPreferencesKey(THEME)

    suspend fun toDisplayName(value: String): String = getString(
        when (value) {
            DYNAMIC -> Res.string.theme_dynamic
            BLUE -> Res.string.theme_blue
            PINK -> Res.string.theme_pink
            YELLOW -> Res.string.theme_yellow
            RED -> Res.string.theme_red
            GREEN -> Res.string.theme_green
            PURPLE -> Res.string.theme_purple
            MAHIRO -> Res.string.theme_mahiro
            else -> Res.string.unknown
        }
    )

    fun toColors(value: String): Triple<Color, Color?, Color?> = Triple(
        toSeedColor(value),
        toSecondaryColor(value),
        toTertiaryColor(value),
    )

    fun toSeedColor(value: String): Color = when (value) {
        BLUE -> Color(0xFF006EBE)
        PINK -> Color(0xFFFF7AA3)
        YELLOW -> Color(0xFFFABE03)
        RED -> Color(0xFFB90037)
        GREEN -> Color(0xFF3F975B)
        PURPLE -> Color(0xFF7E6195)
        MAHIRO -> Color(0xFFEAD4CE)
        else -> Color(0xFF006EBE)
    }

    fun toSecondaryColor(value: String): Color? = when (value) {
        MAHIRO -> Color(0xFF7D859D)
        else -> null
    }

    fun toTertiaryColor(value: String): Color? = when (value) {
        MAHIRO -> Color(0xFFEC9CA8)
        else -> null
    }

    fun put(scope: CoroutineScope, value: String, onSuccess: () -> Unit) {
        scope.launch(Dispatchers.IO) {
            dataStore.put(key, value)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }
}

expect object ThemePreference : BaseThemePreference {
    override val default: String
}

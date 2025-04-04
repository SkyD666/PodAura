package com.skyd.anivu.model.preference.appearance

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.material.color.DynamicColors
import com.skyd.anivu.R
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.ext.put
import com.skyd.ksp.preference.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Preference
object ThemePreference : BasePreference<String> {
    private const val THEME = "theme"

    const val DYNAMIC = "Dynamic"
    const val BLUE = "Blue"
    const val PINK = "Pink"
    const val YELLOW = "Yellow"
    const val RED = "Red"
    const val GREEN = "Green"
    const val PURPLE = "Purple"
    const val MAHIRO = "Mahiro"

    val basicValues = arrayOf(BLUE, PINK, YELLOW, RED, GREEN, PURPLE, MAHIRO)

    override val default = if (DynamicColors.isDynamicColorAvailable()) DYNAMIC else PINK

    override val key = stringPreferencesKey(THEME)

    fun put(
        context: Context,
        scope: CoroutineScope,
        value: String,
        onSuccess: (() -> Unit)? = null,
    ) {
        scope.launch(Dispatchers.IO) {
            context.dataStore.put(key, value)
            withContext(Dispatchers.Main) {
                onSuccess?.invoke()
            }
        }
    }

    fun toDisplayName(
        context: Context,
        value: String = context.dataStore.getOrDefault(this),
    ): String = when (value) {
        DYNAMIC -> context.getString(R.string.theme_dynamic)
        BLUE -> context.getString(R.string.theme_blue)
        PINK -> context.getString(R.string.theme_pink)
        YELLOW -> context.getString(R.string.theme_yellow)
        RED -> context.getString(R.string.theme_red)
        GREEN -> context.getString(R.string.theme_green)
        PURPLE -> context.getString(R.string.theme_purple)
        MAHIRO -> context.getString(R.string.theme_mahiro)
        else -> context.getString(R.string.unknown)
    }

    fun toColors(
        context: Context,
        value: String = context.dataStore.getOrDefault(this),
    ): Triple<Color, Color?, Color?> = Triple(
        toSeedColor(context, value),
        toSecondaryColor(context, value),
        toTertiaryColor(context, value),
    )

    fun toSeedColor(
        context: Context,
        value: String = context.dataStore.getOrDefault(this),
    ): Color = when (value) {
        BLUE -> Color(0xFF006EBE)
        PINK -> Color(0xFFFF7AA3)
        YELLOW -> Color(0xFFFABE03)
        RED -> Color(0xFFB90037)
        GREEN -> Color(0xFF3F975B)
        PURPLE -> Color(0xFF7E6195)
        MAHIRO -> Color(0xFFEAD4CE)
        else -> Color(0xFF006EBE)
    }

    fun toSecondaryColor(
        context: Context,
        value: String = context.dataStore.getOrDefault(this),
    ): Color? = when (value) {
        MAHIRO -> Color(0xFF7D859D)
        else -> null
    }

    fun toTertiaryColor(
        context: Context,
        value: String = context.dataStore.getOrDefault(this),
    ): Color? = when (value) {
        MAHIRO -> Color(0xFFEC9CA8)
        else -> null
    }
}

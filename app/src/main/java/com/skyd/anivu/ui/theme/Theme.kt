package com.skyd.anivu.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.Contrast
import com.materialkolor.dynamicColorScheme
import com.materialkolor.rememberDynamicColorScheme
import com.skyd.anivu.model.preference.appearance.DarkModePreference
import com.skyd.anivu.model.preference.appearance.ThemePreference
import com.skyd.generated.preference.LocalAmoledDarkMode
import com.skyd.generated.preference.LocalTheme

@Composable
fun PodAuraTheme(
    darkTheme: Int,
    content: @Composable () -> Unit
) {
    PodAuraTheme(
        darkTheme = DarkModePreference.inDark(darkTheme),
        content = content
    )
}

@Composable
fun PodAuraTheme(
    darkTheme: Boolean,
    colors: Map<String, ColorScheme> = extractAllColors(darkTheme),
    content: @Composable () -> Unit
) {
    val themeName = LocalTheme.current
    val context = LocalContext.current
    val isAmoled = LocalAmoledDarkMode.current

    MaterialTheme(
        colorScheme = remember(themeName, darkTheme, isAmoled) {
            colors.getOrElse(themeName) {
                val (primary, secondary, tertiary) =
                    ThemePreference.toColors(context, ThemePreference.basicValues[0])
                dynamicColorScheme(
                    seedColor = primary,
                    isDark = darkTheme,
                    isAmoled = isAmoled,
                    secondary = secondary,
                    tertiary = tertiary,
                    contrastLevel = context.contrastLevel,
                )
            }
        },
        typography = Typography,
        content = content
    )
}

private val Context.contrastLevel: Double
    get() {
        var contrastLevel: Double = Contrast.Default.value
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            contrastLevel = uiModeManager.contrast.toDouble()
        }
        return contrastLevel
    }

@Composable
fun extractAllColors(darkTheme: Boolean): Map<String, ColorScheme> {
    return extractDynamicColor(darkTheme) + extractColors(darkTheme)
}

@Composable
fun extractColors(darkTheme: Boolean): Map<String, ColorScheme> {
    val context = LocalContext.current
    return ThemePreference.basicValues.associateWith {
        val (primary, secondary, tertiary) = ThemePreference.toColors(LocalContext.current, it)
        rememberDynamicColorScheme(
            primary = primary,
            isDark = darkTheme,
            isAmoled = LocalAmoledDarkMode.current,
            secondary = secondary,
            tertiary = tertiary,
            contrastLevel = context.contrastLevel,
        )
    }.toMutableMap()
}

@Composable
fun extractDynamicColor(darkTheme: Boolean): Map<String, ColorScheme> = buildMap {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        put(
            ThemePreference.DYNAMIC,
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        )
    }
}
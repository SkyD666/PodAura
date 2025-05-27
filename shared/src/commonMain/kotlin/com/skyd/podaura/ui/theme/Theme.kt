package com.skyd.podaura.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.materialkolor.dynamicColorScheme
import com.materialkolor.rememberDynamicColorScheme
import com.skyd.podaura.model.preference.appearance.AmoledDarkModePreference
import com.skyd.podaura.model.preference.appearance.DarkModePreference
import com.skyd.podaura.model.preference.appearance.ThemePreference

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
    val themeName = ThemePreference.current
    val isAmoled = AmoledDarkModePreference.current

    MaterialExpressiveTheme(
        colorScheme = remember(themeName, darkTheme, isAmoled) {
            colors.getOrElse(themeName) {
                val (primary, secondary, tertiary) = ThemePreference.toColors(ThemePreference.basicValues[0])
                dynamicColorScheme(
                    seedColor = primary,
                    isDark = darkTheme,
                    isAmoled = isAmoled,
                    secondary = secondary,
                    tertiary = tertiary,
                    contrastLevel = contrastLevel(),
                )
            }
        },
        typography = Typography,
        content = content
    )
}

expect fun contrastLevel(): Double

@Composable
fun extractAllColors(darkTheme: Boolean): Map<String, ColorScheme> {
    return extractDynamicColor(darkTheme) + extractColors(darkTheme)
}

@Composable
fun extractColors(darkTheme: Boolean): Map<String, ColorScheme> {
    return ThemePreference.basicValues.associateWith {
        val (primary, secondary, tertiary) = ThemePreference.toColors(it)
        rememberDynamicColorScheme(
            primary = primary,
            isDark = darkTheme,
            isAmoled = AmoledDarkModePreference.current,
            secondary = secondary,
            tertiary = tertiary,
            contrastLevel = contrastLevel(),
        )
    }.toMutableMap()
}

@Composable
expect fun extractDynamicColor(darkTheme: Boolean): Map<String, ColorScheme>
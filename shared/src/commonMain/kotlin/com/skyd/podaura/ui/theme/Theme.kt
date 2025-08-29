package com.skyd.podaura.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.skyd.podaura.model.preference.appearance.AmoledDarkModePreference
import com.skyd.podaura.model.preference.appearance.DarkModePreference
import com.skyd.podaura.model.preference.appearance.ExpressiveColorPreference
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
    val expressiveColor = ExpressiveColorPreference.current

    MaterialExpressiveTheme(
        colorScheme = remember(themeName, darkTheme, isAmoled, expressiveColor) {
            colors.getOrElse(themeName) {
                val (primary, secondary, tertiary) = ThemePreference.toColors(ThemePreference.basicValues[0])
                dynamicColorScheme(
                    seedColor = primary,
                    isDark = darkTheme,
                    isAmoled = isAmoled,
                    secondary = secondary,
                    tertiary = tertiary,
                    style = if (expressiveColor) PaletteStyle.Expressive else PaletteStyle.TonalSpot,
                    contrastLevel = contrastLevel(),
                    specVersion = if (expressiveColor) {
                        ColorSpec.SpecVersion.SPEC_2025
                    } else {
                        ColorSpec.SpecVersion.SPEC_2021
                    },
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
    val expressiveColor = ExpressiveColorPreference.current
    return ThemePreference.basicValues.associateWith {
        val (primary, secondary, tertiary) = ThemePreference.toColors(it)
        rememberDynamicColorScheme(
            primary = primary,
            isDark = darkTheme,
            isAmoled = AmoledDarkModePreference.current,
            secondary = secondary,
            tertiary = tertiary,
            style = if (expressiveColor) PaletteStyle.Expressive else PaletteStyle.TonalSpot,
            contrastLevel = contrastLevel(),
            specVersion = if (expressiveColor) {
                ColorSpec.SpecVersion.SPEC_2025
            } else {
                ColorSpec.SpecVersion.SPEC_2021
            },
        )
    }.toMutableMap()
}

@Composable
expect fun extractDynamicColor(darkTheme: Boolean): Map<String, ColorScheme>
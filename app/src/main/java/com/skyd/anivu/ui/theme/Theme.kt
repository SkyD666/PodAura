package com.skyd.anivu.ui.theme

import android.app.WallpaperManager
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.materialkolor.dynamicColorScheme
import com.materialkolor.rememberDynamicColorScheme
import com.skyd.anivu.model.preference.appearance.DarkModePreference
import com.skyd.anivu.model.preference.appearance.ThemePreference
import com.skyd.anivu.ui.local.LocalAmoledDarkMode
import com.skyd.anivu.ui.local.LocalTheme

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
                dynamicColorScheme(
                    seedColor = ThemePreference.toSeedColor(
                        context = context,
                        value = ThemePreference.values[0],
                    ),
                    isDark = darkTheme,
                    isAmoled = isAmoled,
                )
            }
        },
        typography = Typography,
        content = content
    )
}

@Composable
fun extractAllColors(darkTheme: Boolean): Map<String, ColorScheme> {
    return extractColors(darkTheme) + extractDynamicColor(darkTheme)
}

@Composable
fun extractColors(darkTheme: Boolean): Map<String, ColorScheme> {
    return ThemePreference.values.associateWith {
        rememberDynamicColorScheme(
            primary = ThemePreference.toSeedColor(LocalContext.current, it),
            isDark = darkTheme,
            isAmoled = LocalAmoledDarkMode.current,
        )
    }.toMutableMap()
}

@Composable
fun extractDynamicColor(darkTheme: Boolean): Map<String, ColorScheme> {
    val context = LocalContext.current
    val preset = mutableMapOf<String, ColorScheme>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && !LocalView.current.isInEditMode) {
        val colors = WallpaperManager.getInstance(context)
            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        val primary = colors?.primaryColor?.toArgb()
        if (primary != null) {
            preset[ThemePreference.DYNAMIC] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                rememberSystemDynamicColorScheme(isDark = darkTheme)
            } else {
                rememberDynamicColorScheme(
                    primary = Color(primary),
                    isDark = darkTheme,
                    isAmoled = LocalAmoledDarkMode.current,
                )
            }
        }
    }
    return preset
}
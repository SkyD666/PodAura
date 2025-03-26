package com.skyd.anivu.ui.theme

import android.app.UiModeManager
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.materialkolor.Contrast
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
fun extractDynamicColor(darkTheme: Boolean): Map<String, ColorScheme> {
    val preset = mutableMapOf<String, ColorScheme>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && !LocalView.current.isInEditMode) {
        val context = LocalContext.current
        val colors = WallpaperManager.getInstance(context)
            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        val primary = colors?.primaryColor?.toArgb()
        if (primary != null) {
            preset[ThemePreference.DYNAMIC] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                rememberDynamicColorScheme(
                    primary = Color(primary),
                    isDark = darkTheme,
                    isAmoled = LocalAmoledDarkMode.current,
                    contrastLevel = context.contrastLevel,
                )
            }
        }
    }
    return preset
}
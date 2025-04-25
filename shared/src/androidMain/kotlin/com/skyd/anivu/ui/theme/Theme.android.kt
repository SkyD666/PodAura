package com.skyd.anivu.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.Contrast
import com.skyd.anivu.di.get
import com.skyd.anivu.model.preference.appearance.BaseThemePreference


actual fun contrastLevel(): Double {
    var contrastLevel: Double = Contrast.Default.value
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val uiModeManager =
            get<Context>().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        contrastLevel = uiModeManager.contrast.toDouble()
    }
    return contrastLevel
}

@Composable
actual fun extractDynamicColor(darkTheme: Boolean): Map<String, ColorScheme> = buildMap {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        put(
            BaseThemePreference.DYNAMIC,
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        )
    }
}
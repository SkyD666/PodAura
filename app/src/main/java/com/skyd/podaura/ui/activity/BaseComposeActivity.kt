package com.skyd.podaura.ui.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.skyd.podaura.ext.flowOf
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.AcceptTermsPreference
import com.skyd.podaura.model.preference.appearance.DarkModePreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.ui.component.SettingsProvider
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.theme.PodAuraTheme

open class BaseComposeActivity : AppCompatActivity() {
    private var acceptTerms = false
    private var darkMode = DarkModePreference.default

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // A known bug: https://issuetracker.google.com/issues/387281251
        enableEdgeToEdge()
        acceptTerms = dataStore.getOrDefault(AcceptTermsPreference)
        darkMode = dataStore.getOrDefault(DarkModePreference)
    }

    fun setContentBase(content: @Composable () -> Unit) = setContent {
        CompositionLocalProvider(
            LocalWindowSizeClass provides calculateWindowSizeClass(this@BaseComposeActivity),
            AcceptTermsPreference.local provides dataStore.flowOf(AcceptTermsPreference)
                .collectAsState(initial = acceptTerms).value,
        ) {
            SettingsProvider(dataStore) {
                CompositionLocalProvider(
                    DarkModePreference.local provides dataStore.flowOf(DarkModePreference)
                        .collectAsState(initial = darkMode).value,
                ) {
                    PodAuraTheme(darkTheme = DarkModePreference.current, content)
                }
            }
        }
    }
}
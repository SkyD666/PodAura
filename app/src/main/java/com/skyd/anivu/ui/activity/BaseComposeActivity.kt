package com.skyd.anivu.ui.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.skyd.anivu.model.preference.appearance.DarkModePreference
import com.skyd.anivu.model.preference.dataStore
import com.skyd.anivu.ui.component.SettingsProvider
import com.skyd.anivu.ui.local.LocalWindowSizeClass
import com.skyd.anivu.ui.theme.PodAuraTheme

open class BaseComposeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A known bug: https://issuetracker.google.com/issues/387281251
        enableEdgeToEdge()
    }

    fun setContentBase(content: @Composable () -> Unit) = setContent {
        CompositionLocalProvider(
            LocalWindowSizeClass provides calculateWindowSizeClass(this@BaseComposeActivity)
        ) {
            SettingsProvider(dataStore) {
                PodAuraTheme(darkTheme = DarkModePreference.current, content)
            }
        }
    }
}
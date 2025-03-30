package com.skyd.anivu.base

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ui.local.LocalWindowSizeClass
import com.skyd.anivu.ui.theme.PodAuraTheme
import com.skyd.generated.preference.LocalDarkMode
import com.skyd.generated.preference.SettingsProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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
            val context = LocalContext.current
            val dataStore = remember { context.dataStore }
            SettingsProvider(dataStore) {
                PodAuraTheme(darkTheme = LocalDarkMode.current, content)
            }
        }
    }
}
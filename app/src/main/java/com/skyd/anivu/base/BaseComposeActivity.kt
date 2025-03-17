package com.skyd.anivu.base

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.skyd.anivu.model.preference.SettingsProvider
import com.skyd.anivu.ui.local.LocalDarkMode
import com.skyd.anivu.ui.local.LocalWindowSizeClass
import com.skyd.anivu.ui.theme.PodAuraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class BaseComposeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    fun setContentBase(content: @Composable () -> Unit) = setContent {
        CompositionLocalProvider(
            LocalWindowSizeClass provides calculateWindowSizeClass(this@BaseComposeActivity)
        ) {
            SettingsProvider { PodAuraTheme(darkTheme = LocalDarkMode.current, content) }
        }
    }
}
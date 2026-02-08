package com.skyd.podaura.ui.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.skyd.podaura.ui.screen.SettingsProvider

open class BaseComposeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // A known bug: https://issuetracker.google.com/issues/387281251
        enableEdgeToEdge()
    }

    fun setContentBase(content: @Composable () -> Unit) = setContent {
        SettingsProvider(content = content)
    }
}
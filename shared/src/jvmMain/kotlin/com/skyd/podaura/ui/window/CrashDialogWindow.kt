package com.skyd.podaura.ui.window

import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.skyd.fundation.config.Const
import com.skyd.podaura.BuildKonfig
import com.skyd.podaura.ext.safeOpenUri
import com.skyd.podaura.model.preference.appearance.DarkModePreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.ui.component.SettingsProvider
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.screen.settings.CrashScreen
import com.skyd.podaura.ui.theme.PodAuraTheme

@Composable
fun CrashWindow(onCloseRequest: () -> Unit, crashInfo: String) {
    var message by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(crashInfo) {
        message = buildString {
            append("Version: ").append(BuildKonfig.versionForDesktop).append("\n")
            append("OS: ").append(System.getProperty("os.name")).append("\n")
            append("OS Version: ").append(System.getProperty("os.version")).append("\n")
            append("OS Architecture: ").append(System.getProperty("os.arch")).append("\n")
            append("Crash Info: \n")
            append(crashInfo)
        }
    }
    Window(
        onCloseRequest = onCloseRequest,
        state = rememberWindowState(position = WindowPosition.Aligned(alignment = Alignment.Center)),
    ) {
        CompositionLocalProvider(LocalWindowSizeClass provides calculateWindowSizeClass()) {
            val dataStore = remember { dataStore }
            SettingsProvider(dataStore) {
                PodAuraTheme(darkTheme = DarkModePreference.current) {
                    val uriHandler = LocalUriHandler.current
                    CrashScreen(
                        message = message,
                        onReport = { uriHandler.safeOpenUri(Const.GITHUB_NEW_ISSUE_URL) },
                    )
                }
            }
        }
    }
}
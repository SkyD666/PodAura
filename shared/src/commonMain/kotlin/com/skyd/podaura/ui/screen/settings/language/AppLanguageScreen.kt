package com.skyd.podaura.ui.screen.settings.language

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.podaura.model.preference.language.AppLanguage
import com.skyd.podaura.model.preference.language.AppLanguagePreference
import com.skyd.settings.BaseSettingsItem
import com.skyd.settings.SettingsLazyColumn
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.app_language_follow_system
import podaura.shared.generated.resources.app_language_screen_name

@Serializable
data object AppLanguageRoute : NavKey

@Composable
fun AppLanguageScreen(
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val selectedLanguage = AppLanguage.fromPreferenceValue(AppLanguagePreference.current)

    Scaffold(
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(Res.string.app_language_screen_name)) },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
        },
        contentWindowInsets = windowInsets,
    ) { innerPadding ->
        SettingsLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
        ) {
            group {
                AppLanguage.entries.forEach { language ->
                    item(key = language.preferenceValue) {
                        val onSelect = { AppLanguagePreference.put(scope, language) }
                        BaseSettingsItem(
                            modifier = Modifier.selectable(
                                selected = selectedLanguage == language,
                                role = Role.RadioButton,
                                onClick = onSelect,
                            ),
                            icon = null,
                            text = language.nativeDisplayName
                                ?: stringResource(Res.string.app_language_follow_system),
                            descriptionText = null,
                        ) {
                            RadioButton(
                                selected = selectedLanguage == language,
                                onClick = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

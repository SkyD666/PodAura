package com.skyd.podaura.ui.screen.settings.language

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.component.navigation.newNavBackStack
import com.skyd.compone.local.LocalWindowController
import com.skyd.compone.local.WindowController
import com.skyd.podaura.ui.component.navigation.PodAuraSerializersModule
import kotlin.test.Test

class AppLanguageScreenTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun listsEverySupportedLanguageByItsNativeName() = runComposeUiTest {
        setContent {
            val navBackStack = rememberNavBackStack(
                SavedStateConfiguration {
                    serializersModule = PodAuraSerializersModule
                },
                AppLanguageRoute,
            )
            CompositionLocalProvider(
                LocalNavBackStack provides newNavBackStack(navBackStack, parent = null),
                LocalWindowController provides WindowController(onClose = {}),
            ) {
                MaterialTheme {
                    AppLanguageScreen()
                }
            }
        }

        listOf(
            "English",
            "简体中文",
            "繁體中文",
            "日本語",
            "Türkçe",
            "Esperanto",
        ).forEach { nativeName ->
            onNodeWithText(nativeName).assertExists()
        }
    }
}

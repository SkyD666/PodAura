package com.skyd.podaura.ui.screen.settings

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.component.navigation.newNavBackStack
import com.skyd.compone.ext.isSinglePane
import com.skyd.podaura.ui.component.navigation.PodAuraSerializersModule
import com.skyd.podaura.ui.screen.settings.appearance.AppearanceRoute
import kotlinx.serialization.Serializable


@Serializable
data object SettingsRoute : NavKey

@Composable
fun SettingsScreen() {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        directive = remember(windowAdaptiveInfo) {
            calculatePaneScaffoldDirective(windowAdaptiveInfo)
                .copy(horizontalPartitionSpacerSize = 0.dp)
        }
    )
    val newBackStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = PodAuraSerializersModule
        },
        *buildList {
            add(SettingsListRoute)
            if (!listDetailStrategy.isSinglePane) {
                add(AppearanceRoute)
            }
        }.toTypedArray<NavKey>()
    )

    CompositionLocalProvider(LocalNavBackStack provides newNavBackStack(newBackStack)) {
        val navBackStack = LocalNavBackStack.current
        SettingsDetailPaneNavDisplay(
            navBackStack = navBackStack,
            sceneStrategy = listDetailStrategy,
            onPaneBack = if (listDetailStrategy.isSinglePane) {
                { navBackStack.removeLastOrNull() }
            } else null,
        )
    }
}

package com.skyd.podaura.ui.screen

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.skyd.compone.component.navigation.multiplestacks.NavigationState
import com.skyd.compone.component.navigation.multiplestacks.Navigator
import com.skyd.podaura.model.preference.behavior.DefaultHomeTabPreference
import com.skyd.podaura.ui.component.PodAuraNavDisplay
import com.skyd.podaura.ui.screen.calendar.CalendarRoute
import com.skyd.podaura.ui.screen.feed.FeedRoute
import com.skyd.podaura.ui.screen.media.MediaRoute
import com.skyd.podaura.ui.screen.more.MoreRoute
import com.skyd.podaura.ui.screen.playlist.PlaylistRoute
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MainNavigationTest {
    @Test
    fun homePreferenceSelectsEachTabAndUnknownValuesFallBackToSubscriptions() {
        val cases = mapOf(
            DefaultHomeTabPreference.FEED to FeedRoute,
            DefaultHomeTabPreference.CALENDAR to CalendarRoute,
            DefaultHomeTabPreference.MEDIA to MediaRoute,
            DefaultHomeTabPreference.MORE to MoreRoute,
            "unknown" to FeedRoute,
        )
        cases.forEach { (preference, expected) ->
            runComposeUiTest {
                lateinit var state: NavigationState
                setContent { state = rememberMainNavigationState(preference) }
                runOnIdle { assertEquals(expected, state.topLevelRoute) }
            }
        }
    }

    @Test
    fun backFromEveryTabFallsThroughButDetailPagesStillPop() = runComposeUiTest {
        lateinit var state: NavigationState
        val outerStack = mutableStateListOf<NavKey>(MainRoute)
        var systemBacks = 0
        val backInput = object : NavigationEventInput() {
            fun back() = dispatchOnBackCompleted()
        }
        setContent {
            val dispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current)
                .navigationEventDispatcher
            DisposableEffect(dispatcher) {
                dispatcher.addInput(backInput)
                onDispose { dispatcher.removeInput(backInput) }
            }
            // Represents the host's normal system Back behavior below app navigation.
            NavigationBackHandler(
                state = rememberNavigationEventState(NavigationEventInfo.None),
                isBackEnabled = true,
                onBackCompleted = { systemBacks++ },
            )
            PodAuraNavDisplay(
                backStack = outerStack,
                entryProvider = { route ->
                    NavEntry(route) {
                        if (route == MainRoute) {
                            state = rememberMainNavigationState(DefaultHomeTabPreference.CALENDAR)
                            MainTabNavDisplay(
                                navigationState = state,
                                entryProvider = { NavEntry(it) { Text(it.toString()) } },
                                onBack = { Navigator(state).goBack() },
                            )
                        } else {
                            Text("Playlist detail")
                        }
                    }
                },
            )
        }
        mainTabRoutes.forEachIndexed { index, route ->
            runOnIdle { Navigator(state).navigate(route) }
            runOnIdle { backInput.back() }
            runOnIdle {
                assertEquals(index + 1, systemBacks)
                assertEquals(route, state.topLevelRoute)
            }
        }
        runOnIdle { outerStack.add(PlaylistRoute) }
        onNodeWithText("Playlist detail").assertExists()
        runOnIdle { backInput.back() }
        runOnIdle {
            assertEquals(listOf<NavKey>(MainRoute), outerStack.toList())
            assertEquals(mainTabRoutes.size, systemBacks)
            assertEquals(MoreRoute, state.topLevelRoute)
        }
    }

    @Test
    fun switchingTabsAndRestoringKeepsPageStateDespiteChangedHomePreference() = runComposeUiTest {
        val preference = mutableStateOf(DefaultHomeTabPreference.FEED)
        lateinit var state: NavigationState
        val visible = mutableStateOf(true)
        var registry = SaveableStateRegistry(null) { true }
        setContent {
            if (visible.value) {
                CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                    state = rememberMainNavigationState(preference.value)
                    MainTabNavDisplay(
                        navigationState = state,
                        entryProvider = { NavEntry(it) { CounterPage(it) } },
                        onBack = { Navigator(state).goBack() },
                    )
                }
            }
        }
        runOnIdle { Navigator(state).navigate(CalendarRoute) }
        onNodeWithText("CalendarRoute: 0").performClick()
        runOnIdle { Navigator(state).navigate(MediaRoute) }
        runOnIdle { Navigator(state).navigate(CalendarRoute) }
        onNodeWithText("CalendarRoute: 1").assertExists()

        runOnIdle { preference.value = DefaultHomeTabPreference.MORE }
        // StateRestorationTester's platform encoding is not implemented on JVM.
        val saved = runOnIdle { registry.performSave().also { visible.value = false } }
        runOnIdle {
            registry = SaveableStateRegistry(saved) { true }
            visible.value = true
        }
        onNodeWithText("CalendarRoute: 1").assertExists()
        runOnIdle {
            assertEquals(CalendarRoute, state.topLevelRoute)
            assertEquals(FeedRoute, state.startRoute)
        }
    }

    @Composable
    private fun CounterPage(route: NavKey) {
        var count by rememberSaveable { mutableIntStateOf(0) }
        Button(onClick = { count++ }) { Text("$route: $count") }
    }
}

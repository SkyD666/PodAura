package com.skyd.podaura.ui.player

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.skyd.podaura.model.preference.AcceptTermsPreference
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.model.preference.player.PlayerAutoPipPreference
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.screen.AppEntrance
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

class AppEntranceSessionTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun mainScreenDoesNotInspectOrDestroyAnExternallyOwnedSession() {
        val preferences = object : DataStore<Preferences> {
            override val data = MutableStateFlow(preferencesOf(
                AcceptTermsPreference.key to false,
                BackgroundPlayPreference.key to false,
                PlayerAutoPipPreference.key to false,
            ))
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                transform(data.value).also { data.value = it }
        }
        var sessionReads = 0
        var destroys = 0
        val session = object : PlayerSession {
            override val coordinator: PlayerCoordinator?
                get() { sessionReads++; return null }
            override val isFullPlayerVisible: Boolean
                get() { sessionReads++; return false }
            override fun openFullPlayer() = Unit
            override fun destroySession() { destroys++ }
        }
        startKoin { modules(module { single<DataStore<Preferences>> { preferences } }) }
        try {
            runDesktopComposeUiTest {
                setContent {
                    CompositionLocalProvider(
                        LocalPlayerSession provides session,
                        LocalNavigationEventDispatcherOwner provides rememberNavigationEventDispatcherOwner(),
                    ) { AppEntrance() }
                }
                waitForIdle()
                // MainActivity's visibility flag cannot describe PlayActivity or AVKit's window.
                // Even the entrance (before navigation) must leave lifetime to their owners.
                runOnIdle {
                    assertEquals(0, sessionReads)
                    assertEquals(0, destroys)
                }
            }
        } finally {
            stopKoin()
        }
    }
}

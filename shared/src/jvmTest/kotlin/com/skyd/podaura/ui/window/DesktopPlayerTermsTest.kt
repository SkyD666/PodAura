package com.skyd.podaura.ui.window

import com.skyd.podaura.model.repository.player.PlayerRepository
import com.skyd.podaura.ui.player.PlayerArticleContextViewModel
import com.skyd.podaura.ui.player.PlayerViewModel
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.io.File
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopPlayerTermsTest {
    @Test
    fun everyPlayerEntryRejectsBeforeEngineInitializationAndShowsMainWindow() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val (app, viewModel) = createApp { false }
            val entries: List<() -> Unit> = listOf(
                { app.openFiles(listOf(PlatformFile(File("unread.mp4")))) },
                { app.openPlayer(PlayDataMode.Playlist("unread", null)) },
                { app.openFullPlayer() },
            )
            for (open in entries) {
                val previousActivation = app.windowManager.windows.single().activationToken
                open()
                assertNull(app.coordinator)
                assertEquals(listOf(DesktopWindowId.Main), app.windowManager.windows.map { it.id })
                assertTrue(app.windowManager.windows.single().activationToken > previousActivation)
                assertTrue(viewModel.mediaInfos.replayCache.isEmpty())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun acceptingTermsDoesNotReplayRejectedFilesAndConsentIsCheckedAgain() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var accepted = false
            var checks = 0
            val (app, viewModel) = createApp { checks++; accepted }
            app.openFiles(listOf(PlatformFile(File("unread.mp4"))))
            accepted = true
            val activation = app.windowManager.windows.single().activationToken
            app.openFullPlayer()
            assertEquals(2, checks)
            assertEquals(activation, app.windowManager.windows.single().activationToken)
            assertNull(app.coordinator)
            assertTrue(viewModel.mediaInfos.replayCache.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createApp(accepted: () -> Boolean): Pair<DesktopAppState, PlayerViewModel> {
        val viewModel = PlayerViewModel(
            PlayerRepository(unused(), unused(), unused()), unused(),
        )
        return DesktopAppState(
            windowManager = DesktopWindowManager(),
            playerWindowController = PlayerWindowController(viewModel) {
                error("Must not create a media session before accepting terms")
            },
            playerArticleContextViewModel = PlayerArticleContextViewModel(unused()),
            hasAcceptedTerms = accepted,
        ) to viewModel
    }

    private inline fun <reified T> unused(): T = Proxy.newProxyInstance(
        T::class.java.classLoader, arrayOf(T::class.java),
    ) { _, method, _ -> error("Unexpected repository access: ${method.name}") } as T
}

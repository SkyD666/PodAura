package com.skyd.podaura.ui.window

import com.skyd.podaura.model.repository.player.PlayerRepository
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.repository.playlist.IPlaylistMediaRepository
import com.skyd.podaura.ui.player.mpv.MPVPlayer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import com.skyd.podaura.ui.player.PlayerArticleContextViewModel
import com.skyd.podaura.ui.player.PlayerViewModel
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
            val app = createApp { false }
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
            val app = createApp { checks++; accepted }
            app.openFiles(listOf(PlatformFile(File("unread.mp4"))))
            accepted = true
            val activation = app.windowManager.windows.single().activationToken
            app.openFullPlayer()
            assertEquals(2, checks)
            assertEquals(activation, app.windowManager.windows.single().activationToken)
            assertNull(app.coordinator)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun backgroundRequestsDoNotOpenOrActivateWindowsButExplicitOpensDo() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val acquired = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val owner = launch(Dispatchers.IO) {
            MPVPlayer.instance.withOwnership { acquired.complete(Unit); release.await() }
        }
        acquired.await()
        val viewModel = PlayerViewModel(
            PlayerRepository(unused(), unused(), unused()),
            object : IPlaylistMediaRepository {
                override fun requestPlaylistMediaList(playlistId: String) = flowOf(
                    listOf(PlaylistMediaWithArticleBean.fromUrl(playlistId, "media.mp3", 0.0)),
                )
            },
        )
        val controller = PlayerWindowController(viewModel) { null }
        val app = DesktopAppState(
            DesktopWindowManager(), controller, PlayerArticleContextViewModel(unused()),
            hasAcceptedTerms = { true },
        )
        try {
            val mode = PlayDataMode.Playlist("playlist", null)
            val initialWindows = app.windowManager.windows.toList()
            app.openPlayer(mode, showPlayer = false)
            assertEquals(initialWindows, app.windowManager.windows.toList())
            app.openFullPlayer()
            assertTrue(app.isFullPlayerVisible)
            val visibleWindows = app.windowManager.windows.toList()
            app.openPlayer(mode, showPlayer = false)
            assertEquals(visibleWindows, app.windowManager.windows.toList())
            app.openFiles(listOf(PlatformFile(File("external.mp4"))))
            assertTrue(app.windowManager.windows.last().activationToken > visibleWindows.last().activationToken)
        } finally {
            val coordinator = controller.coordinator
            controller.destroy()
            withContext(Dispatchers.IO) { coordinator?.awaitDestroyed() }
            release.complete(Unit)
            owner.join()
            Dispatchers.resetMain()
        }
    }

    private fun createApp(accepted: () -> Boolean): DesktopAppState {
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
        )
    }

    private inline fun <reified T> unused(): T = Proxy.newProxyInstance(
        T::class.java.classLoader, arrayOf(T::class.java),
    ) { _, method, _ -> error("Unexpected repository access: ${method.name}") } as T
}

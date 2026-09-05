package com.skyd.podaura.ui.player

import androidx.lifecycle.ViewModelStore
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.repository.player.PlayerRepository
import com.skyd.podaura.model.repository.playlist.IPlaylistMediaRepository
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import java.lang.reflect.Proxy
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExternalOpenViewModelTest {
    @Test
    fun replacingExternalFilesReleasesThePreviousPublishedBatch() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val first = Files.createTempFile("podaura-first-", ".mp3")
        val second = Files.createTempFile("podaura-second-", ".mp3")
        try {
            val viewModel = createViewModel()
            store.put("player", viewModel)
            viewModel.handlePlatformFiles(listOf(PlatformFile(first.toFile())), "A")
            val firstBatch = assertNotNull(viewModel.mediaInfos.first { it.requestId == "A" }.externalBatch)

            viewModel.handlePlatformFiles(listOf(PlatformFile(second.toFile())), "B")
            val secondBatch = assertNotNull(viewModel.mediaInfos.first { it.requestId == "B" }.externalBatch)

            assertFalse(firstBatch.retain())
            store.clear()
            assertFalse(secondBatch.retain())
            assertTrue(viewModel.mediaInfos.replayCache.isEmpty())
        } finally {
            store.clear()
            Files.deleteIfExists(first)
            Files.deleteIfExists(second)
            Dispatchers.resetMain()
        }
    }

    @Test
    fun internalPublicationReleasesOnlyTheViewModelsExternalOwnership() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val file = Files.createTempFile("podaura-open-", ".mp3")
        try {
            val viewModel = createViewModel()
            store.put("player", viewModel)
            viewModel.handlePlatformFiles(listOf(PlatformFile(file.toFile())), "external")
            val batch = assertNotNull(viewModel.mediaInfos.first().externalBatch)
            assertTrue(batch.retain()) // The coordinator owns playback independently of the view model.
            try {
                viewModel.handlePlayDataMode(PlayDataMode.Playlist("internal", null), "internal")
                val internal = viewModel.mediaInfos.first { it.requestId == "internal" }
                assertNull(internal.externalBatch)
                assertEquals("internal.mp3", internal.startPath)
                assertTrue(batch.retain())
                batch.release()
            } finally {
                batch.release()
            }
            assertFalse(batch.retain())
        } finally {
            store.clear()
            Files.deleteIfExists(file)
            Dispatchers.resetMain()
        }
    }

    @Test
    fun lateOldRequestCannotOverwriteNewExternalQueue() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val entered = CompletableDeferred<Unit>()
        val finishOld = CompletableDeferred<Unit>()
        val oldFinished = CompletableDeferred<Unit>()
        val store = ViewModelStore()
        val file = Files.createTempFile("podaura-open-", ".mp3")
        try {
            val viewModel = PlayerViewModel(
                PlayerRepository(unusedDao(), unusedDao(), unusedDao()),
                object : IPlaylistMediaRepository {
                    override fun requestPlaylistMediaList(playlistId: String) = flow {
                        entered.complete(Unit)
                        withContext(NonCancellable) { finishOld.await() }
                        try {
                            emit(listOf(PlaylistMediaWithArticleBean.fromUrl("", "old.mp3", 0.0)))
                        } finally {
                            oldFinished.complete(Unit)
                        }
                    }
                },
            )
            store.put("player", viewModel)
            viewModel.handlePlayDataMode(PlayDataMode.Playlist("old", null), "A")
            entered.await()
            viewModel.handlePlatformFiles(listOf(PlatformFile(file.toFile())), "B")
            val latest = viewModel.mediaInfos.first { it.requestId == "B" }
            assertEquals(listOf(file.toString()), latest.playlist.map { it.playlistMediaBean.stableUrl })
            assertEquals("", latest.playlist.single().playlistMediaBean.playlistId)
            finishOld.complete(Unit)
            oldFinished.await()
            assertEquals("B", viewModel.mediaInfos.replayCache.single().requestId)
        } finally {
            finishOld.complete(Unit)
            store.clear()
            Files.deleteIfExists(file)
            Dispatchers.resetMain()
        }
    }

    @Test
    fun desktopResolverRejectsDirectoriesAndMissingFilesAndPreservesUnicodePaths() {
        val directory = Files.createTempDirectory("podaura-open-")
        val file = directory.resolve("音频 with spaces.mp3")
        try {
            assertFailsWith<IllegalArgumentException> { resolveExternalMedia(PlatformFile(directory.toFile())) }
            assertFailsWith<IllegalArgumentException> { resolveExternalMedia(PlatformFile(file.toFile())) }
            Files.createFile(file)
            val resolved = resolveExternalMedia(PlatformFile(file.toFile()))
            assertEquals(file.toString(), resolved.source)
            assertEquals(file.toString(), resolved.playbackUrl)
            resolved.release()
        } finally {
            Files.deleteIfExists(file)
            Files.deleteIfExists(directory)
        }
    }

    private fun createViewModel() = PlayerViewModel(
        PlayerRepository(unusedDao(), unusedDao(), unusedDao()),
        object : IPlaylistMediaRepository {
            override fun requestPlaylistMediaList(playlistId: String) = flowOf(
                listOf(PlaylistMediaWithArticleBean.fromUrl(playlistId, "$playlistId.mp3", 0.0)),
            )
        },
    )

    private inline fun <reified T> unusedDao(): T = Proxy.newProxyInstance(
        T::class.java.classLoader, arrayOf(T::class.java),
    ) { _, method, _ -> error("Unexpected database access: ${method.name}") } as T
}

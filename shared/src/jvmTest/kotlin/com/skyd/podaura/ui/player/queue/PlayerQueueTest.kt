package com.skyd.podaura.ui.player.queue

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.ImageLoader
import coil3.PlatformContext
import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.coordinator.PlayerModel
import com.skyd.podaura.ui.player.service.PlayerState
import com.skyd.podaura.ui.player.withoutHotPlayerValues
import com.skyd.podaura.ui.screen.playlist.medialist.list.ListViewModel
import com.skyd.podaura.ui.screen.playlist.medialist.list.PlaylistMediaList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class)
class PlayerQueueTest {
    private lateinit var imageLoader: ImageLoader

    @BeforeTest
    fun setUp() {
        imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE).build()
        startKoin { modules(module { single<ImageLoader> { imageLoader } }) }
    }

    @AfterTest
    fun tearDown() {
        imageLoader.shutdown()
        stopKoin()
    }

    @Test
    fun modelEventsFollowCurrentItemAndReordersWithoutFollowingProgress() =
        runDesktopComposeUiTest(width = 420, height = 480) {
            val model = PlayerModel()
            val items = queueItems(40)
            fun publish(items: List<PlaylistMediaWithArticleBean>) = model.onEvent(
                PlayerEvent.Playlist("queue", items.associateByTo(linkedMapOf()) { it.playlistMediaBean.url }),
            )
            publish(items)
            model.onEvent(PlayerEvent.StartFile(items[20].playlistMediaBean.url))
            lateinit var listState: LazyListState
            setContent {
                // The same state projection used by the real player must retain reorder events.
                val source = androidx.compose.runtime.remember(model) { model.playerState.withoutHotPlayerValues() }
                val state by source.collectAsState(model.playerState.value)
                listState = rememberLazyListState()
                MaterialTheme { PlayerQueue(PlayState(false, false, state), queueCallbacks, state = listState) }
            }
            onNodeWithText(items[20].title).assertIsDisplayed()
            onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            runOnIdle { model.onEvent(PlayerEvent.Position(10)) }
            runOnIdle { assertEquals(0, listState.firstVisibleItemIndex) }
            runOnIdle { model.onEvent(PlayerEvent.StartFile(items[21].playlistMediaBean.url)) }
            onNodeWithText(items[21].title).assertIsDisplayed()
            runOnIdle { assertEquals(21, listState.firstVisibleItemIndex) }

            // Browse away, then reorder without changing the current path or any metadata.
            onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            runOnIdle { publish(items.reversed()) }
            onNodeWithText(items[21].title).assertIsDisplayed()
            runOnIdle { assertEquals(18, listState.firstVisibleItemIndex) }
            onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            val first = onNodeWithText(items[39].title).fetchSemanticsNode().boundsInRoot
            val second = onNodeWithText(items[38].title).fetchSemanticsNode().boundsInRoot
            assertTrue(first.top < second.top, "The actual displayed queue must use the new order")
            assertEquals(first.left, second.left, "The queue stays in one column")

            // Removing the current entry before the engine reports its successor must be safe.
            runOnIdle { publish(items.filterNot { it == items[21] }) }
            runOnIdle { model.onEvent(PlayerEvent.StartFile(items[22].playlistMediaBean.url)) }
            onNodeWithText(items[22].title).assertIsDisplayed()
            runOnIdle { assertEquals(21, listState.firstVisibleItemIndex) }
            runOnIdle { model.onEvent(PlayerEvent.StartFile("https://example.invalid/missing")) }
            runOnIdle { publish(emptyList()) }
            onNodeWithText("The queue is empty").assertIsDisplayed()
            runOnIdle {
                publish(listOf(items[0]))
                model.onEvent(PlayerEvent.StartFile(items[0].playlistMediaBean.url))
            }
            onNodeWithText(items[0].title).assertIsDisplayed()
            runOnIdle { assertEquals(0, listState.firstVisibleItemIndex) }
        }

    @Test
    fun unrelatedRecompositionDoesNotPullBrowsingBackToCurrentItem() =
        runDesktopComposeUiTest(width = 420, height = 480) {
            var state by mutableStateOf(queueState(queueItems(40), currentIndex = 20))
            lateinit var listState: LazyListState
            setContent {
                listState = rememberLazyListState()
                MaterialTheme { PlayerQueue(PlayState(false, false, state), queueCallbacks, state = listState) }
            }
            onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            // Deliberately pass raw progress and metadata updates, even though the real player filters ticks.
            runOnIdle { state = state.copy(position = 15, buffer = 50, mediaTitle = "Updated metadata") }
            runOnIdle { assertEquals(0, listState.firstVisibleItemIndex) }
            runOnIdle { state = state.copy(path = state.playlist[22].playlistMediaBean.url) }
            runOnIdle { assertEquals(22, listState.firstVisibleItemIndex) }
        }

    @Test
    fun currentItemChangeDuringUserInputIsNotLostAndDoesNotKeepPullingBack() =
        runDesktopComposeUiTest(width = 420, height = 480) {
            val items = queueItems(40)
            var state by mutableStateOf(queueState(items, currentIndex = 20))
            lateinit var listState: LazyListState
            lateinit var scope: CoroutineScope
            setContent {
                listState = rememberLazyListState()
                scope = rememberCoroutineScope()
                MaterialTheme { PlayerQueue(PlayState(false, false, state), queueCallbacks, state = listState) }
            }
            onNodeWithText(items[20].title).assertIsDisplayed()
            onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            val releaseDrag = CompletableDeferred<Unit>()
            val drag = runOnIdle {
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    // Hold the same priority used by touch dragging while the current media changes.
                    listState.scroll(MutatePriority.UserInput) {
                        scrollBy(1f)
                        releaseDrag.await()
                    }
                }
            }
            try {
                runOnIdle {
                    assertTrue(listState.isScrollInProgress)
                    assertTrue(drag.isActive)
                    state = state.copy(path = items[21].playlistMediaBean.url)
                }
                // Allow the key-triggered effect to run while UserInput still owns the mutex.
                waitForIdle()
                runOnIdle { releaseDrag.complete(Unit) }
                runOnIdle { assertEquals(21, listState.firstVisibleItemIndex) }
                onNodeWithText(items[21].title).assertIsDisplayed()

                // Following the new item is a one-shot request, not a persistent scroll constraint.
                onNode(hasScrollToIndexAction()).performScrollToIndex(0)
                runOnIdle { state = state.copy(position = 25, buffer = 50) }
                runOnIdle { assertEquals(0, listState.firstVisibleItemIndex) }
            } finally {
                runOnIdle { drag.cancel() }
            }
        }

    @Test
    fun rapidChangesDuringUserInputFollowOnlyTheLatestItemInTheUpdatedQueue() =
        runDesktopComposeUiTest(width = 420, height = 480) {
            val model = PlayerModel()
            val items = queueItems(40)
            fun publish(items: List<PlaylistMediaWithArticleBean>) = model.onEvent(
                PlayerEvent.Playlist("queue", items.associateByTo(linkedMapOf()) { it.playlistMediaBean.url }),
            )
            publish(items)
            model.onEvent(PlayerEvent.StartFile(items[10].playlistMediaBean.url))
            lateinit var listState: LazyListState
            lateinit var scope: CoroutineScope
            setContent {
                val state by model.playerState.collectAsState()
                listState = rememberLazyListState()
                scope = rememberCoroutineScope()
                MaterialTheme { PlayerQueue(PlayState(false, false, state), queueCallbacks, state = listState) }
            }
            onNodeWithText(items[10].title).assertIsDisplayed()
            onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            val releaseDrag = CompletableDeferred<Unit>()
            val drag = runOnIdle {
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    listState.scroll(MutatePriority.UserInput) {
                        scrollBy(1f)
                        releaseDrag.await()
                    }
                }
            }
            try {
                runOnIdle {
                    assertTrue(listState.isScrollInProgress)
                    assertTrue(drag.isActive)
                    model.onEvent(PlayerEvent.StartFile(items[30].playlistMediaBean.url))
                }
                waitForIdle()
                runOnIdle {
                    model.onEvent(PlayerEvent.StartFile(items[31].playlistMediaBean.url))
                    publish(items.reversed())
                }
                waitForIdle()
                // The current item can disappear before the engine supplies its replacement.
                runOnIdle { publish(items.filterNot { it == items[31] }) }
                waitForIdle()
                val finalQueue = items.drop(12).filterNot { it == items[31] }.reversed()
                runOnIdle {
                    model.onEvent(PlayerEvent.StartFile(items[19].playlistMediaBean.url))
                    model.onEvent(PlayerEvent.StartFile(items[20].playlistMediaBean.url))
                    model.onEvent(PlayerEvent.StartFile(items[32].playlistMediaBean.url))
                    publish(finalQueue)
                }
                val expectedIndex = finalQueue.indexOf(items[32])
                // The latest target is reached even before the held user-input block is released.
                runOnIdle { assertEquals(expectedIndex, listState.firstVisibleItemIndex) }
                onNodeWithText(items[32].title).assertIsDisplayed()
                runOnIdle { releaseDrag.complete(Unit) }
                mainClock.advanceTimeBy(250)
                runOnIdle {
                    assertEquals(expectedIndex, listState.firstVisibleItemIndex)
                    model.onEvent(PlayerEvent.Position(50))
                }
                runOnIdle { assertEquals(expectedIndex, listState.firstVisibleItemIndex) }
            } finally {
                runOnIdle { drag.cancel() }
            }
        }

    @Test
    fun savedPlaylistKeepsItsOwnSelectionWhenPlayerQueueIsOpened() =
        runDesktopComposeUiTest(width = 420, height = 600) {
            val items = queueItems(3)
            var showQueue by mutableStateOf(false)
            val store = ViewModelStore()
            lateinit var viewModel: ListViewModel
            setContent {
                MaterialTheme {
                    viewModel = remember { ListViewModel().also { store.put("saved-list", it) } }
                    val paging = remember { flowOf(PagingData.from(items)) }.collectAsLazyPagingItems()
                    if (showQueue) {
                        PlayerQueue(PlayState(false, false, queueState(items)), queueCallbacks)
                    } else {
                        PlaylistMediaList(
                            currentPlaylistId = "queue", playlist = paging, onMoved = { _, _ -> },
                            onPlay = {}, onDelete = {}, viewModel = viewModel,
                        )
                    }
                }
            }
            try {
                onNodeWithText(items[0].title).performTouchInput { longClick() }
                onNodeWithText(items[0].title).assertIsSelected()
                runOnIdle { showQueue = true }
                onNodeWithText(items[0].title).assertIsNotSelected()
                onNodeWithText(items[1].title).performTouchInput { longClick() }
                runOnIdle { showQueue = false }
                onNodeWithText(items[0].title).assertIsSelected()
                onNodeWithText(items[1].title).assertIsNotSelected()
            } finally {
                runOnIdle { store.clear() }
            }
        }

    @Test
    fun selectionIsLocalPrunesRemovedItemsAndDeletesLiveItemsOnly() =
        runDesktopComposeUiTest(width = 900, height = 600) {
            val items = queueItems(3)
            var state by mutableStateOf(queueState(items))
            var showFirst by mutableStateOf(true)
            var deletes = emptyList<PlaylistMediaWithArticleBean>()
            var played: String? = null
            val otherItems = queueItems(3, "other")
            setContent {
                MaterialTheme {
                    Row {
                        if (showFirst) PlayerQueue(
                            PlayState(false, false, state),
                            queueCallbacks.copy(
                                onRemoveFromPlaylist = { deletes = it },
                                onPlayFileInPlaylist = { played = it },
                            ),
                            Modifier.weight(1f).fillMaxHeight(),
                        )
                        PlayerQueue(
                            PlayState(false, false, queueState(otherItems)), queueCallbacks,
                            Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
            onNodeWithText(items[0].title).performClick()
            runOnIdle { assertEquals(items[0].playlistMediaBean.url, played) }
            onNodeWithText(items[0].title).performTouchInput { longClick() }
            onNodeWithText(items[0].title).assertIsSelected()
            onNodeWithText(items[1].title).performClick().assertIsSelected()
            onNodeWithText(otherItems[0].title).assertIsNotSelected()
            // Repeating a long press must not duplicate the deletion request.
            onNodeWithText(items[1].title).performTouchInput { longClick() }
            runOnIdle { state = queueState(items.drop(1)) }
            onNodeWithContentDescription("Delete").performClick()
            runOnIdle { assertEquals(listOf(items[1]), deletes) }
            onNodeWithContentDescription("Delete").assertDoesNotExist()
            onNodeWithText(items[1].title).performTouchInput { longClick() }
            runOnIdle { showFirst = false }
            onNodeWithText(otherItems[0].title).assertIsNotSelected()
            runOnIdle { showFirst = true }
            onNodeWithText(items[1].title).assertIsNotSelected()
            // New source playlists must not inherit the previous source's selection.
            onNodeWithText(items[1].title).performTouchInput { longClick() }
            runOnIdle { state = queueState(queueItems(3, "replacement")) }
            onNodeWithText("replacement episode 0").assertIsNotSelected()
            onNodeWithContentDescription("Delete").assertDoesNotExist()
        }
}

internal fun queueItems(count: Int, id: String = "queue") = List(count) { index ->
    PlaylistMediaWithArticleBean(
        PlaylistMediaBean(id, "https://example.invalid/$id/$index", null, index.toDouble(), 0).apply {
            title = "$id episode $index"
            artist = "PodAura podcast"
            duration = 3_661_000
        }, null,
    )
}

internal fun queueState(items: List<PlaylistMediaWithArticleBean>, currentIndex: Int = 0) = PlayerState(
    playlistId = items.firstOrNull()?.playlistMediaBean?.playlistId ?: "queue",
    playlist = items,
    path = items.getOrNull(currentIndex)?.playlistMediaBean?.url,
    mediaStarted = true,
    duration = 3661,
    position = 61,
    playlistPosition = currentIndex,
)

internal val queueCallbacks = PlayStateCallback(
    onPlayStateChanged = {}, onPlayOrPause = {}, onSeekTo = {}, onSeekBy = {},
    onSpeedChanged = {}, onPreviousMedia = {}, onNextMedia = {}, onCycleLoop = {},
    onShuffle = {}, onPlayFileInPlaylist = {}, onRemoveFromPlaylist = {},
)

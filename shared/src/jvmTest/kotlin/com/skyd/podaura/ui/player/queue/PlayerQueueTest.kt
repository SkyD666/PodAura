package com.skyd.podaura.ui.player.queue

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.ImageLoader
import coil3.PlatformContext
import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.service.PlayerState
import com.skyd.podaura.ui.screen.playlist.medialist.list.ListViewModel
import com.skyd.podaura.ui.screen.playlist.medialist.list.PlaylistMediaList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.flowOf
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
    fun followingIsExplicitAndManualScrollingUnlocksIt() =
        runDesktopComposeUiTest(width = 420, height = 480) {
            val items = queueItems(40)
            var state by mutableStateOf(queueState(items, currentIndex = 20))
            val browsing = PlayerQueueState()
            setContent {
                MaterialTheme { PlayerQueue(PlayState(false, false, state), queueCallbacks, queueState = browsing) }
            }
            onNodeWithText(items[20].title).assertIsDisplayed()
            onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            runOnIdle { state = state.copy(path = items[21].playlistMediaBean.url, position = 20) }
            runOnIdle { assertEquals(0, browsing.listState.firstVisibleItemIndex) }
            onNodeWithContentDescription("Follow current playback").performClick()
            onNodeWithText(items[21].title).assertIsDisplayed()
            runOnIdle { state = state.copy(path = items[22].playlistMediaBean.url) }
            onNodeWithText(items[22].title).assertIsDisplayed()
            onNode(hasScrollToIndexAction()).performTouchInput { swipeDown() }
            runOnIdle { assertTrue(!browsing.following) }
            val browsed = runOnIdle { browsing.listState.firstVisibleItemIndex }
            runOnIdle { state = state.copy(path = items[35].playlistMediaBean.url) }
            runOnIdle { assertEquals(browsed, browsing.listState.firstVisibleItemIndex) }
            onNodeWithContentDescription("Follow current playback").performClick()
            onNodeWithText(items[35].title).assertIsDisplayed()
            onNodeWithContentDescription("Stop following playback").performClick()
            val stoppedAt = runOnIdle { browsing.listState.firstVisibleItemIndex }
            runOnIdle { state = state.copy(path = items[0].playlistMediaBean.url) }
            runOnIdle { assertEquals(stoppedAt, browsing.listState.firstVisibleItemIndex) }
        }

    @Test
    fun handleRequiresLongPressOnTouchAndCommitsOnlyOnRelease() =
        runDesktopComposeUiTest(width = 420, height = 600) {
            val items = queueItems(6)
            var state by mutableStateOf(queueState(items))
            var calls = 0
            var played = 0
            val browsing = PlayerQueueState()
            setContent {
                MaterialTheme {
                    PlayerQueue(PlayState(false, false, state), queueCallbacks.copy(
                        onPlayFileInPlaylist = { played++ },
                        onReorderPlaylist = { _, from, to, original ->
                            calls++
                            val order = com.skyd.podaura.model.bean.playlist.movePlaylistItem(original, from, to)
                            state = state.copy(playlist = order.map { url -> items.first { it.playlistMediaBean.url == url } })
                        },
                    ), queueState = browsing)
                }
            }
            val handle = onAllNodesWithContentDescription("Reorder")[0]
            handle.performClick()
            runOnIdle { assertEquals(0, calls); assertEquals(0, played) }
            handle.performTouchInput { swipe(center, center + androidx.compose.ui.geometry.Offset(0f, 140f), 150) }
            runOnIdle { assertEquals(0, calls) }
            onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            onNodeWithContentDescription("Follow current playback").performClick()
            onAllNodesWithContentDescription("Reorder")[0].performTouchInput {
                down(center)
                advanceEventTime(700)
                moveBy(androidx.compose.ui.geometry.Offset(0f, 160f), 200)
            }
            waitForIdle()
            runOnIdle { assertEquals(0, calls); assertTrue(!browsing.following) }
            onNode(hasScrollToIndexAction()).performTouchInput { up() }
            waitForIdle()
            runOnIdle { assertEquals(1, calls); assertTrue(state.playlist.first() != items[0]) }
            onAllNodesWithContentDescription("Reorder")[0].performTouchInput {
                down(center)
                advanceEventTime(700)
                moveBy(androidx.compose.ui.geometry.Offset(0f, 100f), 150)
            }
            waitForIdle()
            // Desktop's injected cancel() is a no-op. Disabling the handle cancels its real
            // pointer coroutine and exercises the same cleanup as removal or a layout change.
            runOnIdle { state = state.copy(shuffle = true) }
            onNode(hasScrollToIndexAction()).performTouchInput { up() }
            runOnIdle { assertEquals(1, calls); state = state.copy(shuffle = false) }
            // Long-pressing the row still selects; selection hides handles and unlocks following.
            onNodeWithText(state.playlist.first().title).performTouchInput { longClick() }
            onAllNodesWithContentDescription("Reorder").assertCountEquals(0)
        }

    @Test
    fun mouseKeyboardAndAccessibilityActionsCanReorderWithoutLongPress() =
        runDesktopComposeUiTest(width = 420, height = 600) {
            val items = queueItems(6)
            var state by mutableStateOf(queueState(items))
            var calls = 0
            setContent {
                MaterialTheme {
                    PlayerQueue(PlayState(false, false, state), queueCallbacks.copy(
                        onReorderPlaylist = { _, from, to, original ->
                            calls++
                            val order = com.skyd.podaura.model.bean.playlist.movePlaylistItem(original, from, to)
                            state = state.copy(playlist = order.map { url -> items.first { it.playlistMediaBean.url == url } })
                        },
                    ))
                }
            }
            onAllNodesWithContentDescription("Reorder")[0].performMouseInput {
                moveTo(center)
                press()
                moveTo(center + androidx.compose.ui.geometry.Offset(0f, 30f), 20)
            }
            waitForIdle()
            onAllNodesWithContentDescription("Reorder")[0].performMouseInput {
                moveTo(center + androidx.compose.ui.geometry.Offset(0f, 160f), 80)
            }
            waitForIdle()
            onNode(hasScrollToIndexAction()).performMouseInput { release() }
            runOnIdle { assertEquals(1, calls) }
            onAllNodesWithContentDescription("Reorder")[0].performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            onAllNodesWithContentDescription("Reorder")[0].performKeyInput {
                keyDown(Key.AltLeft)
                keyDown(Key.DirectionDown)
                keyUp(Key.DirectionDown)
                keyUp(Key.AltLeft)
            }
            runOnIdle { assertEquals(2, calls) }
            val actions = onAllNodesWithContentDescription("Reorder")[0].fetchSemanticsNode().config[SemanticsActions.CustomActions]
            runOnIdle { actions.first { it.label == "Move down" }.action() }
            runOnIdle { assertEquals(3, calls) }
        }

    @Test
    fun reopeningQueuePreservesPositionAndFollowStateAndShuffleDisablesDragging() =
        runDesktopComposeUiTest(width = 420, height = 480) {
            val items = queueItems(40)
            var visible by mutableStateOf(true)
            var state by mutableStateOf(queueState(items, 20))
            val browsing = PlayerQueueState()
            setContent {
                MaterialTheme {
                    if (visible) PlayerQueue(PlayState(false, false, state), queueCallbacks, queueState = browsing)
                }
            }
            onNode(hasScrollToIndexAction()).performScrollToIndex(3)
            runOnIdle { visible = false }
            runOnIdle { visible = true }
            runOnIdle { assertEquals(3, browsing.listState.firstVisibleItemIndex); assertTrue(!browsing.following) }
            onNodeWithContentDescription("Follow current playback").performClick()
            runOnIdle { visible = false; state = state.copy(path = items[25].playlistMediaBean.url) }
            runOnIdle { visible = true }
            onNodeWithText(items[25].title).assertIsDisplayed()
            runOnIdle { state = state.copy(shuffle = true) }
            onAllNodesWithContentDescription("Reorder").assertCountEquals(0)
            onNodeWithText("Turn off shuffle to reorder the queue.").assertIsDisplayed()
        }

    @Test
    fun replacingTemporaryQueueResetsSelectionAndFollowing() =
        runDesktopComposeUiTest(width = 420, height = 480) {
            val items = queueItems(12, "")
            var state by mutableStateOf(queueState(items, 4).copy(queueId = "temporary-a"))
            setContent { MaterialTheme { PlayerQueue(PlayState(false, false, state), queueCallbacks) } }
            onNodeWithContentDescription("Follow current playback").performClick()
            onNodeWithText(items[4].title).performTouchInput { longClick() }
            onNodeWithText(items[4].title).assertIsSelected()
            onNodeWithContentDescription("Follow current playback").performClick()
            runOnIdle { state = state.copy(queueId = "temporary-b", path = items[0].playlistMediaBean.url) }
            onNodeWithContentDescription("Follow current playback").assertIsDisplayed()
            onNodeWithContentDescription("Delete").assertDoesNotExist()
            onNodeWithText(items[0].title).assertIsDisplayed().assertIsNotSelected()
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
                            currentPlaylistId = "queue", playlist = paging, onMoved = { _, _, _ -> },
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

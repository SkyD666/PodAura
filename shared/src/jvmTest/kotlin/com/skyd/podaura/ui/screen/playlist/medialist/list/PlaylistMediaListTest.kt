package com.skyd.podaura.ui.screen.playlist.medialist.list

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.ImageLoader
import coil3.PlatformContext
import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.ui.component.playlist.PlaylistDragState
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PlaylistMediaListTest {
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
    fun addingBeforeDraggedRowsKeepsOneSnapshotUntilRelease() =
        refreshWhileDragging(listOf("D", "C", "B", "A"))

    @Test
    fun removingRowsDuringDragKeepsOneSnapshotUntilRelease() =
        refreshWhileDragging(listOf("C", "B"))

    @Test
    fun refreshedRowsDoNotReplacePreviewPlaceholders() =
        refreshWhileDragging(listOf("D", "C", "B", "A"), placeholdersAfter = 1)

    @Test
    fun draggingLoadsMoreRowsOnlyFromTheSameIndexOrder() {
        val old = listOf(item("C"), item("B"), null)
        val drag = PlaylistDragState()
        drag.start(old, old.first()!!.playlistMediaBean.stableUrl)
        drag.move(0, 2, listOf(item("D"), item("C"), item("B")))
        assertEquals(old, drag.preview, "A refreshed page must not duplicate a known row")
        drag.move(0, 2, listOf(item("C"), item("B"), item("A")))
        assertEquals(listOf("B", "A", "C"), drag.preview!!.map { it?.title })
    }

    private fun refreshWhileDragging(updated: List<String>, placeholdersAfter: Int = 0) =
        runDesktopComposeUiTest(width = 420, height = 640) {
            val old = listOf("C", "B", "A")
            val pages = MutableStateFlow(PagingData.from(old.map(::item), placeholdersAfter = placeholdersAfter))
            val store = ViewModelStore()
            val model = ListViewModel().also { store.put("list", it) }
            setContent {
                MaterialTheme {
                    PlaylistMediaList(
                        currentPlaylistId = "saved", playlist = pages.collectAsLazyPagingItems(),
                        draggable = true, onMoved = { _, _, _ -> }, onPlay = {}, onDelete = {},
                        viewModel = model,
                    )
                }
            }
            try {
                onNodeWithText("C").assertIsDisplayed()
                onAllNodesWithContentDescription("Reorder")[0].performTouchInput {
                    down(center)
                    advanceEventTime(700)
                    moveBy(Offset(0f, 20f), 200)
                }
                waitForIdle()
                runOnIdle { pages.value = PagingData.from(updated.map(::item)) }
                waitForIdle()
                // A newly filled live placeholder must not duplicate A from the drag snapshot.
                old.forEach { onNodeWithText(it).assertIsDisplayed() }
                onNodeWithText("D").assertDoesNotExist()
                onAllNodesWithContentDescription("Reorder")[0].performTouchInput { up() }
                waitForIdle()
                updated.forEach { onNodeWithText(it).assertIsDisplayed() }
                (old - updated.toSet()).forEach { onNodeWithText(it).assertDoesNotExist() }
            } finally {
                runOnIdle { store.clear() }
            }
        }

    private fun item(name: String) = PlaylistMediaWithArticleBean(
        PlaylistMediaBean("saved", "https://example.invalid/$name", null, 1.0, 0).apply {
            title = name
            artist = "Podcast"
        }, null,
    )
}

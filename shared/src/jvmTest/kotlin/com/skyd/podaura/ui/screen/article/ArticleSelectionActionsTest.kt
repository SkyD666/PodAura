package com.skyd.podaura.ui.screen.article

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.sp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeTopBar
import com.skyd.podaura.model.bean.playlist.PlaylistBean
import com.skyd.podaura.model.bean.playlist.PlaylistViewBean
import com.skyd.podaura.model.preference.language.AppLanguage
import com.skyd.podaura.ui.component.AppLanguageProvider
import com.skyd.podaura.ui.screen.playlist.addto.AddToPlaylistSheetContent
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.add_to_playlist
import podaura.shared.generated.resources.article_screen_favorite
import podaura.shared.generated.resources.article_screen_mark_as_read
import podaura.shared.generated.resources.article_screen_mark_as_unread
import podaura.shared.generated.resources.article_screen_unfavorite
import podaura.shared.generated.resources.article_selection_continue
import podaura.shared.generated.resources.article_selection_exit_message
import podaura.shared.generated.resources.article_selection_stop_exit
import podaura.shared.generated.resources.more

class ArticleSelectionActionsTest {
    @Test
    fun narrowChineseToolbarPlaylistAndExitDialog() = checkActions(320, AppLanguage.SimplifiedChinese)

    @Test
    fun wideEnglishToolbarPlaylistAndExitDialog() = checkActions(840, AppLanguage.English)

    @OptIn(ExperimentalTestApi::class)
    private fun checkActions(width: Int, language: AppLanguage) = runDesktopComposeUiTest(width = width, height = 520) {
        var busy by mutableStateOf(false)
        var showExit by mutableStateOf(false)
        var confirmCount = 0
        var dismissCount = 0
        var addedCount = 0
        val actions = mutableListOf<String>()
        var more = ""
        var labels = emptyList<String>()
        var continueLabel = ""
        var exitLabel = ""
        var message = ""
        val added = PlaylistViewBean(PlaylistBean("added", "Already added playlist", 10.0, 0, false), 3)
        val available = PlaylistViewBean(PlaylistBean("available", "Another playlist with a longer name", 20.0, 0, false))
        val playlists = flowOf(PagingData.from(listOf(added, available)))
        setContent {
            AppLanguageProvider(language) {
                MaterialTheme {
                    more = stringResource(Res.string.more)
                    labels = listOf(
                        stringResource(Res.string.article_screen_mark_as_read),
                        stringResource(Res.string.article_screen_mark_as_unread),
                        stringResource(Res.string.article_screen_favorite),
                        stringResource(Res.string.article_screen_unfavorite),
                        stringResource(Res.string.add_to_playlist),
                    )
                    continueLabel = stringResource(Res.string.article_selection_continue)
                    exitLabel = stringResource(Res.string.article_selection_stop_exit)
                    message = stringResource(Res.string.article_selection_exit_message)
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize().testTag("SelectionPreview")) {
                            ComponeTopBar(
                                title = { Text("12345", maxLines = 1, autoSize = TextAutoSize.StepBased(
                                    minFontSize = 12.sp, maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
                                )) },
                                navigationIcon = { BackIcon(onClick = { if (busy) showExit = true }) },
                                actions = {
                                    ArticleSelectionActions(
                                        selection = ArticleSelectionState(active = true, selectedIds = setOf("episode"), busy = busy),
                                        onSelectAll = {}, onClearSelection = {}, onDownload = {},
                                        onRead = { actions += "read:$it" },
                                        onFavorite = { actions += "favorite:$it" },
                                        onAddToPlaylist = { actions += "playlist" },
                                    )
                                },
                            )
                            AddToPlaylistSheetContent(
                                playlist = playlists.collectAsLazyPagingItems(),
                                selected = { it.playlist.playlistId == "added" },
                                onSelect = { addedCount++ },
                                onRemove = { error("Bulk mode must never remove media") },
                                addOnly = true,
                                enabled = !busy,
                            )
                        }
                        if (showExit) ArticleSelectionExitDialog(
                            onDismiss = { showExit = false; dismissCount++ },
                            onConfirm = { showExit = false; confirmCount++ },
                        )
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 10_000) { onAllNodes(androidx.compose.ui.test.hasText(available.playlist.name)).fetchSemanticsNodes().isNotEmpty() }
        fun save(name: String, bitmap: androidx.compose.ui.graphics.ImageBitmap) {
            val output = File("build/reports/article-batch-$name-$width.png")
            output.parentFile.mkdirs()
            output.writeBytes(Image.makeFromBitmap(bitmap.asSkiaBitmap()).encodeToData()!!.bytes)
        }
        fun overlayImage(): androidx.compose.ui.graphics.ImageBitmap {
            val roots = onAllNodes(isRoot())
            return roots[roots.fetchSemanticsNodes().lastIndex].captureToImage()
        }
        save("actions", onNodeWithTag("SelectionPreview").captureToImage())
        labels.forEachIndexed { index, label ->
            onNodeWithContentDescription(more).performClick()
            if (index == 0) save("menu", overlayImage())
            onNodeWithText(label).performClick()
        }
        assertEquals(listOf("read:true", "read:false", "favorite:true", "favorite:false", "playlist"), actions)
        onNodeWithText(added.playlist.name).assertIsNotEnabled().performTouchInput { click() }
        onNodeWithText(available.playlist.name).performClick()
        assertEquals(1, addedCount)
        runOnIdle { busy = true }
        onNodeWithContentDescription(more).assertIsNotEnabled()
        onNodeWithText(available.playlist.name).assertIsNotEnabled().performTouchInput { click() }
        assertEquals(1, addedCount)
        runOnIdle { showExit = true }
        onNodeWithText(message).assertExists()
        save("exit", overlayImage())
        onNodeWithText(continueLabel).performClick()
        assertEquals(0, confirmCount)
        assertEquals(1, dismissCount)
        runOnIdle { showExit = true }
        onNodeWithText(exitLabel).performClick()
        assertEquals(1, confirmCount)
    }
}

package com.skyd.podaura.app

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.skyd.podaura.api.model.bean.download.AssistantDownloadReceipt
import com.skyd.podaura.api.model.repository.IAssistantDownloadRepository
import com.skyd.podaura.ui.screen.download.DownloadListFocus
import com.skyd.podaura.podnya.capabilities.downloads.OpenDownloads
import com.skyd.podaura.podnya.core.AssistantAction
import kotlinx.coroutines.runBlocking
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class PodnyaDownloadPluginTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun restoredSheetKeepsItsRequestWhenAnotherPluginIsRemoved() {
        val app = koinApplication {}
        try {
            val scope = app.koin.createScope<PodnyaDownloadPluginTest>("test")
            runDesktopComposeUiTest(width = 400, height = 600) {
                val conversationId = Uuid.random()
                val label = "$conversationId:12 option:a:1"
                fun downloads() = createDownloadPlugin { request, _ ->
                    BasicText("${request.batchId} ${request.submissionIds.single()}")
                }
                val other = PodnyaPlugin(id = "other") { _, _ -> { _, _ -> } }
                var plugins = listOf(other, downloads())
                var registry = SaveableStateRegistry(null) { true }
                var visible by mutableStateOf(true)
                lateinit var dispatch: (Uuid, AssistantAction) -> Unit
                setContent {
                    if (visible) {
                        CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                            dispatch = plugins.bindNavigation(scope) {}
                        }
                    }
                }
                runOnIdle {
                    dispatch(conversationId, OpenDownloads(originTurnId = 12, submissionIds = listOf("option:a:1")))
                }
                onNodeWithText(label).assertExists()
                val saved = runOnIdle { registry.performSave().also { visible = false } }
                onNodeWithText(label).assertDoesNotExist()
                runOnIdle {
                    registry = SaveableStateRegistry(saved) { true }
                    plugins = listOf(downloads())
                    visible = true
                }
                onNodeWithText(label).assertExists()
            }
        } finally {
            app.close()
        }
    }

    @Test
    fun openSheetRequestRestoresTheSameHistoricalSubmissions() {
        val request = DownloadSheetRequest(batchId = "session:12", submissionIds = listOf("turn:12", "option:a:1"))
        val scope = SaverScope { it is String }
        val saved = with(DownloadSheetRequest.Saver) { scope.save(request) }
        assertEquals(request, DownloadSheetRequest.Saver.restore(checkNotNull(saved)))
        assertNull(with(DownloadSheetRequest.Saver) { scope.save(null) })
    }

    @Test
    fun focusReadsOnlyTheClickedCardsSubmissionsAndKeepsPartialReceipts() = runBlocking {
        val read = mutableListOf<String>()
        val downloads = object : IAssistantDownloadRepository by TestDownloads() {
            override suspend fun receipt(batchId: String, submissionId: String): AssistantDownloadReceipt? {
                assertEquals("session:12", batchId)
                read += submissionId
                if (submissionId == "missing") return null
                if (submissionId == "unreadable") error("Unreadable receipt")
                return AssistantDownloadReceipt(
                    batchId = batchId,
                    submissionId = submissionId,
                    queued = mapOf("added" to "new-$submissionId"),
                    existing = mapOf("duplicate" to "already-there"),
                    failed = setOf("failed"),
                    matchedCount = 3,
                    remainingCount = 0,
                    limitExceeded = false,
                )
            }
        }
        val focus = downloads.downloadListFocus(
            batchId = "session:12",
            submissionIds = listOf("a", "missing", "unreadable", "b", "a"),
        )
        assertEquals(listOf("a", "missing", "unreadable", "b"), read)
        assertEquals(DownloadListFocus(queued = setOf("new-a", "new-b"), existing = setOf("already-there")), focus)
    }
}

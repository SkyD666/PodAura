package com.skyd.podaura.nano.ui.screen.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.savedstate.serialization.SavedStateConfiguration
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.component.navigation.newNavBackStack
import com.skyd.compone.local.LocalWindowController
import com.skyd.compone.local.WindowController
import com.skyd.fundation.credential.CredentialStore
import com.skyd.podaura.api.model.bean.download.AssistantDownloadTarget
import com.skyd.podaura.api.model.repository.IAssistantDownloadRepository
import com.skyd.podaura.nano.capabilities.articles.model.ArticleHistoryCodec
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchAction
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchContent
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchPhase
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchProgress
import com.skyd.podaura.nano.capabilities.articles.model.bean.AssistantArticle
import com.skyd.podaura.nano.capabilities.articles.model.bean.RecommendedArticle
import com.skyd.podaura.nano.capabilities.articles.model.bean.ValidatedAssistantAnswer
import com.skyd.podaura.nano.capabilities.articles.ui.ArticleSearchUi
import com.skyd.podaura.nano.capabilities.downloads.DownloadConversation
import com.skyd.podaura.nano.capabilities.downloads.DownloadUi
import com.skyd.podaura.nano.capabilities.downloads.OpenDownloads
import com.skyd.podaura.nano.capabilities.downloads.TestAssistantDownloadQueue
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadChoice
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadContent
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadIssue
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadIssueKind
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadOption
import com.skyd.podaura.nano.capabilities.downloads.selection.ValidatedDownloadPlan
import com.skyd.podaura.nano.capabilities.feeds.model.FeedHistoryCodec
import com.skyd.podaura.nano.capabilities.feeds.model.bean.AssistantFeed
import com.skyd.podaura.nano.capabilities.feeds.model.bean.FeedSearchAction
import com.skyd.podaura.nano.capabilities.feeds.model.bean.FeedSearchContent
import com.skyd.podaura.nano.capabilities.feeds.model.bean.RecommendedFeed
import com.skyd.podaura.nano.capabilities.feeds.ui.FeedSearchUi
import com.skyd.podaura.nano.core.AssistantAnswerKind
import com.skyd.podaura.nano.core.AssistantConnection
import com.skyd.podaura.nano.core.AssistantConnectionResult
import com.skyd.podaura.nano.core.AssistantContent
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantHistorySnapshot
import com.skyd.podaura.nano.core.AssistantProgress
import com.skyd.podaura.nano.core.AssistantRunEvent
import com.skyd.podaura.nano.core.AssistantRuntime
import com.skyd.podaura.nano.core.AssistantTurnRequest
import com.skyd.podaura.nano.model.bean.conversation.*
import com.skyd.podaura.nano.model.bean.conversation.AssistantTurnStatus
import com.skyd.podaura.nano.model.db.ConversationRecordCodec
import com.skyd.podaura.nano.model.repository.conversation.AssistantConversationRepository
import com.skyd.podaura.nano.model.repository.conversation.ConversationCredentials
import com.skyd.podaura.nano.model.repository.conversation.ConversationStore
import com.skyd.podaura.nano.model.repository.conversation.InMemoryConversationStore
import com.skyd.podaura.nano.model.repository.conversation.RoomConversationStore
import com.skyd.podaura.nano.settings.AssistantSettingsRepository
import com.skyd.podaura.nano.ui.LocalCapabilityUis
import com.skyd.nano.resources.*
import com.skyd.nano.resources.Res
import com.skyd.nano.resources.nano_download_missing_attachment
import com.skyd.nano.resources.nano_download_open
import com.skyd.nano.resources.nano_download_result
import com.skyd.nano.resources.nano_latest
import com.skyd.nano.resources.nano_new_conversation
import com.skyd.nano.resources.nano_open_search
import com.skyd.nano.resources.nano_preparing
import com.skyd.nano.resources.nano_retry
import com.skyd.nano.resources.nano_settings_title
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Real screen/session fixtures; captures can be compared before and after UI changes. */
@OptIn(ExperimentalTestApi::class)
class NanoScreenTest {
    @Test
    fun userMessageBubblesFitTheirContentAndWrapAtTheWidthLimit() {
        val messages = listOf(
            "你好",
            "请推荐一档关于音乐的播客",
            "I am looking for a podcast that explores how music changes across generations. "
                .repeat(4).trimEnd(),
        )
        val session = AssistantSessionState(
            turns = messages.mapIndexed { index, text ->
                AssistantSessionTurn(
                    id = index + 1L,
                    question = "**$text**",
                    status = AssistantTurnStatus.COMPLETED,
                )
            },
        )
        val state = NanoState.initial(
            conversations = AssistantConversationsState(selected = session),
            available = true,
        )

        for ((width, maxBubbleWidth) in listOf(390 to 350.dp, 1100 to 620.dp)) {
            runDesktopComposeUiTest(width = width, height = 844) {
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        MaterialTheme {
                            NanoMessageList(
                                uiState = state,
                                listState = rememberLazyListState(),
                                followLatest = false,
                                onFollowLatest = {},
                                keyboardVisible = false,
                                onHideKeyboard = {},
                                formatDate = { "" },
                                dispatch = {},
                            )
                        }
                    }
                }
                waitUntil(timeoutMillis = 5_000) {
                    messages.all { onAllNodes(hasText(it)).fetchSemanticsNodes().size == 1 }
                }
                val short = onNodeWithTag("nano_user_message_1").getUnclippedBoundsInRoot()
                val medium = onNodeWithTag("nano_user_message_2").getUnclippedBoundsInRoot()
                val long = onNodeWithTag("nano_user_message_3").getUnclippedBoundsInRoot()
                assertTrue(short.left > medium.left && medium.left > long.left)
                assertEquals(maxBubbleWidth, long.right - long.left)
                assertTrue(long.bottom - long.top > medium.bottom - medium.top)
                assertEquals(long.right, short.right)
                assertEquals(long.right, medium.right)
            }
        }
    }

    @Test
    fun chatDownloadShowsReceiptAndOpensDownloads() = runDesktopComposeUiTest(width = 390, height = 844) {
        Fixture(mode = Mode.DOWNLOAD, downloads = TestAssistantDownloadQueue()).use { fixture ->
            setContent { fixture.Screen() }
            waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
            onNodeWithTag("nano_composer").performTextReplacement("Download these episodes")
            onNodeWithTag("nano_send").performClick()
            waitUntil(timeoutMillis = 5_000) {
                val content = fixture.repository.state.value.selected.turns.lastOrNull()?.content as? DownloadContent
                content?.submissions?.singleOrNull()?.receipt?.added == 1
            }
            onNodeWithTag("nano_messages").performScrollToNode(hasText(fixture.downloadReceiptLabel))
            onNodeWithText(fixture.downloadReceiptLabel).assertIsDisplayed()
            onNodeWithText(fixture.downloadMissingLabel).assertIsDisplayed()
            onNodeWithText("1. MP3 · 64 kbps").performClick()
            waitUntil(timeoutMillis = 5_000) {
                val content = fixture.repository.state.value.selected.turns.lastOrNull()?.content as? DownloadContent
                content?.submissions?.count { it.receipt?.added == 1 } == 2
            }
            onNodeWithTag("nano_messages").performScrollToNode(hasText(fixture.openDownloadsLabel))
            onNodeWithText(fixture.openDownloadsLabel).performClick()
            waitUntil(timeoutMillis = 5_000) { fixture.openedDownloads != null }
            val turnId = fixture.repository.state.value.selected.turns.single().id
            assertEquals(
                OpenDownloads(originTurnId = turnId, submissionIds = listOf("turn:$turnId", "option:version:1")),
                fixture.openedDownloads,
            )
        }
    }

    @Test
    fun subscriptionCardShowsLocalContentAndOpensTheFeed() {
        val layouts = listOf(
            Layout("phone", 390, 844),
            Layout("desktop", 1100, 820),
            Layout("phone-dark", 390, 844, dark = true),
            Layout("phone-large-type", 390, 844, fontScale = 1.3f),
        )
        for (layout in layouts) {
            runDesktopComposeUiTest(width = layout.width, height = layout.height) {
                Fixture(mode = Mode.FEEDS).use { fixture ->
                    setContent { fixture.Screen(layout.dark, layout.fontScale) }
                    waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                    onNodeWithTag("nano_composer").performTextReplacement("找一下音乐订阅")
                    onNodeWithTag("nano_send").performClick()
                    waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.turns.lastOrNull()?.status == AssistantTurnStatus.COMPLETED }
                    onNodeWithTag("nano_messages").performScrollToNode(hasText("音乐与生活"))
                    onNodeWithText("音乐与生活").assertIsDisplayed()
                    capture("${layout.name}-feed-result")
                    onNodeWithText("音乐与生活").performClick()
                    waitUntil(timeoutMillis = 5_000) { fixture.openedFeed == "https://example.test/music" }
                    assertEquals("https://example.test/music", fixture.openedFeed)
                    assertEquals(null, fixture.openedArticle)
                }
            }
        }
    }

    @OptIn(InternalComposeUiApi::class)
    @Test
    fun keyboardDismissalKeepsDraftAndGenerationAndOnlyThenAllowsBack() =
        runDesktopComposeUiTest(width = 390, height = 844) {
            Fixture(mode = Mode.WAITING).use { fixture ->
                var keyboardHeight by mutableStateOf(0)
                var hides = 0
                var pageBacks = 0
                val insets = object : PlatformWindowInsets {
                    override val ime get() = PlatformInsets(bottom = keyboardHeight)
                }
                val keyboard = object : SoftwareKeyboardController {
                    override fun show() { keyboardHeight = 300 }
                    override fun hide() { hides++; keyboardHeight = 0 }
                }
                val backInput = object : NavigationEventInput() {
                    fun start() = dispatchOnBackStarted(NavigationEvent())
                    fun cancel() = dispatchOnBackCancelled()
                    fun complete() = dispatchOnBackCompleted()
                }
                setContent {
                    val dispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current)
                        .navigationEventDispatcher
                    DisposableEffect(dispatcher) {
                        dispatcher.addInput(backInput)
                        onDispose { dispatcher.removeInput(backInput) }
                    }
                    NavigationBackHandler(
                        state = rememberNavigationEventState(NavigationEventInfo.None),
                        isBackEnabled = true,
                        onBackCompleted = { pageBacks++ },
                    )
                    CompositionLocalProvider(
                        LocalPlatformWindowInsets provides insets,
                        LocalSoftwareKeyboardController provides keyboard,
                    ) { fixture.Screen() }
                }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                onNodeWithTag("nano_composer").performTextReplacement(QUESTION)
                onNodeWithTag("nano_send").performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.isRunning }
                onNodeWithTag("nano_composer").performClick().performTextReplacement("Next question")
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.draft == "Next question" }
                runOnIdle { keyboard.show() }
                capture("phone-keyboard-visible")

                runOnIdle { backInput.start(); backInput.cancel() }
                runOnIdle {
                    assertEquals(0, hides)
                    assertEquals(0, pageBacks)
                }
                runOnIdle { backInput.start(); backInput.complete() }
                onNodeWithTag("nano_composer").assertIsNotFocused()
                runOnIdle { assertEquals(0, pageBacks) }

                onNodeWithTag("nano_composer").performClick()
                runOnIdle { keyboard.show() }
                onNodeWithTag("nano_composer").performTouchInput { click() }
                runOnIdle { assertEquals(300, keyboardHeight) }
                onNodeWithTag("nano_messages").performTouchInput { click(center) }
                onNodeWithTag("nano_composer").assertIsNotFocused()
                runOnIdle {
                    assertEquals(0, keyboardHeight)
                    assertEquals("Next question", fixture.repository.state.value.selected.draft)
                    assertTrue(fixture.repository.state.value.selected.isRunning)
                    assertFalse(fixture.assistant.cancelled.get())
                }
                runOnIdle { backInput.complete() }
                runOnIdle { assertEquals(1, pageBacks) }
            }
        }

    @OptIn(InternalComposeUiApi::class)
    @Test
    fun firstArticleTapDismissesKeyboardAndSecondTapOpensArticle() =
        runDesktopComposeUiTest(width = 390, height = 844) {
            Fixture().use { fixture ->
                var keyboardHeight by mutableStateOf(0)
                val insets = object : PlatformWindowInsets {
                    override val ime get() = PlatformInsets(bottom = keyboardHeight)
                }
                val keyboard = object : SoftwareKeyboardController {
                    override fun show() { keyboardHeight = 300 }
                    override fun hide() { keyboardHeight = 0 }
                }
                setContent {
                    CompositionLocalProvider(
                        LocalPlatformWindowInsets provides insets,
                        LocalSoftwareKeyboardController provides keyboard,
                    ) { fixture.Screen() }
                }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                onNodeWithTag("nano_composer").performTextReplacement(QUESTION)
                onNodeWithTag("nano_send").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    fixture.repository.state.value.selected.turns.lastOrNull()?.status == AssistantTurnStatus.COMPLETED
                }
                onNodeWithTag("nano_messages").performScrollToNode(hasText(FIRST_TITLE))
                runOnIdle { keyboard.show() }
                onNodeWithTag("nano_messages").performTouchInput { swipeUp() }
                runOnIdle { assertEquals(300, keyboardHeight) }
                onNodeWithTag("nano_messages").performScrollToNode(hasText(FIRST_TITLE))
                onNodeWithText(FIRST_TITLE).performTouchInput { click() }
                runOnIdle {
                    assertEquals(0, keyboardHeight)
                    assertEquals(null, fixture.openedArticle)
                }
                onNodeWithText(FIRST_TITLE).performTouchInput { click() }
                waitUntil(timeoutMillis = 5_000) { fixture.openedArticle == "luo-1" }
            }
        }

    @Test
    fun longProfileAndCharacterCounterLeaveRoomForSendOnNarrowScreens() =
        runDesktopComposeUiTest(width = 320, height = 720) {
            Fixture().use { fixture ->
                runBlocking {
                    val active = fixture.settings.snapshot()
                    fixture.settings.save(
                        baseUrl = active.baseUrl,
                        modelId = active.modelId,
                        apiKey = null,
                        editing = active,
                        name = "Long profile name ".repeat(8),
                    )
                }
                setContent { fixture.Screen(fontScale = 1.5f) }
                waitUntil { fixture.viewModel.viewState.value.ready }
                onNodeWithTag("nano_composer").performTextReplacement("q".repeat(1800))
                val bounds = onNodeWithTag("nano_send").getUnclippedBoundsInRoot()
                assertTrue(bounds.right - bounds.left >= 48.dp)
            }
        }

    @Test
    fun unconfiguredScreenKeepsInputEditableAndSendDisabled() =
        runDesktopComposeUiTest(width = 1100, height = 820) {
            Fixture(configured = false).use { fixture ->
                setContent { fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.profiles != null }
                capture("desktop-unconfigured")
                onNodeWithTag("nano_send").assertIsNotEnabled()
                onNodeWithTag("nano_composer").performTextReplacement(QUESTION)
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.draft == QUESTION }
                capture("desktop-unconfigured-typing")
                onNodeWithTag("nano_send").assertIsNotEnabled()
                assertEquals(0, fixture.assistant.requests.get())
                onNodeWithTag("nano_settings").assertIsDisplayed().performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.settingsOpened == 1 }
                assertEquals(1, fixture.settingsOpened)
                onNodeWithTag("nano_more").assertDoesNotExist()
                onNodeWithText(fixture.searchLabel).assertDoesNotExist()
                assertEquals(0, fixture.searchOpened)
            }
        }

    @Test
    fun recommendationsAndComposerFitDesktopPhoneTabletLandscapeAndLargeType() {
        val cases = listOf(
            Layout("desktop", 1100, 820),
            Layout("phone", 390, 844),
            Layout("tablet", 840, 1000),
            Layout("landscape", 844, 390),
            Layout("phone-dark", 390, 844, dark = true),
            Layout("phone-large-type", 390, 844, fontScale = 1.3f),
        )
        for (layout in cases) runDesktopComposeUiTest(width = layout.width, height = layout.height) {
            Fixture().use { fixture ->
                setContent { fixture.Screen(layout.dark, layout.fontScale) }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.profiles?.active?.configured == true }
                capture("${layout.name}-empty")
                onNodeWithTag("nano_composer").performTextReplacement(QUESTION)
                if (layout.fontScale > 1f || layout.name == "landscape") capture("${layout.name}-typing")
                assertComposerFits(layout.width, layout.height)
                onNodeWithTag("nano_send").assertIsEnabled().performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.turns.lastOrNull()?.status == AssistantTurnStatus.COMPLETED }
                capture("${layout.name}-recommendations")
                assertComposerFits(layout.width, layout.height)
                onNodeWithTag("nano_messages").performScrollToNode(hasText(FIRST_TITLE))
                onNodeWithText("音乐与生活 · $FORMATTED_DATE").assertIsDisplayed()
                assertEquals(ARTICLE_DATE, fixture.formattedDate)
                onNodeWithText(FIRST_TITLE).assertIsDisplayed().performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.openedArticle == "luo-1" }
                assertEquals("luo-1", fixture.openedArticle)
                assertEquals(1, fixture.assistant.requests.get())
                assertEquals(QUESTION, fixture.assistant.lastQuestion)
            }
        }
    }

    @Test
    fun queuedDraftEchoesDoNotOverwriteNewerTextOrSelection() =
        runDesktopComposeUiTest(width = 1100, height = 820) {
            Fixture().use { fixture ->
                setContent { fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.profiles?.active?.configured == true }
                onNodeWithTag("nano_composer").performClick().performTextReplacement("acb")
                onNodeWithTag("nano_composer").performTextInputSelection(TextRange(2))
                // Deliver an older draft snapshot after the editor has already advanced.
                runOnIdle {
                    fixture.viewModel.viewModelScope.launch {
                        val current = fixture.repository.state.value.selected
                        fixture.viewModel.processIntent(NanoIntent.UpdateDraft(current.id, "ab", current.editorId, current.draftRevision - 1))
                    }
                }
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.draft == "acb" }
                onNodeWithTag("nano_composer")
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("acb")))
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(2)))
                    .performTextInput("x")
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.draft == "acxb" }
                assertEquals(0, fixture.assistant.requests.get())
            }
        }

    @Test
    fun enterSendsAndShiftEnterInsertsANewLine() =
        runDesktopComposeUiTest(width = 1100, height = 820) {
            Fixture().use { fixture ->
                setContent { fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.profiles?.active?.configured == true }
                onNodeWithTag("nano_composer").performClick().performTextReplacement(QUESTION)
                onNodeWithTag("nano_composer").performKeyInput {
                    keyDown(Key.ShiftLeft)
                    pressKey(Key.Enter)
                    keyUp(Key.ShiftLeft)
                }
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.draft.contains('\n') }
                assertEquals(0, fixture.assistant.requests.get())
                onNodeWithTag("nano_composer").performKeyInput { pressKey(Key.Enter) }
                waitUntil(timeoutMillis = 5_000) {
                    fixture.repository.state.value.selected.turns.lastOrNull()?.status == AssistantTurnStatus.COMPLETED
                }
                assertEquals(1, fixture.assistant.requests.get())
                assertEquals(QUESTION, fixture.assistant.lastQuestion)
                assertEquals("", fixture.repository.state.value.selected.draft)
                onNodeWithTag("nano_composer")
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
            }
        }

    @Test
    fun numpadEnterSendsAndShiftNumpadEnterInsertsANewLine() =
        runDesktopComposeUiTest(width = 1100, height = 820) {
            Fixture().use { fixture ->
                setContent { fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                onNodeWithTag("nano_composer").performClick().performTextReplacement(QUESTION)
                onNodeWithTag("nano_composer").performKeyInput {
                    keyDown(Key.ShiftLeft)
                    pressKey(Key.NumPadEnter)
                    keyUp(Key.ShiftLeft)
                }
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.draft.contains('\n') }
                assertEquals(0, fixture.assistant.requests.get())
                onNodeWithTag("nano_composer").performKeyInput { pressKey(Key.NumPadEnter) }
                waitUntil(timeoutMillis = 5_000) {
                    fixture.repository.state.value.selected.turns.lastOrNull()?.status == AssistantTurnStatus.COMPLETED
                }
                assertEquals(1, fixture.assistant.requests.get())
                assertEquals(QUESTION, fixture.assistant.lastQuestion)
                onNodeWithTag("nano_composer")
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
                capture("desktop-numpad-send-cleared")
            }
        }

    @Test
    fun acceptanceClearsTheCurrentEditorAfterLoadingAndConversationSwitch() =
        runDesktopComposeUiTest(width = 1100, height = 820) {
            val memory = InMemoryConversationStore()
            val opened = CompletableDeferred<Unit>()
            val conversationStore = object : ConversationStore by memory {
                override suspend fun open() { opened.await() }
            }
            Fixture(conversationStore = conversationStore).use { fixture ->
                setContent { fixture.Screen() }
                onNodeWithTag("nano_composer").assertDoesNotExist()
                opened.complete(Unit)
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                repeat(2) { index ->
                    onNodeWithTag("nano_composer").performTextReplacement("Question $index")
                    onNodeWithTag("nano_send").performClick()
                    waitUntil(timeoutMillis = 5_000) {
                        fixture.repository.state.value.selected.turns.lastOrNull()?.status == AssistantTurnStatus.COMPLETED
                    }
                    onNodeWithTag("nano_composer")
                        .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
                    assertEquals("", fixture.repository.state.value.selected.draft)
                    if (index == 0) {
                        val previous = fixture.repository.state.value.selected.id
                        onNodeWithContentDescription(fixture.newConversationLabel).performClick()
                        waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.session.id != previous }
                    }
                }
                assertEquals(2, fixture.assistant.requests.get())
                capture("desktop-button-send-cleared")
            }
        }

    @Test
    fun delayedAcceptanceKeepsNewInputAndSelection() =
        runDesktopComposeUiTest(width = 390, height = 844) {
            val memory = InMemoryConversationStore()
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val conversationStore = object : ConversationStore by memory {
                override suspend fun commit(
                    metadata: AssistantSessionState?,
                    turn: AssistantSessionTurn?,
                    workspace: ConversationWorkspace?,
                ) {
                    if (turn?.status == AssistantTurnStatus.RUNNING) {
                        entered.complete(Unit)
                        release.await()
                    }
                    memory.commit(metadata = metadata, turn = turn, workspace = workspace)
                }
            }
            Fixture(conversationStore = conversationStore).use { fixture ->
                try {
                    setContent { fixture.Screen() }
                    waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                    onNodeWithTag("nano_composer").performTextReplacement(QUESTION)
                    onNodeWithTag("nano_send").performClick()
                    waitUntil(timeoutMillis = 5_000) { entered.isCompleted }
                    onNodeWithTag("nano_composer").performTextReplacement("Next question")
                    onNodeWithTag("nano_composer").performTextInputSelection(TextRange(2, 5))
                    release.complete(Unit)
                    waitUntil(timeoutMillis = 5_000) {
                        fixture.repository.state.value.selected.turns.lastOrNull()?.status == AssistantTurnStatus.COMPLETED
                    }
                    onNodeWithTag("nano_composer")
                        .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Next question")))
                        .assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(2, 5)))
                    assertEquals(1, fixture.assistant.requests.get())
                    capture("phone-accepted-send-keeps-new-input")
                } finally {
                    release.complete(Unit)
                }
            }
        }

    @Test
    fun waitingCanBeStoppedAndNewConversationClearsTheSession() =
        runDesktopComposeUiTest(width = 390, height = 844) {
            Fixture(mode = Mode.WAITING).use { fixture ->
                setContent { fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.profiles?.active?.configured == true }
                onNodeWithTag("nano_composer").performTextReplacement(QUESTION)
                onNodeWithTag("nano_send").assertIsEnabled().performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.assistant.requests.get() == 1 && fixture.repository.state.value.selected.isRunning }
                onNodeWithTag("nano_stop").assertIsDisplayed().assertIsEnabled()
                capture("phone-searching")
                assertComposerFits(390, 844, action = "nano_stop")
                onNodeWithTag("nano_stop").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    fixture.assistant.cancelled.get() &&
                        fixture.repository.state.value.selected.turns.lastOrNull()?.status == AssistantTurnStatus.STOPPED
                }
                capture("phone-stopped")
                onNodeWithTag("nano_send").assertIsNotEnabled()
                onNodeWithContentDescription(fixture.newConversationLabel).performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.turns.isEmpty() }
                assertEquals("", fixture.repository.state.value.selected.draft)
                assertEquals(1, fixture.assistant.requests.get())
                capture("phone-new-conversation")
            }
        }

    @Test
    fun failedRequestCanRetryAndNavigationCallbacksRemainConnected() =
        runDesktopComposeUiTest(width = 1100, height = 820) {
            Fixture(mode = Mode.FAIL_FIRST).use { fixture ->
                setContent { fixture.Screen(dark = true) }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.profiles?.active?.configured == true }
                onNodeWithTag("nano_composer").performTextReplacement(QUESTION)
                onNodeWithTag("nano_send").performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.turns.lastOrNull()?.status == AssistantTurnStatus.FAILED }
                assertEquals(AssistantError.NETWORK, fixture.repository.state.value.selected.turns.single().error)
                val conversationId = fixture.repository.state.value.selected.id
                onNodeWithTag("nano_history_$conversationId")
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, fixture.historyFailedLabel))
                onNodeWithText(fixture.historyFailedLabel).assertDoesNotExist()
                capture("desktop-error-dark")
                onNodeWithTag("nano_messages").performScrollToNode(hasText(fixture.retryLabel))
                onNodeWithText(fixture.retryLabel).assertIsEnabled().performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.turns.lastOrNull()?.status == AssistantTurnStatus.COMPLETED }
                assertEquals(2, fixture.assistant.requests.get())
                assertEquals(1, fixture.repository.state.value.selected.turns.size)
                onNodeWithTag("nano_history_$conversationId")
                    .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
                onNodeWithTag("nano_history_status_$conversationId", useUnmergedTree = true).assertDoesNotExist()
                capture("desktop-retried-dark")
                onNodeWithContentDescription(fixture.newConversationLabel).performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.turns.isEmpty() }
                onNodeWithTag("nano_settings").assertIsDisplayed().performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.settingsOpened == 1 }
                assertEquals(1, fixture.settingsOpened)
                onNodeWithTag("nano_more").assertDoesNotExist()
                onNodeWithText(fixture.searchLabel).assertDoesNotExist()
                assertEquals(0, fixture.searchOpened)
            }
        }

    @Test
    fun longStreamingReplyFollowsTheTailAndLatestReturnsToTheEnd() =
        runDesktopComposeUiTest(width = 390, height = 844) {
            Fixture(mode = Mode.LONG_REPLY).use { fixture ->
                setContent { fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.profiles?.active?.configured == true }
                onNodeWithTag("nano_composer").performTextReplacement(QUESTION)
                onNodeWithTag("nano_send").performClick()
                repeat(3) { chunk ->
                    fixture.assistant.nextChunk.trySend(Unit)
                    waitUntil(timeoutMillis = 5_000) {
                        fixture.repository.state.value.selected.turns.lastOrNull()?.reply?.length ==
                            LONG_REPLY_CHUNK.length * (chunk + 1)
                    }
                    waitForIdle()
                    onNodeWithTag("nano_conversation_end").assertIsDisplayed()
                }
                onNodeWithTag("nano_messages").performTouchInput { swipeDown() }
                onNodeWithTag("nano_messages").performScrollToIndex(0)
                val questionBounds = onNodeWithText(QUESTION).getUnclippedBoundsInRoot()
                onNodeWithTag("nano_history_open").performClick()
                onNodeWithTag("nano_history_screen").assertIsDisplayed()
                onNode(hasClickAction() and hasAnyAncestor(hasTestTag("nano_history_back"))).performClick()
                assertEquals(questionBounds, onNodeWithText(QUESTION).getUnclippedBoundsInRoot())
                onNodeWithText(fixture.latestLabel).assertIsDisplayed().performClick()
                waitForIdle()
                onNodeWithTag("nano_conversation_end").assertIsDisplayed()
                capture("phone-long-reply")
            }
        }

    @Test
    fun streamedReplyStaysInViewWhenRecommendationsArriveFirst() =
        runDesktopComposeUiTest(width = 390, height = 600) {
            val question = "Find subscriptions about music"
            val opening = "I found these subscriptions in your library."
            val ending = "Start with the first recommendation."
            var turn by mutableStateOf(AssistantSessionTurn(id = 1, question = question))
            var followLatest by mutableStateOf(true)
            val session = AssistantSessionState()
            var latestLabel = ""
            setContent {
                CompositionLocalProvider(LocalCapabilityUis provides listOf(FeedSearchUi)) {
                    MaterialTheme {
                        latestLabel = stringResource(Res.string.nano_latest)
                        NanoMessageList(
                            uiState = NanoState.initial(
                                conversations = AssistantConversationsState(
                                    selected = session.copy(turns = listOf(turn)),
                                ),
                                available = true,
                            ),
                            listState = rememberLazyListState(),
                            followLatest = followLatest,
                            onFollowLatest = { followLatest = it },
                            keyboardVisible = false,
                            onHideKeyboard = {},
                            formatDate = { "" },
                            dispatch = {},
                        )
                    }
                }
            }

            val questionNode = onNodeWithText(question).assertIsDisplayed()
            val questionTop = questionNode.getUnclippedBoundsInRoot().top
            runOnIdle {
                turn = turn.copy(content = FeedSearchContent(
                    feeds = List(8) { index ->
                        RecommendedFeed(
                            feed = AssistantFeed(
                                feedId = "feed-$index",
                                title = "Music feed $index",
                                description = "",
                                muted = false,
                            ),
                            reason = "A discussion about music across generations.",
                            feedUrl = "https://example.test/feeds/$index",
                        )
                    },
                    hadCandidates = true,
                ))
            }
            questionNode.assertIsDisplayed()
            assertEquals(questionTop, questionNode.getUnclippedBoundsInRoot().top)
            runOnIdle { turn = turn.copy(reply = "**$opening**") }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodes(hasText(opening)).fetchSemanticsNodes().size == 1
            }
            onNodeWithText(opening).assertIsDisplayed()

            runOnIdle { turn = turn.copy(reply = "${turn.reply}\n\n${LONG_REPLY_CHUNK}\n\n**$ending**") }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodes(hasText(ending)).fetchSemanticsNodes().size == 1
            }
            val endingNode = onNodeWithText(ending).assertIsDisplayed()
            val latest = onNodeWithText(latestLabel).assertIsDisplayed()
            assertTrue(endingNode.getUnclippedBoundsInRoot().bottom <= latest.getUnclippedBoundsInRoot().top)

            runOnIdle { turn = turn.copy(status = AssistantTurnStatus.COMPLETED) }
            endingNode.assertIsDisplayed()
            latest.performClick()
            onNodeWithTag("nano_conversation_end").assertIsDisplayed()
        }

    @Test
    fun scrollingUpDuringMarkdownStreamingKeepsTheReadingPosition() =
        runDesktopComposeUiTest(width = 390, height = 844) {
            Fixture(mode = Mode.LONG_REPLY).use { fixture ->
                setContent { fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.profiles?.active?.configured == true }
                onNodeWithTag("nano_composer").performTextReplacement("**用户消息**")
                onNodeWithTag("nano_send").performClick()
                repeat(2) { chunk ->
                    fixture.assistant.nextChunk.trySend(Unit)
                    waitUntil(timeoutMillis = 5_000) {
                        onAllNodes(hasText("检索说明")).fetchSemanticsNodes().size == chunk + 1
                    }
                    waitForIdle()
                    onNodeWithTag("nano_conversation_end").assertIsDisplayed()
                }
                onNodeWithTag("nano_messages").performTouchInput { swipeDown() }
                onNodeWithTag("nano_messages").performScrollToIndex(0)
                waitUntil { onAllNodes(hasText("用户消息")).fetchSemanticsNodes().size == 1 }
                val position = onNodeWithText("用户消息").getUnclippedBoundsInRoot()
                fixture.assistant.nextChunk.trySend(Unit)
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodes(hasText("检索说明")).fetchSemanticsNodes().size == 3
                }
                assertEquals(position, onNodeWithText("用户消息").getUnclippedBoundsInRoot())
                assertEquals("**用户消息**", fixture.repository.state.value.selected.turns.single().question)
                onNodeWithText(fixture.latestLabel).performClick()
                waitForIdle()
                onNodeWithTag("nano_conversation_end").assertIsDisplayed()
                capture("phone-markdown-follow-latest")
            }
        }

    @Test
    fun unsupportedCapabilityUsesFallbackProgressAndKeepsReplyAndStopWorking() =
        runDesktopComposeUiTest(width = 1100, height = 820) {
            Fixture(mode = Mode.OTHER_CAPABILITY).use { fixture ->
                setContent { fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                onNodeWithTag("nano_composer").performTextReplacement("Synthetic request")
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.draft == "Synthetic request" }
                onNodeWithTag("nano_send").assertIsEnabled().performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.turns.lastOrNull()?.progress == SampleProgress }
                onNodeWithText(fixture.preparingLabel).assertIsDisplayed()
                fixture.assistant.nextChunk.trySend(Unit)
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.turns.lastOrNull()?.content is SampleContent }
                onNodeWithText("Synthetic capability result", useUnmergedTree = true).assertIsDisplayed()
                onNodeWithTag("nano_messages").performScrollToNode(hasText("Synthetic reply"))
                onNodeWithText("Synthetic reply").assertIsDisplayed()
                capture("desktop-unknown-capability-streaming")
                assertEquals(AssistantTurnStatus.RUNNING, fixture.repository.state.value.selected.turns.single().status)
                assertFalse(fixture.assistant.cancelled.get())
                onNodeWithTag("nano_stop").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    fixture.assistant.cancelled.get() &&
                            fixture.repository.state.value.selected.turns.single().status == AssistantTurnStatus.STOPPED
                }
                assertEquals(1, fixture.assistant.requests.get())
            }
        }

    @Test
    fun backgroundAndDisposedScreenKeepGeneratingAndReentryShowsTheCompletedReply() =
        runDesktopComposeUiTest(width = 1100, height = 820) {
            Fixture(mode = Mode.BACKGROUND_REPLY).use { fixture ->
                var visible by mutableStateOf(true)
                setContent { if (visible) fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                onNodeWithTag("nano_composer").performTextReplacement(QUESTION)
                onNodeWithTag("nano_send").performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.turns.lastOrNull()?.reply == FIRST_DELTA }
                onNodeWithTag("nano_composer").performTextReplacement("Next question")
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.draft == "Next question" }
                onNodeWithTag("nano_settings").assertIsDisplayed().performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.settingsOpened == 1 }
                assertTrue(fixture.repository.state.value.selected.isRunning)

                // ON_STOP alone must not cancel a request while the screen is still composed.
                runOnIdle { fixture.lifecycle.currentState = Lifecycle.State.CREATED }
                fixture.assistant.nextChunk.trySend(Unit)
                waitUntil(timeoutMillis = 5_000) {
                    fixture.repository.state.value.selected.turns.single().reply == FIRST_DELTA + BACKGROUND_DELTA
                }
                assertTrue(fixture.repository.state.value.selected.isRunning)
                assertFalse(fixture.assistant.cancelled.get())

                // Dispose the screen and its VM, then complete the same repository-owned request.
                runOnIdle { visible = false }
                waitForIdle()
                onNodeWithTag("nano_composer").assertDoesNotExist()
                runOnIdle { fixture.recreateViewModel() }
                fixture.assistant.nextChunk.trySend(Unit)
                waitUntil(timeoutMillis = 5_000) {
                    fixture.repository.state.value.selected.turns.single().status == AssistantTurnStatus.COMPLETED
                }
                assertFalse(fixture.assistant.cancelled.get())

                runOnIdle {
                    fixture.lifecycle.currentState = Lifecycle.State.RESUMED
                    visible = true
                }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                onNodeWithTag("nano_composer")
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Next question")))
                onNodeWithTag("nano_messages").performScrollToNode(hasText(FIRST_DELTA + BACKGROUND_DELTA + RESULT_REPLY))
                onNodeWithText(FIRST_DELTA + BACKGROUND_DELTA + RESULT_REPLY).assertIsDisplayed()
                onNodeWithTag("nano_messages").performScrollToNode(hasText(FIRST_TITLE))
                onNodeWithText(FIRST_TITLE).assertIsDisplayed()
                assertEquals(1, fixture.assistant.requests.get())
            }
        }

    @Test
    fun phoneHistoryTopBarChangesColorOnScrollAndResetsAtTop() =
        runDesktopComposeUiTest(width = 390, height = 844) {
            Fixture().use { fixture ->
                setContent { fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                runBlocking {
                    repeat(20) { index ->
                        fixture.repository.updateDraft(
                            id = fixture.repository.state.value.selected.id,
                            text = "Conversation $index",
                            editorId = "history-scroll-test",
                            revision = index.toLong()
                        )
                        fixture.repository.newConversation()
                    }
                }
                onNodeWithTag("nano_history_open").performClick()
                val screen = onNodeWithTag("nano_history_screen")
                val initialColor = screen.captureToImage().toPixelMap()[380, 30]
                val list = onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("nano_history")))
                list.performTouchInput { swipeUp() }
                assertTrue(screen.captureToImage().toPixelMap()[380, 30] != initialColor)
                capture("phone-history-scrolled")
                repeat(4) { list.performTouchInput { swipeDown() } }
                assertEquals(initialColor, screen.captureToImage().toPixelMap()[380, 30])
                capture("phone-history-scroll-reset")
            }
        }

    @Test
    fun resizingHistoryPageUsesPersistentWideHistoryAndReturnsToNarrowChat() =
        runDesktopComposeUiTest(width = 1100, height = 844) {
            Fixture().use { fixture ->
                var width by mutableStateOf(390.dp)
                setContent {
                    Box(modifier = Modifier.width(width)) {
                        fixture.Screen(dark = true, fontScale = 1.3f)
                    }
                }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                onNodeWithTag("nano_composer").performTextReplacement("Saved draft")
                onNodeWithTag("nano_history_open").performClick()
                onNodeWithTag("nano_history_screen").assertIsDisplayed()
                capture("phone-history-dark-large-type")
                runOnIdle { width = 1100.dp }
                onNodeWithTag("nano_history_screen").assertDoesNotExist()
                onNodeWithTag("nano_history").assertIsDisplayed()
                onNodeWithTag("nano_history_open").assertDoesNotExist()
                onNodeWithTag("nano_composer")
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Saved draft")))
                runOnIdle { width = 390.dp }
                onNodeWithTag("nano_history").assertDoesNotExist()
                onNodeWithTag("nano_history_open").assertIsDisplayed()
                onNodeWithTag("nano_composer")
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Saved draft")))
            }
        }

    @Test
    fun phoneHistoryBackRestoresEditorAndKeepsGenerationRunning() =
        runDesktopComposeUiTest(width = 390, height = 844) {
            Fixture(mode = Mode.WAITING).use { fixture ->
                var pageBacks = 0
                val backInput = object : NavigationEventInput() {
                    fun start() = dispatchOnBackStarted(NavigationEvent())
                    fun cancel() = dispatchOnBackCancelled()
                    fun complete() = dispatchOnBackCompleted()
                }
                setContent {
                    val dispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current)
                        .navigationEventDispatcher
                    DisposableEffect(dispatcher) {
                        dispatcher.addInput(backInput)
                        onDispose { dispatcher.removeInput(backInput) }
                    }
                    NavigationBackHandler(
                        state = rememberNavigationEventState(NavigationEventInfo.None),
                        isBackEnabled = true,
                        onBackCompleted = { pageBacks++ }
                    )
                    fixture.Screen()
                }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                onNodeWithTag("nano_composer").performTextReplacement(QUESTION)
                onNodeWithTag("nano_send").performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.isRunning }
                onNodeWithTag("nano_composer").performTextReplacement("Next draft")
                onNodeWithTag("nano_composer").performTextInputSelection(TextRange(3))
                onNodeWithTag("nano_history_open").performClick()
                onNodeWithTag("nano_history_screen").assertIsDisplayed()
                runOnIdle {
                    backInput.start()
                    backInput.cancel()
                }
                onNodeWithTag("nano_history_screen").assertIsDisplayed()
                runOnIdle { backInput.complete() }
                onNodeWithTag("nano_history_screen").assertDoesNotExist()
                onNodeWithTag("nano_composer")
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Next draft")))
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(3)))
                assertEquals(0, pageBacks)
                assertTrue(fixture.repository.state.value.selected.isRunning)
                assertFalse(fixture.assistant.cancelled.get())

                onNodeWithTag("nano_history_open").performClick()
                onNode(hasClickAction() and hasAnyAncestor(hasTestTag("nano_history_back"))).performClick()
                onNodeWithTag("nano_composer").assertIsDisplayed()
                onNodeWithTag("nano_history_open").performClick()
                onNodeWithTag("nano_history_screen").performMouseInput {
                    click(button = MouseButton(3))
                }
                onNodeWithTag("nano_history_screen").assertDoesNotExist()
                onNodeWithTag("nano_composer")
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Next draft")))
                assertEquals(0, pageBacks)
                onNodeWithTag("nano_history_open").performClick()
                val previousId = fixture.repository.state.value.selected.id
                onNodeWithText(fixture.newConversationLabel).performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.id != previousId }
                onNodeWithTag("nano_history_screen").assertDoesNotExist()
                onNodeWithTag("nano_composer").assertIsDisplayed()
                assertFalse(fixture.assistant.cancelled.get())
            }
        }

    @Test
    fun desktopHistoryStaysVisibleAndRestoresCursorScrollAndDraftAndSupportsRenameDelete() =
        runDesktopComposeUiTest(width = 1100, height = 820) {
            Fixture().use { fixture ->
                setContent { fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                onNodeWithTag("nano_composer").performTextReplacement(QUESTION)
                onNodeWithTag("nano_send").performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.turns.lastOrNull()?.status == AssistantTurnStatus.COMPLETED }
                val a = fixture.repository.state.value.selected.id
                onNodeWithTag("nano_composer").performTextReplacement("acb")
                onNodeWithTag("nano_composer").performTextInputSelection(TextRange(2))
                onNodeWithTag("nano_messages").performScrollToIndex(0)
                val firstBounds = onNode(hasText(QUESTION) and hasAnyAncestor(hasTestTag("nano_messages"))).getUnclippedBoundsInRoot()
                onNodeWithContentDescription(fixture.newConversationLabel).performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.id != a }
                val b = fixture.repository.state.value.selected.id
                onNodeWithTag("nano_composer").performTextReplacement("B draft")
                onNodeWithTag("nano_history_$a").performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.id == a }
                onNodeWithTag("nano_composer")
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("acb")))
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(2)))
                assertEquals(firstBounds.top, onNode(hasText(QUESTION) and hasAnyAncestor(hasTestTag("nano_messages"))).getUnclippedBoundsInRoot().top)
                onNodeWithTag("nano_history").assertIsDisplayed()
                capture("desktop-history-restored")
                onNodeWithTag("nano_history_more_$a").performClick()
                onNodeWithText(fixture.renameLabel).performClick()
                onNodeWithTag("nano_rename_input").performTextReplacement("My saved chat")
                onNodeWithText(fixture.saveLabel).performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.title == "My saved chat" }
                onNodeWithTag("nano_history_more_$b").performClick()
                onNodeWithText(fixture.deleteLabel).performClick()
                capture("desktop-history-delete")
                onNodeWithTag("nano_delete_confirm").performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.summaries.none { it.id == b } }
                onNodeWithTag("nano_history").assertIsDisplayed()
                onNodeWithTag("nano_history_open").assertDoesNotExist()
                onNodeWithContentDescription(fixture.hideHistoryLabel).assertDoesNotExist()
                onNodeWithText("My saved chat").assertIsDisplayed()
                onNodeWithTag("nano_composer")
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("acb")))
            }
        }

    @Test
    fun phoneFullScreenHistoryReturnsAfterSelectionAndConfirmsStoppingDeletedConversation() =
        runDesktopComposeUiTest(width = 390, height = 844) {
            Fixture(mode = Mode.WAITING).use { fixture ->
                setContent { fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                onNodeWithTag("nano_composer").performTextReplacement("A question")
                onNodeWithTag("nano_send").performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.assistant.requests.get() == 1 }
                val a = fixture.repository.state.value.selected.id
                onNodeWithContentDescription(fixture.newConversationLabel).performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.id != a }
                onNodeWithTag("nano_composer").performTextReplacement("B draft")
                onNodeWithTag("nano_history").assertDoesNotExist()
                assertTrue(onNodeWithTag("nano_history_open").getUnclippedBoundsInRoot().left > 195.dp)
                capture("phone-history-entry")
                onNodeWithContentDescription(fixture.historyLabel).performClick()
                onNodeWithTag("nano_composer").assertDoesNotExist()
                val historyBounds = onNodeWithTag("nano_history_screen").getUnclippedBoundsInRoot()
                assertEquals(0.dp, historyBounds.left)
                assertEquals(390.dp, historyBounds.right)
                assertEquals(844.dp, historyBounds.bottom)
                onNodeWithTag("nano_history_$a")
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, fixture.historyReplyingLabel))
                onNodeWithTag("nano_history_status_$a", useUnmergedTree = true).assertIsDisplayed()
                onNodeWithText(fixture.historyReplyingLabel).assertDoesNotExist()
                capture("phone-history-full-screen")
                onNodeWithTag("nano_history_$a").performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.id == a }
                onNodeWithTag("nano_stop").assertIsDisplayed()
                assertFalse(fixture.assistant.cancelled.get())
                onNodeWithContentDescription(fixture.historyLabel).performClick()
                onNodeWithTag("nano_history_more_$a").performClick()
                onNodeWithText(fixture.deleteLabel).performClick()
                onNodeWithText(fixture.deleteStopLabel).assertIsDisplayed()
                capture("phone-history-delete-running")
                assertFalse(fixture.assistant.cancelled.get())
                onNodeWithTag("nano_delete_confirm").performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.assistant.cancelled.get() && fixture.repository.state.value.summaries.none { it.id == a } }
                assertEquals("B draft", fixture.repository.state.value.selected.draft)
            }
        }

    @Test
    fun selectingProfileUsesItInExistingConversationAndPreservesOtherDraft() =
        runDesktopComposeUiTest(width = 1100, height = 820) {
            Fixture().use { fixture ->
                setContent { fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.ready }
                onNodeWithTag("nano_composer").performTextReplacement("Original A")
                onNodeWithTag("nano_send").performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.runningIds.isEmpty() && fixture.assistant.requests.get() == 1 }
                val a = fixture.repository.state.value.selected.id
                onNodeWithContentDescription(fixture.newConversationLabel).performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.id != a }
                val b = fixture.repository.state.value.selected.id
                onNodeWithTag("nano_composer").performTextReplacement("B draft")
                onNodeWithTag("nano_history_$a").performClick()
                onNodeWithTag("nano_composer").performTextReplacement("Continue A")
                runBlocking {
                    fixture.settings.save(baseUrl = "https://other.example/v2", modelId = "other-model", apiKey = "synthetic-key")
                }
                onNodeWithTag("nano_profile_menu").performClick()
                onNodeWithText("other-model").performClick()
                runOnIdle {
                    val current = fixture.repository.state.value.selected
                    fixture.viewModel.viewModelScope.launch {
                        fixture.viewModel.processIntent(NanoIntent.Send(conversationId = a, editorId = current.editorId, revision = current.draftRevision))
                        fixture.viewModel.processIntent(NanoIntent.Select(b))
                    }
                }
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.id == b }
                waitUntil(timeoutMillis = 5_000) { fixture.assistant.requests.get() == 2 && fixture.repository.state.value.runningIds.isEmpty() }
                assertEquals(b, fixture.repository.state.value.selected.id)
                onNodeWithTag("nano_composer").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("B draft")))
            }
        }

    @Test
    fun jumpLatestFromMiddleWindowMustReachActualTail() =
        runDesktopComposeUiTest(width = 1100, height = 820) {
            val memory = InMemoryConversationStore()
            val metadata = AssistantSessionState(
                position = ConversationPosition(turnId = 72, itemKey = "72-question", offset = 15, followLatest = false),
            )
            runBlocking {
                repeat(120) { index ->
                    val turn = AssistantSessionTurn(id = index + 1L, question = "Jump question ${index + 1}", reply = "Jump response ${index + 1}", status = AssistantTurnStatus.COMPLETED)
                    memory.commit(metadata = metadata.copy(lastTurnId = turn.id, lastStatus = turn.status), turn = turn, workspace = ConversationWorkspace(selectedId = metadata.id))
                }
            }
            val windowEntered = CompletableDeferred<Unit>()
            val releaseWindow = CompletableDeferred<Unit>()
            var pauseWindow = false
            val store = object : ConversationStore by memory {
                override suspend fun window(id: kotlin.uuid.Uuid, anchor: Long?, limit: Int): ConversationWindow {
                    if (pauseWindow) {
                        windowEntered.complete(Unit)
                        releaseWindow.await()
                    }
                    return memory.window(id = id, anchor = anchor, limit = limit)
                }
            }
            Fixture(conversationStore = store).use { fixture ->
                setContent { fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.conversations.storage == ConversationStorageState.READY }
                onNodeWithText("Jump question 72").assertIsDisplayed()
                pauseWindow = true
                onNodeWithText(fixture.latestLabel).performClick()
                waitUntil(timeoutMillis = 5_000) { windowEntered.isCompleted }
                waitForIdle()
                releaseWindow.complete(Unit)
                waitUntil(timeoutMillis = 5_000) { !fixture.repository.state.value.selected.hasAfter }
                waitForIdle()
                capture("desktop-disk-jump-latest")
                onNodeWithTag("nano_conversation_end").assertIsDisplayed()
                assertTrue(fixture.repository.state.value.selected.position.followLatest)
            }
        }

    @Test
    fun diskHistoryRestoresAnchorSelectionAndShowsSaveFailureWithoutLosingInput() =
        runDesktopComposeUiTest(width = 1100, height = 820) {
            val directory = Files.createTempDirectory("nano-ui-disk").toFile()
            val path = directory.resolve("history.db").path
            val codec = ConversationRecordCodec(listOf(ArticleHistoryCodec, FeedHistoryCodec))
            val seed = RoomConversationStore(codec = codec, path = { path })
            val metadata = AssistantSessionState(
                title = "Restored long conversation",
                draft = "restored draft",
                selectionStart = 2,
                selectionEnd = 6,
                position = ConversationPosition(turnId = 72, itemKey = "72-question", offset = 15, followLatest = false),
            )
            runBlocking {
                seed.open()
                repeat(120) { index ->
                    val turn = AssistantSessionTurn(id = index + 1L, question = "Saved question ${index + 1}", reply = "Saved response ${index + 1}", status = AssistantTurnStatus.COMPLETED)
                    seed.commit(metadata = metadata.copy(lastTurnId = turn.id, lastStatus = turn.status), turn = turn, workspace = ConversationWorkspace(selectedId = metadata.id))
                }
                seed.close()
            }
            val disk = RoomConversationStore(codec = codec, path = { path })
            var failing = false
            val store = object : ConversationStore by disk {
                override suspend fun commit(metadata: AssistantSessionState?, turn: AssistantSessionTurn?, workspace: ConversationWorkspace?) {
                    check(!failing)
                    disk.commit(metadata = metadata, turn = turn, workspace = workspace)
                }
            }
            Fixture(conversationStore = store).use { fixture ->
                setContent { fixture.Screen() }
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.conversations.storage == ConversationStorageState.READY }
                onNodeWithText("Saved question 72").assertIsDisplayed()
                onNodeWithTag("nano_composer").assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(2, 6)))
                assertEquals(40, fixture.repository.state.value.selected.turns.size)
                assertEquals(0, fixture.assistant.requests.get())
                capture("desktop-disk-restored-anchor")
                val firstLoaded = fixture.repository.state.value.selected.turns.first().id
                onNodeWithTag("nano_messages").performScrollToIndex(0)
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.position.turnId == firstLoaded }
                val beforeBounds = onNodeWithText("Saved question $firstLoaded").getUnclippedBoundsInRoot()
                runBlocking { fixture.repository.loadWindow(id = metadata.id, anchor = firstLoaded - 10) }
                waitForIdle()
                assertEquals(beforeBounds, onNodeWithText("Saved question $firstLoaded").getUnclippedBoundsInRoot())
                assertTrue(fixture.repository.state.value.selected.turns.first().id < firstLoaded)
                assertEquals(40, fixture.repository.state.value.selected.turns.size)
                val nextFirst = fixture.repository.state.value.selected.turns.first().id
                onNodeWithTag("nano_messages").performScrollToIndex(0)
                onNodeWithTag("nano_messages").performTouchInput { swipeDown() }
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.turns.first().id < nextFirst }
                assertTrue(fixture.repository.state.value.selected.turns.size <= 40)
                capture("desktop-disk-window-prepended")
                val lastLoaded = fixture.repository.state.value.selected.turns.last().id
                onNodeWithTag("nano_messages").performScrollToIndex(fixture.repository.state.value.selected.turns.size * 2 - 10)
                onNodeWithTag("nano_messages").performTouchInput { swipeUp() }
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.selected.turns.last().id > lastLoaded }
                assertTrue(fixture.repository.state.value.selected.turns.size <= 40)
                capture("desktop-disk-window-appended")
                failing = true
                onNodeWithTag("nano_composer").performTextReplacement("keep this unsaved draft")
                waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.conversations.storage == ConversationStorageState.SAVE_FAILED }
                onNodeWithTag("nano_storage_notice", useUnmergedTree = true).assertIsDisplayed()
                onNodeWithTag("nano_send").assertIsNotEnabled()
                capture("desktop-disk-save-failed")
                failing = false
                onNodeWithText(fixture.retrySaveLabel).performClick()
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.storage == ConversationStorageState.READY }
                assertEquals("keep this unsaved draft", runBlocking { disk.metadata(metadata.id)!!.draft })
                assertEquals(0, fixture.assistant.requests.get())
                runBlocking { fixture.repository.flushPending() }
            }
            runBlocking { disk.close() }
            directory.deleteRecursively()
        }

    @Test
    fun disappearedCardAndMissingTurnFallBackToReadableNearbyQuestions() {
        for (missingTurn in listOf(false, true)) {
            runDesktopComposeUiTest(width = 390, height = 844) {
                val memory = InMemoryConversationStore()
                val metadata = AssistantSessionState(
                    draft = "kept draft",
                    position = ConversationPosition(turnId = 20, itemKey = "20-article-deleted-card", offset = 0, followLatest = false),
                )
                runBlocking {
                    for (id in 1L..60L) {
                        if (missingTurn && id == 20L) continue
                        val turn = AssistantSessionTurn(id = id, question = "Fallback question $id", status = AssistantTurnStatus.COMPLETED)
                        memory.commit(metadata = metadata.copy(lastTurnId = id, lastStatus = turn.status), turn = turn, workspace = ConversationWorkspace(selectedId = metadata.id))
                    }
                }
                Fixture(conversationStore = memory).use { fixture ->
                    setContent { fixture.Screen() }
                    waitUntil(timeoutMillis = 5_000) { fixture.viewModel.viewState.value.conversations.storage == ConversationStorageState.READY }
                    onNodeWithText("Fallback question ${if (missingTurn) 19 else 20}").assertIsDisplayed()
                    assertEquals(0, fixture.assistant.requests.get())
                    capture(if (missingTurn) "phone-missing-turn-fallback" else "phone-missing-card-fallback")
                }
            }
        }
    }

    @Test
    fun loadingAndOccupiedStorageHaveVisibleRecoveryStates() =
        runDesktopComposeUiTest(width = 390, height = 844) {
            val gate = CompletableDeferred<Unit>()
            val memory = InMemoryConversationStore()
            val store = object : ConversationStore by memory {
                override suspend fun open() {
                    gate.await()
                    throw ConversationStorageException(ConversationStorageState.OCCUPIED)
                }
            }
            Fixture(conversationStore = store).use { fixture ->
                setContent { fixture.Screen(dark = true, fontScale = 1.3f) }
                onNodeWithTag("nano_storage_notice", useUnmergedTree = true).assertIsDisplayed()
                capture("phone-storage-loading")
                gate.complete(Unit)
                waitUntil(timeoutMillis = 5_000) { fixture.repository.state.value.storage == ConversationStorageState.OCCUPIED }
                capture("phone-storage-occupied")
                assertEquals(0, fixture.assistant.requests.get())
            }
        }

    private fun DesktopComposeUiTest.capture(name: String) {
        val image = onNode(
            isRoot() and hasAnyDescendant(
                hasTestTag("nano_composer") or hasTestTag("nano_history_screen") or hasTestTag("nano_storage_notice")
            )
        ).captureToImage()
        File("build/reports/nano-ui/$name.png").apply {
            parentFile.mkdirs()
            writeBytes(Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData()!!.bytes)
        }
    }

    private fun DesktopComposeUiTest.assertComposerFits(width: Int, height: Int, action: String = "nano_send") {
        for (tag in listOf("nano_composer", action)) {
            val node = onNodeWithTag(tag).assertIsDisplayed()
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue(bounds.left.value >= 0 && bounds.top.value >= 0, "$tag starts outside the viewport: $bounds")
            assertTrue(bounds.right.value <= width + 1 && bounds.bottom.value <= height + 1,
                "$tag is clipped at ${width}x$height: $bounds")
            assertTrue(bounds.right.value - bounds.left.value >= if (tag == "nano_composer") 100f else 40f,
                "$tag is too narrow to operate: $bounds")
        }
    }

    @Serializable
    private data object TestRoute : NavKey

    private data class Layout(val name: String, val width: Int, val height: Int, val dark: Boolean = false, val fontScale: Float = 1f)
    private enum class Mode { FEEDS, RESULTS, WAITING, FAIL_FIRST, LONG_REPLY, OTHER_CAPABILITY, BACKGROUND_REPLY, DOWNLOAD }

    private data class SampleContent(val label: String) : AssistantContent {
        override val retainedBytes = label.encodeToByteArray().size
        override fun historySnapshot() = AssistantHistorySnapshot("synthetic", label, "{}")
    }

    private data object SampleProgress : AssistantProgress

    private class Fixture(
        configured: Boolean = true,
        mode: Mode = Mode.RESULTS,
        conversationStore: ConversationStore = InMemoryConversationStore(),
        downloads: IAssistantDownloadRepository? = null,
    ) : AutoCloseable {
        val assistant = FakeAssistant(mode)
        private val preferences = object : DataStore<Preferences> {
            override val data = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                transform(data.value).also { data.value = it }
        }
        val settings = AssistantSettingsRepository(preferences, ConversationCredentials())
        init {
            if (configured) runBlocking {
                settings.save("https://example.test/v1/", "preview-model", "synthetic-ui-key",
                    )
            }
        }
        val repository = AssistantConversationRepository(
            assistant = assistant,
            settings = settings,
            capabilities = listOfNotNull(downloads?.let { DownloadConversation(it) }),
            store = conversationStore,
        )
        var viewModel = NanoViewModel(repository, settings)
            private set
        private val store = ViewModelStore().apply { put("nano", viewModel) }
        private val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        val lifecycle: LifecycleRegistry get() = owner.lifecycle

        fun recreateViewModel() {
            store.clear()
            viewModel = NanoViewModel(repository, settings)
            store.put("nano", viewModel)
        }
        var settingsOpened = 0
        var searchOpened = 0
        var openedDownloads: OpenDownloads? = null
        var openDownloadsLabel = ""
        var downloadReceiptLabel = ""
        var downloadMissingLabel = ""
        var openedArticle: String? = null
        var openedFeed: String? = null
        var formattedDate: Long? = null
        var settingsLabel = ""
        var searchLabel = ""
        var newConversationLabel = ""
        var retryLabel = ""
        var latestLabel = ""
        var preparingLabel = ""
        var historyLabel = ""
        var historyReplyingLabel = ""
        var historyFailedLabel = ""
        var hideHistoryLabel = ""
        var renameLabel = ""
        var deleteLabel = ""
        var deleteStopLabel = ""
        var saveLabel = ""
        var retrySaveLabel = ""
        private val formatArticleDate: (Long) -> String = { millis ->
            formattedDate = millis
            FORMATTED_DATE
        }

        @Composable
        fun Screen(dark: Boolean = false, fontScale: Float = 1f) {
            val backStack = rememberNavBackStack(
                SavedStateConfiguration {
                    serializersModule = SerializersModule {
                        polymorphic(NavKey::class) { subclass(TestRoute::class, TestRoute.serializer()) }
                    }
                }, TestRoute,
            )
            CompositionLocalProvider(
                LocalCapabilityUis provides listOf(ArticleSearchUi, FeedSearchUi, DownloadUi),
                LocalLifecycleOwner provides owner,
                LocalDensity provides Density(1f, fontScale),
                LocalNavBackStack provides newNavBackStack(backStack, parent = null),
                LocalWindowController provides WindowController(onClose = {}),
            ) {
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    settingsLabel = stringResource(Res.string.nano_settings_title)
                    searchLabel = stringResource(Res.string.nano_open_search)
                    newConversationLabel = stringResource(Res.string.nano_new_conversation)
                    retryLabel = stringResource(Res.string.nano_retry)
                    latestLabel = stringResource(Res.string.nano_latest)
                    preparingLabel = stringResource(Res.string.nano_preparing)
                    historyLabel = stringResource(Res.string.nano_history)
                    historyReplyingLabel = stringResource(Res.string.nano_history_replying)
                    historyFailedLabel = stringResource(Res.string.nano_history_failed)
                    hideHistoryLabel = stringResource(Res.string.nano_history_hide)
                    renameLabel = stringResource(Res.string.nano_rename)
                    deleteLabel = stringResource(Res.string.nano_delete)
                    deleteStopLabel = stringResource(Res.string.nano_delete_stop)
                    saveLabel = stringResource(Res.string.nano_save)
                    retrySaveLabel = stringResource(Res.string.nano_retry_save)
                    openDownloadsLabel = stringResource(Res.string.nano_download_open)
                    downloadReceiptLabel = stringResource(Res.string.nano_download_result, 1, 0, 0)
                    downloadMissingLabel = stringResource(Res.string.nano_download_missing_attachment, "No enclosure")
                    Surface {
                        NanoScreen(
                            viewModel = viewModel,
                            formatDate = formatArticleDate,
                            onBack = {},
                            onSettings = { settingsOpened++ },
                            onAction = { _, action ->
                                if (action is ArticleSearchAction.OpenArticle) openedArticle = action.articleId
                                if (action is FeedSearchAction.OpenFeed) openedFeed = action.feedUrl
                                if (action is OpenDownloads) openedDownloads = action
                            },
                            onSearch = { searchOpened++ },
                            windowInsets = WindowInsets(0),
                        )
                    }
                }
            }
        }

        override fun close() {
            store.clear()
            runBlocking { repository.close().join() }
        }
    }

    private class FakeAssistant(private val mode: Mode) : AssistantRuntime {
        override val available = true
        val requests = AtomicInteger()
        val cancelled = AtomicBoolean()
        val nextChunk = Channel<Unit>(Channel.UNLIMITED)
        @Volatile var lastQuestion: String? = null
        override suspend fun verify(connection: suspend () -> AssistantConnection) = AssistantConnectionResult(true)
        override fun run(request: AssistantTurnRequest, connection: com.skyd.podaura.nano.core.AssistantConnection) = flow {
            lastQuestion = request.question
            val attempt = requests.incrementAndGet()
            if (mode == Mode.DOWNLOAD) {
                emit(AssistantRunEvent.Handoff(ValidatedDownloadPlan(
                    targets = listOf(AssistantDownloadTarget.DirectLink("https://example.test/episode.mp3")),
                    choices = listOf(DownloadChoice(
                        id = "version",
                        title = "Two versions",
                        options = listOf(
                            DownloadOption(
                                id = "1",
                                title = "MP3 · 64 kbps",
                                targets = listOf(AssistantDownloadTarget.Attachment(articleId = "two", url = "https://example.test/low.mp3")),
                            ),
                            DownloadOption(
                                id = "2",
                                title = "MP3 · 128 kbps",
                                targets = listOf(AssistantDownloadTarget.Attachment(articleId = "two", url = "https://example.test/high.mp3")),
                            ),
                        ),
                    )),
                    issues = listOf(DownloadIssue(title = "No enclosure", kind = DownloadIssueKind.MISSING_ATTACHMENT)),
                )))
                return@flow
            }
            if (mode == Mode.FEEDS) {
                emit(AssistantRunEvent.ContentReady(FeedSearchContent(listOf(RecommendedFeed(
                    AssistantFeed("feed-1", "音乐与生活", "聊聊音乐、城市与创作背后的故事。", true),
                    "节目主题符合你对音乐访谈的兴趣。", "https://example.test/music"
                )), hadCandidates = true)))
                emit(AssistantRunEvent.ReplyDelta("找到了一个已添加的音乐订阅。"))
                emit(AssistantRunEvent.Completed)
                return@flow
            }
            if (mode == Mode.BACKGROUND_REPLY) {
                try {
                    emit(AssistantRunEvent.ReplyDelta(FIRST_DELTA))
                    nextChunk.receive()
                    emit(AssistantRunEvent.ReplyDelta(BACKGROUND_DELTA))
                    nextChunk.receive()
                } catch (cancelled: CancellationException) {
                    this@FakeAssistant.cancelled.set(true)
                    throw cancelled
                }
            }
            if (mode == Mode.OTHER_CAPABILITY) {
                try {
                    emit(AssistantRunEvent.Progress(SampleProgress))
                    nextChunk.receive()
                    emit(AssistantRunEvent.ContentReady(SampleContent("Synthetic capability result")))
                    emit(AssistantRunEvent.ReplyDelta("Synthetic reply"))
                    awaitCancellation()
                } finally { cancelled.set(true) }
            }
            if (mode == Mode.LONG_REPLY) {
                emit(AssistantRunEvent.ContentReady(ArticleSearchContent.recommendations(ValidatedAssistantAnswer(
                    kind = AssistantAnswerKind.CLARIFICATION, text = "", articles = emptyList(), hadCandidates = false,
                ))))
                repeat(3) {
                    nextChunk.receive()
                    emit(AssistantRunEvent.ReplyDelta(LONG_REPLY_CHUNK))
                }
                emit(AssistantRunEvent.Completed)
                return@flow
            }
            if (mode == Mode.WAITING) {
                try {
                    emit(AssistantRunEvent.Progress(ArticleSearchProgress(ArticleSearchPhase.SEARCHING, candidateCount = 4)))
                    awaitCancellation()
                } finally { cancelled.set(true) }
            }
            if (mode == Mode.FAIL_FIRST && attempt == 1) {
                emit(AssistantRunEvent.Failed(AssistantError.NETWORK))
                return@flow
            }
            emit(AssistantRunEvent.ContentReady(ArticleSearchContent.recommendations(ValidatedAssistantAnswer(
                kind = AssistantAnswerKind.RESULTS, text = "", hadCandidates = true,
                articles = listOf(
                    RecommendedArticle(AssistantArticle("luo-1", FIRST_TITLE, "音乐与生活", dateMillis = ARTICLE_DATE, snippet =
                        "从《鹿港小镇》到《恋曲》，聊聊罗大佑如何把城市的变化、日常生活和一代人的记忆写进歌里。"),
                        "围绕罗大佑的创作展开，适合从歌曲背后的故事开始。"),
                    RecommendedArticle(AssistantArticle("luo-2", "当民谣遇见城市：重听《之乎者也》", "耳边来信", snippet =
                        "一场关于华语音乐的对谈。重听熟悉的旋律，回到那些改变创作方式的时刻。"),
                        "从专辑切入，补充了解作品与时代的关系。"),
                ),
            ))))
            emit(AssistantRunEvent.ReplyDelta(RESULT_REPLY))
            emit(AssistantRunEvent.Completed)
        }
    }

    private companion object {
        const val QUESTION = "找一些罗大佑的相关文章，想了解他的创作故事"
        const val FIRST_TITLE = "罗大佑：用一首歌记住一座城"
        const val ARTICLE_DATE = 1_700_000_000_000L
        const val FORMATTED_DATE = "Formatted date"
        val LONG_REPLY_CHUNK = "### 检索说明\n\n" + "- 请进一步说明希望检索的 **内容**。\n".repeat(15) + "\n"
        const val FIRST_DELTA = "First part. "
        const val BACKGROUND_DELTA = "Continued in background. "
        const val RESULT_REPLY = "找到两篇与罗大佑相关的内容。一篇聊创作与城市记忆，另一篇从专辑出发，你可以挑感兴趣的继续读。"
    }
}

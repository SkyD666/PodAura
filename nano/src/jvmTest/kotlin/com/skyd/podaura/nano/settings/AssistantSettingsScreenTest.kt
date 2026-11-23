package com.skyd.podaura.nano.settings

import kotlinx.coroutines.runBlocking
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.PlatformWindowInsetsProviderNode
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.component.navigation.newNavBackStack
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skyd.compone.local.LocalWindowController
import com.skyd.compone.local.WindowController
import com.skyd.fundation.credential.CredentialStore
import com.skyd.podaura.nano.core.AssistantReasoningEffort
import com.skyd.podaura.nano.core.AssistantConnection
import com.skyd.podaura.nano.core.AssistantConnectionResult
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantRunEvent
import com.skyd.podaura.nano.core.AssistantRuntime
import com.skyd.podaura.nano.core.AssistantTurnRequest
import com.skyd.nano.resources.Res
import com.skyd.nano.resources.nano_edit_named_profile
import com.skyd.nano.resources.nano_save_and_use
import com.skyd.nano.resources.nano_custom_provider
import com.skyd.nano.resources.nano_manual_model
import com.skyd.nano.resources.nano_reload
import com.skyd.nano.resources.nano_create_and_use
import com.skyd.nano.resources.nano_test_saved
import com.skyd.nano.resources.nano_settings_info
import com.skyd.nano.resources.nano_settings_description
import com.skyd.nano.resources.nano_ok
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.skia.Image
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, InternalComposeUiApi::class)
class AssistantSettingsScreenTest {
    @Test
    fun reasoningChoiceShowsFiveLevelsAndClearsWhenTheModelBecomesUnsupported() {
        for (layout in listOf(
            Layout(name = "effort-wide", width = 1100, height = 820),
            Layout(name = "effort-narrow", width = 390, height = 844, dark = true, fontScale = 1.5f),
        )) {
            runDesktopComposeUiTest(width = layout.width, height = layout.height) {
                Fixture().use { fixture ->
                    setContent { fixture.Screen(layout) }
                    waitUntil { fixture.model.viewState.value.loaded }
                    onNodeWithTag("nano_add_profile").performClick()
                    onNodeWithTag("nano_provider").performClick()
                    onNodeWithText("OpenAI").performClick()
                    onNodeWithTag("nano_settings_form").performScrollToNode(hasTestTag("nano_reasoning_effort"))
                    onNodeWithTag("nano_reasoning_effort").performClick()
                    onNodeWithText("none").assertDoesNotExist()
                    for (id in listOf("medium", "high", "xhigh", "max")) {
                        onNodeWithText(id).assertIsDisplayed()
                    }
                    capture(name = "${layout.name}-menu", dialogTitle = "max")
                    onNodeWithText("max").performClick()
                    waitUntil { fixture.model.viewState.value.reasoningEffort == AssistantReasoningEffort.MAX }
                    capture("${layout.name}-selected")
                    onNodeWithTag("nano_settings_form").performScrollToNode(hasTestTag("nano_model_choice"))
                    onNodeWithTag("nano_model_choice").performClick()
                    onNodeWithText(fixture.manual).performClick()
                    onNodeWithTag("nano_model_input").performTextReplacement("unknown-model")
                    onNodeWithTag("nano_reasoning_effort").assertDoesNotExist()
                    waitUntil { fixture.model.viewState.value.reasoningEffortReset }
                    capture("${layout.name}-reset")
                    onNodeWithTag("nano_model_input").performTextReplacement("gpt-6.1-terra")
                    waitUntil { fixture.model.viewState.value.supportsReasoningEffort }
                    assertEquals(AssistantReasoningEffort.LOW, fixture.model.viewState.value.reasoningEffort)
                }
            }
        }
    }

    @Test
    fun providerFormsRenderAcrossWidthsThemesAndLargeText() {
        for (layout in listOf(
            Layout(name = "wide-light", width = 1100, height = 820),
            Layout(name = "wide-dark", width = 1100, height = 820, dark = true),
            Layout(name = "narrow-light", width = 390, height = 844),
            Layout(name = "narrow-dark-large", width = 390, height = 844, dark = true, fontScale = 1.5f),
        )) {
            runDesktopComposeUiTest(width = layout.width, height = layout.height) {
                Fixture().use { fixture ->
                    setContent { fixture.Screen(layout) }
                    waitUntil { fixture.model.viewState.value.loaded }
                    capture("${layout.name}-empty")
                    onNodeWithTag("nano_key").assertDoesNotExist()
                    onNodeWithTag("nano_add_profile").performClick()
                    onNodeWithTag("nano_provider").performClick()
                    capture(name = "${layout.name}-provider-menu", dialogTitle = "Claude")
                    onNodeWithText("Claude").performClick()
                    waitUntil { fixture.model.viewState.value.providerId == "claude" }
                    onNodeWithTag("nano_address").assertDoesNotExist()
                    onNodeWithTag("nano_protocol").assertDoesNotExist()
                    onNodeWithTag("nano_key").assertIsDisplayed()
                    capture("${layout.name}-official")
                    onNodeWithTag("nano_model_choice").performClick()
                    onNodeWithText(fixture.manual).performClick()
                    onNodeWithTag("nano_model_input").performTextReplacement("manual-model")
                    capture("${layout.name}-manual")
                    onNodeWithTag("nano_provider").performClick()
                    onNodeWithText(fixture.custom).performClick()
                    waitUntil { fixture.model.viewState.value.custom }
                    onNodeWithTag("nano_address").assertIsDisplayed()
                    onNodeWithTag("nano_protocol").assertIsDisplayed()
                    capture("${layout.name}-custom")
                    onNodeWithTag("nano_settings_form").performScrollToNode(hasText(fixture.save))
                    onNodeWithText(fixture.save).assertIsDisplayed()
                    capture("${layout.name}-bottom")
                }
            }
        }
    }

    @Test
    fun tappingARowSelectsItsSavedKeyAndTappingThePencilEditsIndependently() = runDesktopComposeUiTest(width = 600, height = 880) {
        Fixture().use { fixture ->
            runBlocking {
                fixture.repository.save(baseUrl = "https://work.example", modelId = "work", apiKey = "work")
                fixture.repository.save(baseUrl = "https://home.example", modelId = "home", apiKey = "home")
            }
            setContent { fixture.Screen(Layout(name = "profiles", width = 600, height = 880)) }
            waitUntil { fixture.model.viewState.value.loaded }
            capture("saved-profiles")
            onNodeWithText("work").assertIsSelected()
            onNodeWithText("home").assertIsNotSelected().performTouchInput { click() }
            waitUntil { fixture.model.viewState.value.profiles.active.name == "home" }
            onNodeWithText("home").assertIsSelected()
            onNodeWithText("work").assertIsNotSelected()
            assertEquals("home", runBlocking { fixture.repository.connection().apiKey })
            editButton("work").performTouchInput { click() }
            onNodeWithTag("nano_key").assertExists()
            assertEquals("work", fixture.model.viewState.value.saved.name)
            assertEquals("home", runBlocking { fixture.repository.connection().apiKey })
            onNodeWithTag("nano_key").performTextReplacement("replacement")
            onNodeWithTag("nano_settings_form").performScrollToNode(hasText(fixture.use))
            onNodeWithText(fixture.use).performClick()
            waitUntil { fixture.model.viewState.value.profiles.active.name == "work" }
            assertEquals("replacement", runBlocking { fixture.repository.connection().apiKey })
        }
    }

    @Test
    fun addingAndCancellingSlideTheEditorAndListInFromOppositeSides() =
        runDesktopComposeUiTest(width = 600, height = 880) {
            Fixture().use { fixture ->
                setContent { fixture.Screen(Layout(name = "transition", width = 600, height = 880)) }
                val list = onNodeWithTag("nano_profiles_list")
                val editor = onNodeWithTag("nano_settings_form")
                val bounds = list.getUnclippedBoundsInRoot()
                mainClock.autoAdvance = false
                onNodeWithTag("nano_add_profile").performClick()
                waitUntil { fixture.model.viewState.value.editing }
                mainClock.advanceTimeBy(96)
                val enteringEditor = editor.getUnclippedBoundsInRoot()
                assertTrue(enteringEditor.left > bounds.left && enteringEditor.left < bounds.right)
                mainClock.autoAdvance = true
                list.assertDoesNotExist()
                assertEquals(bounds.left, editor.getUnclippedBoundsInRoot().left)
                onNodeWithTag("nano_provider").performClick()
                onNodeWithText("Claude").performClick()
                editor.performScrollToNode(hasTestTag("nano_cancel_editor"))
                mainClock.autoAdvance = false
                onNodeWithTag("nano_cancel_editor").performClick()
                waitUntil { !fixture.model.viewState.value.editing }
                mainClock.advanceTimeBy(96)
                val returningList = list.getUnclippedBoundsInRoot()
                assertTrue(returningList.left < bounds.left && returningList.right > bounds.left)
                mainClock.autoAdvance = true
                editor.assertDoesNotExist()
                assertEquals(bounds.left, list.getUnclippedBoundsInRoot().left)
            }
        }

    @Test
    fun cancelDiscardsAnEditAndAddingABackupKeepsTheActiveProfile() =
        runDesktopComposeUiTest(width = 500, height = 840) {
            Fixture().use { fixture ->
                val work = runBlocking {
                    fixture.repository.save(
                        baseUrl = "https://work.example",
                        modelId = "work-model",
                        apiKey = "work-key",
                        name = "Work",
                    )
                }
                setContent { fixture.Screen(Layout(name = "add-backup", width = 500, height = 840)) }
                editButton("Work").performClick()
                onNodeWithTag("nano_profile_name").performTextReplacement("Unsaved")
                onNodeWithTag("nano_key").performTextReplacement("unsaved-key")
                onNodeWithTag("nano_settings_form").performScrollToNode(hasTestTag("nano_cancel_editor"))
                onNodeWithTag("nano_cancel_editor").performClick()
                onNodeWithText("Work").assertIsDisplayed()
                onNodeWithTag("nano_add_profile").performClick()
                onNodeWithTag("nano_key").assertDoesNotExist()
                onNodeWithTag("nano_provider").performClick()
                onNodeWithText("Claude").performClick()
                onNodeWithTag("nano_profile_name").performTextReplacement("Personal")
                onNodeWithTag("nano_key").performTextReplacement("personal-key")
                onNodeWithTag("nano_settings_form").performScrollToNode(hasTestTag("nano_save_profile"))
                onNodeWithTag("nano_save_profile").performClick()
                onNodeWithText("Personal").assertIsDisplayed()
                onNodeWithText("Work").assertIsDisplayed()
                onNodeWithTag("nano_provider").assertDoesNotExist()
                assertEquals(work, runBlocking { fixture.repository.snapshot() })
                assertEquals("work-key", runBlocking { fixture.repository.connection().apiKey })
            }
        }

    @Test
    fun providerInfoShowsTheActualEndpointForEveryPreset() {
        val endpoints = listOf(
            "OpenAI" to "https://api.openai.com/v1/responses",
            "DeepSeek" to "https://api.deepseek.com/chat/completions",
            "Claude" to "https://api.anthropic.com/v1/messages",
            "Gemini" to "https://generativelanguage.googleapis.com/v1beta/models/gemini-4-hydrogen:generateContent",
            "Grok" to "https://api.x.ai/v1/chat/completions",
        )
        for ((provider, endpoint) in endpoints) {
            runDesktopComposeUiTest(width = 450, height = 850) {
                Fixture().use { fixture ->
                    setContent { fixture.Screen(Layout(name = "info", width = 450, height = 850)) }
                    waitUntil { fixture.model.viewState.value.loaded }
                    onNodeWithTag("nano_add_profile").performClick()
                    onNodeWithTag("nano_provider").performClick()
                    onNodeWithText(provider).performClick()
                    onNodeWithContentDescription(fixture.info).performClick()
                    onNodeWithText(fixture.description).assertIsDisplayed()
                    onNodeWithText(text = endpoint, substring = true).assertIsDisplayed()
                    capture(name = "info-${provider.lowercase()}", dialogTitle = fixture.info)
                    onNodeWithText(fixture.ok).performClick()
                    assertEquals(provider, AssistantProviderPresets.find(fixture.model.viewState.value.providerId)?.displayName)
                }
            }
        }
    }

    @Test
    fun keyboardInsetsKeepTheFocusedInputAndBottomActionsReachable() {
        runDesktopComposeUiTest(width = 390, height = 844) {
            Fixture().use { fixture ->
                var keyboardHeight by mutableStateOf(0)
                val insets = object : PlatformWindowInsets {
                    override val ime get() = PlatformInsets(bottom = keyboardHeight)
                }
                setContent {
                    CompositionLocalProvider(LocalPlatformWindowInsets provides insets) {
                        Box(modifier = InsetsElement(insets)) {
                            fixture.Screen(Layout(name = "keyboard", width = 390, height = 844))
                        }
                    }
                }
                waitUntil { fixture.model.viewState.value.loaded }
                onNodeWithTag("nano_add_profile").performClick()
                onNodeWithTag("nano_provider").performClick()
                onNodeWithText(fixture.custom).performClick()
                onNodeWithTag("nano_protocol").performClick()
                onNode(hasText("OpenAI compatible") and isSelected()).assertIsSelected()
                onNode(hasText("OpenAI compatible") and isSelected()).performClick()
                onNodeWithTag("nano_key").performClick()
                onNodeWithTag("nano_key").performTextReplacement("synthetic-key")
                runOnIdle { keyboardHeight = 300 }
                onNodeWithTag("nano_settings_form").performScrollToNode(hasTestTag("nano_key"))
                onNodeWithTag("nano_key").assertIsFocused()
                onNodeWithTag("nano_key").assertIsDisplayed()
                assertTrue(onNodeWithTag("nano_key").getUnclippedBoundsInRoot().bottom.value <= 544)
                capture("keyboard-key")
                onNodeWithTag("nano_key").performKeyInput { pressKey(Key.Tab) }
                onNodeWithTag("nano_key").assertIsNotFocused()
                onNodeWithTag("nano_settings_form").performScrollToNode(hasText(fixture.save))
                onNodeWithText(fixture.save).assertIsDisplayed()
                onNodeWithText(fixture.verify).assertDoesNotExist()
                assertTrue(onNodeWithText(fixture.save).getUnclippedBoundsInRoot().bottom.value <= 544)
                capture("keyboard-bottom")
                assertTrue(fixture.model.viewState.value.hasNewKey)
            }
        }
    }

    @Test
    fun realFormKeepsInputOnSaveFailureAndShowsVerificationFailure() {
        runDesktopComposeUiTest(width = 450, height = 850) {
            Fixture().use { fixture ->
                setContent { fixture.Screen(Layout(name = "failure", width = 450, height = 850)) }
                waitUntil { fixture.model.viewState.value.loaded }
                onNodeWithTag("nano_add_profile").performClick()
                onNodeWithTag("nano_provider").performClick()
                onNodeWithText("OpenAI").performClick()
                onNodeWithTag("nano_key").performTextReplacement("synthetic-ui-key")
                onNodeWithTag("nano_key").performKeyInput { pressKey(Key.Tab) }
                onNodeWithTag("nano_settings_form").performScrollToNode(hasText(fixture.save))
                fixture.preferences.failWrite = true
                onNodeWithText(fixture.save).performClick()
                waitUntil { fixture.model.viewState.value.failure != null }
                assertTrue(fixture.model.viewState.value.hasNewKey)
                assertFalse(fixture.model.viewState.value.saved.configured)
                capture("save-failed")
                fixture.preferences.failWrite = false
                onNodeWithText(fixture.save).performClick()
                waitUntil { fixture.model.viewState.value.saved.configured && fixture.model.viewState.value.operation == null }
                assertFalse(fixture.model.viewState.value.hasNewKey)
                onNodeWithTag("nano_profiles_list").assertExists()
                editButton(fixture.model.viewState.value.saved.name).performClick()
                onNodeWithTag("nano_settings_form").performScrollToNode(hasText(fixture.verify))
                onNodeWithText(fixture.verify).performClick()
                waitUntil { fixture.model.viewState.value.failure?.error == AssistantError.AUTHENTICATION }
                capture("test-failed")
            }
        }
    }

    @Test
    fun unreadableConfigurationCanBeReloadedIntoLegacyManualModel() {
        runDesktopComposeUiTest(width = 700, height = 850) {
            Fixture(raw = "{corrupt").use { fixture ->
                setContent { fixture.Screen(Layout(name = "legacy", width = 700, height = 850)) }
                waitUntil { fixture.model.viewState.value.saved.readError != null }
                capture("corrupt")
                fixture.preferences.values.value = mutablePreferencesOf(
                    stringPreferencesKey("nano.configuration.v1") to """{"version":1,"configId":"legacy","revision":2,"baseUrl":"https://api.anthropic.com/v1/","modelId":"old-model"}""",
                )
                onNodeWithText(fixture.reload).performClick()
                waitUntil { fixture.model.viewState.value.saved.readError == null }
                editButton("old-model").performClick()
                assertEquals("old-model", fixture.model.viewState.value.modelId)
                onNodeWithTag("nano_model_input").assertIsDisplayed()
                capture("legacy-recovered")
                onNodeWithContentDescription(fixture.info).performClick()
                onNodeWithText(text = "https://api.anthropic.com/v1/chat/completions", substring = true).assertIsDisplayed()
                capture(name = "info-legacy-compatible", dialogTitle = fixture.info)
            }
        }
    }

    private fun DesktopComposeUiTest.editButton(profile: String) = onNodeWithContentDescription(
        runBlocking { getString(Res.string.nano_edit_named_profile, profile) },
    )

    private fun DesktopComposeUiTest.capture(name: String, dialogTitle: String? = null) {
        val node = if (dialogTitle != null) {
            onNode(isRoot() and hasAnyDescendant(hasText(dialogTitle)))
        } else {
            onNode(isRoot() and hasAnyDescendant(hasTestTag("nano_settings_screen")))
        }
        val bitmap = node.captureToImage()
        File("build/reports/nano-settings-ui/$name.png").apply {
            parentFile.mkdirs()
            writeBytes(Image.makeFromBitmap(bitmap.asSkiaBitmap()).encodeToData()!!.bytes)
        }
    }

    private data class InsetsElement(val insets: PlatformWindowInsets) : ModifierNodeElement<InsetsNode>() {
        override fun create() = InsetsNode(insets)
        override fun update(node: InsetsNode) = Unit
        override fun InspectorInfo.inspectableProperties() { name = "syntheticKeyboardInsets" }
    }

    private class InsetsNode(private val insets: PlatformWindowInsets) : PlatformWindowInsetsProviderNode() {
        override fun calculatePlatformInsets(ancestorWindowInsets: PlatformWindowInsets) = insets
    }

    @Serializable
    private data object TestRoute : NavKey

    private data class Layout(val name: String, val width: Int, val height: Int, val dark: Boolean = false, val fontScale: Float = 1f)

    private class PreferencesStore(raw: String?) : DataStore<Preferences> {
        val values = MutableStateFlow(if (raw == null) emptyPreferences() else mutablePreferencesOf(
            stringPreferencesKey("nano.configuration.v1") to raw,
        ))
        var failWrite = false
        override val data = values
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            if (failWrite) error("Synthetic disk failure")
            return transform(values.value).also { values.value = it }
        }
    }

    private class Fixture(raw: String? = null) : AutoCloseable {
        val preferences = PreferencesStore(raw)
        private val keys = mutableMapOf<String, String>()
        val repository = AssistantSettingsRepository(
            dataStore = preferences,
            credentialStore = object : CredentialStore {
                override suspend fun put(id: String, secret: String) { keys[id] = secret }
                override suspend fun get(id: String) = keys[id]
                override suspend fun delete(id: String) { keys.remove(id) }
            },
        )
        val model = AssistantSettingsViewModel(
            repository = repository,
            assistant = object : AssistantRuntime {
                override val available = true
                override fun run(request: AssistantTurnRequest, connection: AssistantConnection) = emptyFlow<AssistantRunEvent>()
                override suspend fun verify(connection: suspend () -> AssistantConnection) = AssistantConnectionResult(success = false, error = AssistantError.AUTHENTICATION)
            },
        )
        private val store = ViewModelStore().apply { put("settings", model) }
        private val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        var custom = ""
        var manual = ""
        var save = ""
        var use = ""
        var verify = ""
        var reload = ""
        var info = ""
        var description = ""
        var ok = ""

        @Composable
        fun Screen(layout: Layout) {
            val backStack = rememberNavBackStack(
                SavedStateConfiguration {
                    serializersModule = SerializersModule {
                        polymorphic(NavKey::class) { subclass(TestRoute::class, TestRoute.serializer()) }
                    }
                }, TestRoute,
            )
            CompositionLocalProvider(
                LocalNavBackStack provides newNavBackStack(backStack, parent = null),
                LocalLifecycleOwner provides owner,
                LocalDensity provides Density(density = 1f, fontScale = layout.fontScale),
                LocalWindowController provides WindowController(onClose = {}),
            ) {
                MaterialTheme(colorScheme = if (layout.dark) darkColorScheme() else lightColorScheme()) {
                    custom = stringResource(Res.string.nano_custom_provider)
                    manual = stringResource(Res.string.nano_manual_model)
                    save = stringResource(Res.string.nano_create_and_use)
                    use = stringResource(Res.string.nano_save_and_use)
                    verify = stringResource(Res.string.nano_test_saved)
                    reload = stringResource(Res.string.nano_reload)
                    info = stringResource(Res.string.nano_settings_info)
                    description = stringResource(Res.string.nano_settings_description)
                    ok = stringResource(Res.string.nano_ok)
                    Surface { AssistantSettingsScreen(onBack = {}, viewModel = model, windowInsets = WindowInsets(0)) }
                }
            }
        }

        override fun close() { store.clear() }
    }
}

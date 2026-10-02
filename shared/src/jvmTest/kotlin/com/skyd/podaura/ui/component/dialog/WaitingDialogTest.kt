package com.skyd.podaura.ui.component.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.skyd.compone.component.dialog.WaitingDialog
import compone.shared.generated.resources.Res
import compone.shared.generated.resources.cancel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class WaitingDialogTest {
    @Test
    fun defaultOptInDisabledAndProgressDialogsRespectCancellation() = runComposeUiTest {
        val visible = mutableStateOf(true)
        val hasCallback = mutableStateOf(false)
        val enabled = mutableStateOf(true)
        val progress = mutableStateOf(false)
        var cancellations = 0
        var underlyingBacks = 0
        var cancelLabel = ""
        // The headless Compose scene does not include desktop's Escape-to-back adapter.
        val backInput = object : NavigationEventInput() {
            fun back() = dispatchOnBackCompleted()
        }
        setContent {
            MaterialTheme {
                val dispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current)
                    .navigationEventDispatcher
                DisposableEffect(dispatcher) {
                    dispatcher.addInput(backInput)
                    onDispose { dispatcher.removeInput(backInput) }
                }
                cancelLabel = stringResource(Res.string.cancel)
                Box(Modifier.fillMaxSize().testTag("background"))
                NavigationBackHandler(
                    state = rememberNavigationEventState(NavigationEventInfo.None),
                    isBackEnabled = true,
                    onBackCompleted = { underlyingBacks++ },
                )
                WaitingDialog(
                    visible = visible.value,
                    title = "Adding subscription",
                    currentValue = if (progress.value) 3 else null,
                    totalValue = if (progress.value) 10 else null,
                    onCancel = if (hasCallback.value) {
                        {
                            cancellations++
                            visible.value = false
                        }
                    } else null,
                    cancelEnabled = enabled.value,
                )
            }
        }

        fun capture(name: String) {
            val bitmap = onNode(isDialog()).captureToImage()
            val output = File("build/reports/waiting-dialog/$name.png")
            output.parentFile.mkdirs()
            output.writeBytes(Image.makeFromBitmap(bitmap.asSkiaBitmap()).encodeToData()!!.bytes)
        }

        onNodeWithText(cancelLabel).assertDoesNotExist()
        runOnIdle { backInput.back() }
        onNode(isDialog()).assertExists()
        capture("default")

        runOnIdle { hasCallback.value = true }
        onNodeWithText(cancelLabel).assertIsEnabled()
        onNodeWithTag("background").performTouchInput { click(Offset(1f, 1f)) }
        onNode(isDialog()).assertExists()
        capture("cancelable")

        runOnIdle { enabled.value = false }
        onNodeWithText(cancelLabel).assertIsNotEnabled().performClick()
        runOnIdle { backInput.back() }
        onNode(isDialog()).assertExists()
        assertEquals(0, cancellations)
        capture("saving")

        runOnIdle { enabled.value = true }
        runOnIdle { backInput.back() }
        onNode(isDialog()).assertDoesNotExist()
        assertEquals(1, cancellations)

        runOnIdle {
            visible.value = true
            progress.value = true
        }
        onNodeWithText("3 / 10").assertExists()
        capture("progress")
        onNodeWithText(cancelLabel).performClick()
        onNode(isDialog()).assertDoesNotExist()
        assertEquals(2, cancellations)
        assertEquals(0, underlyingBacks)
    }
}

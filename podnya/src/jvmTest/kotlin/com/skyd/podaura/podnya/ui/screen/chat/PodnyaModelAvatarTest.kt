package com.skyd.podaura.podnya.ui.screen.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.ic_model_claude
import com.skyd.podnya.resources.ic_model_deepseek
import com.skyd.podnya.resources.ic_model_gemini
import com.skyd.podnya.resources.ic_model_grok
import com.skyd.podnya.resources.ic_model_openai
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class PodnyaModelAvatarTest {
    @Test
    fun recognizesModelFamiliesIncludingGatewaysAndKeepsUnknownModelsGeneric() {
        for (model in listOf("gpt-6.1-sol", "gpt-6-luna", "gpt-6-astra", "openai/gpt-4o", "o3", "o4-mini")) {
            assertEquals(Res.drawable.ic_model_openai, podnyaModelIcon(model))
        }
        assertEquals(Res.drawable.ic_model_deepseek, podnyaModelIcon("deepseek-ai/DeepSeek-V4-Pro"))
        assertEquals(Res.drawable.ic_model_claude, podnyaModelIcon("anthropic/claude-sonnet-5-5"))
        assertEquals(Res.drawable.ic_model_gemini, podnyaModelIcon("models/gemini-4-hydrogen"))
        assertEquals(Res.drawable.ic_model_grok, podnyaModelIcon("x-ai/grok-4.7"))
        for (model in listOf(null, "", "custom-model", "not-claude-sonnet", "o30", "my-gpt-proxy")) {
            assertNull(podnyaModelIcon(model))
        }
    }

    @Test
    fun rendersAllAvatarsInLightAndDarkThemes() {
        for (dark in listOf(false, true)) {
            runDesktopComposeUiTest(width = if (dark) 1100 else 390, height = 520) {
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1.3f)) {
                        MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                            Surface(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    for (model in listOf("gpt-6.1-sol", "deepseek-flash", "claude-sonnet-5-5", "gemini-4-hydrogen", "grok-4.7", "custom-model")) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            PodnyaModelAvatar(modelId = model)
                                            Text(text = model, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                val bitmap = onRoot().captureToImage()
                File("build/reports/podnya-ui/model-avatars-${if (dark) "dark" else "light"}.png").apply {
                    parentFile.mkdirs()
                    writeBytes(Image.makeFromBitmap(bitmap.asSkiaBitmap()).encodeToData()!!.bytes)
                }
            }
        }
    }
}

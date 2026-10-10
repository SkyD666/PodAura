package com.skyd.podaura.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.skyd.podaura.di.ioModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PodAuraImageTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun imagesShareTheLoaderAcrossCompositionsAndKeepTheirOwnErrorCallbacks() {
        val client = HttpClient(MockEngine { error("Unexpected network request") })
        val koin = startKoin {
            modules(ioModule, module { single(named("coil")) { client } })
        }.koin
        try {
            val sharedLoader = koin.get<ImageLoader>()
            val firstModel = Any()
            val secondModel = Any()
            val visible = mutableStateOf(true)
            val firstFailure = mutableStateOf<Any?>(null)
            val secondFailure = mutableStateOf<Any?>(null)
            var loaders = emptyList<ImageLoader>()

            runComposeUiTest {
                setContent {
                    if (visible.value) {
                        val currentLoaders = List(20) { rememberPodAuraImageLoader() }
                        SideEffect { loaders = currentLoaders }
                        // Unsupported models fail locally, so each image must call its own callback.
                        PodAuraImage(firstModel, modifier = Modifier.size(24.dp), onError = {
                            firstFailure.value = it.result.request.data
                        })
                        PodAuraImage(secondModel, modifier = Modifier.size(24.dp), onError = {
                            secondFailure.value = it.result.request.data
                        })
                    }
                }
                waitUntil { firstFailure.value != null && secondFailure.value != null }
                runOnIdle {
                    assertSame(firstModel, firstFailure.value)
                    assertSame(secondModel, secondFailure.value)
                    assertEquals(20, loaders.size)
                    loaders.forEach { assertSame(sharedLoader, it) }
                    visible.value = false
                }
                waitForIdle()
                runOnIdle {
                    firstFailure.value = null
                    secondFailure.value = null
                    loaders = emptyList()
                    visible.value = true
                }
                waitUntil { firstFailure.value != null && secondFailure.value != null }
                runOnIdle {
                    assertSame(firstModel, firstFailure.value)
                    assertSame(secondModel, secondFailure.value)
                    assertEquals(20, loaders.size)
                    loaders.forEach { assertSame(sharedLoader, it) }
                }
            }
        } finally {
            stopKoin()
            client.close()
        }
    }
}

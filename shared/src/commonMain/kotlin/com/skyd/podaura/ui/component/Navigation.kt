package com.skyd.podaura.ui.component

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import com.skyd.podaura.ui.component.navigation.MaterialSharedAxis
import com.skyd.podaura.ui.component.navigation.deeplink.TypeParser
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid

@Serializable
data class UuidList(val uuids: List<String>) {
    companion object {
        fun encodeUuidList(uuidList: List<Uuid>): String {
            val buffer = Buffer()
            for (u in uuidList) {
                u.toLongs { most, least ->
                    buffer.writeLong(most)
                    buffer.writeLong(least)
                }
            }
            return Base64.UrlSafe.encode(buffer.readByteArray())
        }

        fun decodeUuidList(uuidListString: String): List<Uuid> {
            val bytes: ByteArray = Base64.UrlSafe.decode(uuidListString)
            val uuids = mutableListOf<Uuid>()
            val buffer = Buffer().apply { write(bytes) }
            while (buffer.size >= 16) {
                val msb = buffer.readLong()
                val lsb = buffer.readLong()
                uuids += Uuid.fromLongs(msb, lsb)
            }
            return uuids
        }
    }
}

fun uuidListType(): TypeParser {
    return { value ->
        Json.encodeToJsonElement(UuidList(UuidList.decodeUuidList(value).map { it.toString() }))
    }
}

@Composable
fun <T : Any> PodAuraNavDisplay(
    backStack: List<T>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {
        if (backStack is MutableList<T>) {
            backStack.removeLastOrNull()
        }
    },
    entryDecorators: List<NavEntryDecorator<T>> = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
    ),
    sceneStrategies: List<SceneStrategy<T>> = listOf(SinglePaneSceneStrategy()),
    transitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        MaterialSharedAxis.Z.TransitionSpec
    },
    popTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        MaterialSharedAxis.Z.PopTransitionSpec
    },
    predictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.(
        @NavigationEvent.SwipeEdge Int
    ) -> ContentTransform = {
        MaterialSharedAxis.Z.PopTransitionSpec
    },
    entryProvider: (key: T) -> NavEntry<T>,
) = NavDisplay(
    backStack = backStack,
    modifier = modifier.background(MaterialTheme.colorScheme.background),
    onBack = onBack,
    entryDecorators = entryDecorators,
    sceneStrategies = sceneStrategies,
    transitionSpec = transitionSpec,
    popTransitionSpec = popTransitionSpec,
    predictivePopTransitionSpec = predictivePopTransitionSpec,
    entryProvider = entryProvider,
)

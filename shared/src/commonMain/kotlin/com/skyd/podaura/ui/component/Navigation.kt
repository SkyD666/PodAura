package com.skyd.podaura.ui.component

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.AnimatedPaneScope
import androidx.compose.material3.adaptive.layout.ExtendedPaneScaffoldPaneScope
import androidx.compose.material3.adaptive.layout.PaneMotionDefaults
import androidx.compose.material3.adaptive.layout.PaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion
import androidx.compose.ui.unit.IntRect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation.NavType
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.skyd.podaura.ui.component.UuidListType.Companion.decodeUuidList
import com.skyd.podaura.ui.component.navigation.deeplink.TypeParser
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid


inline fun <reified T> serializableType(
    json: Json = Json,
): TypeParser {
    return { value ->
        json.decodeFromString(value.hexToByteArray().decodeToString())
    }
}

inline fun <reified T> listType(
    json: Json = Json,
): TypeParser {
    return {
        serializableType<List<T>>(json)
    }
}

@Serializable
data class UuidList(val uuids: List<String>)

abstract class UuidListType<T>(
    isNullableAllowed: Boolean = false,
) : NavType<T>(isNullableAllowed) {
    companion object {
        fun encodeUuidList(uuidList: List<Uuid>): String {
            val buffer = Buffer()
            for (u in uuidList) {
                val (mostSignificantBits, leastSignificantBits) = u.toLongs { most, least ->
                    most to least
                }
                buffer.writeLong(mostSignificantBits)
                buffer.writeLong(leastSignificantBits)
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
        UuidList(decodeUuidList(value).map { it.toString() })
    }
}

val EnterTransition = fadeIn(animationSpec = tween(220, delayMillis = 30)) + scaleIn(
    animationSpec = tween(220, delayMillis = 30),
    initialScale = 0.92f,
)

val ExitTransition = fadeOut(animationSpec = tween(90))

val PopEnterTransition = fadeIn(animationSpec = tween(220)) + scaleIn(
    animationSpec = tween(220),
    initialScale = 0.92f,
)

val PopExitTransition = fadeOut(animationSpec = tween(220)) + scaleOut(
    animationSpec = tween(220),
    targetScale = 0.92f,
)

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
    sceneStrategy: SceneStrategy<T> = SinglePaneSceneStrategy(),
    entryProvider: (key: T) -> NavEntry<T>,
) = NavDisplay(
    backStack = backStack,
    modifier = modifier.background(MaterialTheme.colorScheme.background),
    onBack = onBack,
    entryDecorators = entryDecorators,
    sceneStrategy = sceneStrategy,
    transitionSpec = { EnterTransition togetherWith ExitTransition },
    popTransitionSpec = { PopEnterTransition togetherWith PopExitTransition },
    predictivePopTransitionSpec = { PopEnterTransition togetherWith PopExitTransition },
    entryProvider = entryProvider,
)

@Composable
fun <S : PaneScaffoldRole, T : PaneScaffoldValue<S>> ExtendedPaneScaffoldPaneScope<S, T>.PodAuraAnimatedPane(
    modifier: Modifier = Modifier,
    boundsAnimationSpec: FiniteAnimationSpec<IntRect> = PaneMotionDefaults.AnimationSpec,
    content: (@Composable AnimatedPaneScope.() -> Unit),
) = AnimatedPane(
    modifier = modifier,
    enterTransition = EnterTransition,
    exitTransition = ExitTransition,
    boundsAnimationSpec = boundsAnimationSpec,
    content = content,
)

package com.skyd.podaura.ui.component

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.PathEasing
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntRect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
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
        Json.encodeToJsonElement(UuidList(UuidList.decodeUuidList(value).map { it.toString() }))
    }
}

private val MotionEmphasizedEasing = PathEasing(
    Path().apply {
        moveTo(0f, 0f)
        cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
        cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
    }
)

private const val DefaultDuration: Int = 450
private const val ProgressThreshold: Float = 0.35f

private val FadeInEasing = Easing { fraction ->
    ((MotionEmphasizedEasing.transform(fraction) - ProgressThreshold) / (1f - ProgressThreshold))
        .coerceIn(0f, 1f)
}

private val FadeOutEasing = Easing { fraction ->
    (MotionEmphasizedEasing.transform(fraction) / ProgressThreshold)
        .coerceIn(0f, 1f)
}

val EnterTransition =
    scaleIn(
        initialScale = 0.8f,
        animationSpec = tween(durationMillis = DefaultDuration, easing = MotionEmphasizedEasing)
    ) + fadeIn(
        animationSpec = tween(durationMillis = DefaultDuration, easing = FadeInEasing)
    )

val ExitTransition =
    scaleOut(
        targetScale = 1.1f,
        animationSpec = tween(durationMillis = DefaultDuration, easing = MotionEmphasizedEasing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = DefaultDuration, easing = FadeOutEasing)
    )

val PopEnterTransition =
    scaleIn(
        initialScale = 1.1f,
        animationSpec = tween(durationMillis = DefaultDuration, easing = MotionEmphasizedEasing)
    ) + fadeIn(
        animationSpec = tween(durationMillis = DefaultDuration, easing = FadeInEasing)
    )

val PopExitTransition =
    scaleOut(
        targetScale = 0.8f,
        animationSpec = tween(durationMillis = DefaultDuration, easing = MotionEmphasizedEasing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = DefaultDuration, easing = FadeOutEasing)
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
    transitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        EnterTransition togetherWith ExitTransition
    },
    popTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        PopEnterTransition togetherWith PopExitTransition
    },
    predictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.(
        @NavigationEvent.SwipeEdge Int
    ) -> ContentTransform = {
        PopEnterTransition togetherWith PopExitTransition
    },
    entryProvider: (key: T) -> NavEntry<T>,
) = NavDisplay(
    backStack = backStack,
    modifier = modifier.background(MaterialTheme.colorScheme.background),
    onBack = onBack,
    entryDecorators = entryDecorators,
    sceneStrategy = sceneStrategy,
    transitionSpec = transitionSpec,
    popTransitionSpec = popTransitionSpec,
    predictivePopTransitionSpec = predictivePopTransitionSpec,
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

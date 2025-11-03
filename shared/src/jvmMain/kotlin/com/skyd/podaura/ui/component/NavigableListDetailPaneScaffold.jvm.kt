package com.skyd.podaura.ui.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldPaneScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

@Composable
actual fun <T> NavigableListDetailPaneScaffold(
    navigator: ThreePaneScaffoldNavigator<T>,
    listPane: @Composable (ThreePaneScaffoldPaneScope.() -> Unit),
    detailPane: @Composable (ThreePaneScaffoldPaneScope.() -> Unit),
    modifier: Modifier,
    extraPane: @Composable (ThreePaneScaffoldPaneScope.() -> Unit)?,
    defaultBackBehavior: BackNavigationBehavior,
    paneExpansionDragHandle: @Composable (ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)?,
    paneExpansionState: PaneExpansionState?
) {
    // androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldPredictiveBackHandler
    key(navigator, defaultBackBehavior) {
        PredictiveBackHandler(enabled = navigator.canNavigateBack(defaultBackBehavior)) { progress ->
            // code for gesture back started
            try {
                progress.collect { backEvent ->
                    navigator.seekBack(
                        defaultBackBehavior,
                        fraction = backProgressToStateProgress(
                            progress = backEvent.progress,
                            scaffoldValue = navigator.scaffoldValue
                        ),
                    )
                }
                // code for completion
                navigator.navigateBack(defaultBackBehavior)
            } catch (_: CancellationException) {
                // code for cancellation
                withContext(NonCancellable) {
                    navigator.seekBack(
                        defaultBackBehavior,
                        fraction = 0f
                    )
                }
            }
        }
    }

    ListDetailPaneScaffold(
        modifier = modifier,
        directive = navigator.scaffoldDirective,
        scaffoldState = navigator.scaffoldState,
        detailPane = detailPane,
        listPane = listPane,
        extraPane = extraPane,
        paneExpansionDragHandle = paneExpansionDragHandle,
        paneExpansionState = paneExpansionState,
    )
}

private fun backProgressToStateProgress(
    progress: Float,
    scaffoldValue: ThreePaneScaffoldValue,
): Float =
    ThreePaneScaffoldPredictiveBackEasing.transform(progress) *
            when (scaffoldValue.expandedCount) {
                1 -> SinglePaneProgressRatio
                2 -> DualPaneProgressRatio
                else -> TriplePaneProgressRatio
            }

private val ThreePaneScaffoldPredictiveBackEasing: Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
private const val SinglePaneProgressRatio: Float = 0.1f
private const val DualPaneProgressRatio: Float = 0.15f
private const val TriplePaneProgressRatio: Float = 0.2f

private val ThreePaneScaffoldValue.expandedCount: Int
    get() {
        var count = 0
        if (primary == PaneAdaptedValue.Expanded) {
            count++
        }
        if (secondary == PaneAdaptedValue.Expanded) {
            count++
        }
        if (tertiary == PaneAdaptedValue.Expanded) {
            count++
        }
        return count
    }

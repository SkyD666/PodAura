package com.skyd.podaura.ui.component

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.MutableThreePaneScaffoldState
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldAdaptStrategies
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import kotlinx.serialization.json.Json

/**
 * Copied from `androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator`
 */
@Composable
inline fun <reified T> rememberListDetailPaneScaffoldNavigator(
    scaffoldDirective: PaneScaffoldDirective =
        calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
    adaptStrategies: ThreePaneScaffoldAdaptStrategies =
        ListDetailPaneScaffoldDefaults.adaptStrategies(),
    isDestinationHistoryAware: Boolean = true,
    initialDestinationHistory: List<ThreePaneScaffoldDestinationItem<T>> =
        DefaultListDetailPaneHistory
): ThreePaneScaffoldNavigator<T> =
    rememberThreePaneScaffoldNavigator(
        scaffoldDirective,
        adaptStrategies,
        isDestinationHistoryAware,
        initialDestinationHistory
    )

@Composable
inline fun <reified T> rememberThreePaneScaffoldNavigator(
    scaffoldDirective: PaneScaffoldDirective,
    adaptStrategies: ThreePaneScaffoldAdaptStrategies,
    isDestinationHistoryAware: Boolean,
    initialDestinationHistory: List<ThreePaneScaffoldDestinationItem<T>>
): ThreePaneScaffoldNavigator<T> =
    rememberSaveable(
        saver = DefaultThreePaneScaffoldNavigator.saver(
            scaffoldDirective,
            adaptStrategies,
            isDestinationHistoryAware
        )
    ) {
        DefaultThreePaneScaffoldNavigator(
            initialDestinationHistory = initialDestinationHistory,
            initialScaffoldDirective = scaffoldDirective,
            initialAdaptStrategies = adaptStrategies,
            initialIsDestinationHistoryAware = isDestinationHistoryAware
        )
    }.apply {
        this.scaffoldDirective = scaffoldDirective
        this.adaptStrategies = adaptStrategies
        this.isDestinationHistoryAware = isDestinationHistoryAware
    }

class DefaultThreePaneScaffoldNavigator<T>(
    initialDestinationHistory: List<ThreePaneScaffoldDestinationItem<T>>,
    initialScaffoldDirective: PaneScaffoldDirective,
    initialAdaptStrategies: ThreePaneScaffoldAdaptStrategies,
    initialIsDestinationHistoryAware: Boolean,
) : ThreePaneScaffoldNavigator<T> {

    val destinationHistory =
        mutableStateListOf<ThreePaneScaffoldDestinationItem<T>>().apply {
            addAll(initialDestinationHistory)
        }

    override var scaffoldDirective by mutableStateOf(initialScaffoldDirective)

    override var isDestinationHistoryAware by mutableStateOf(initialIsDestinationHistoryAware)

    var adaptStrategies by mutableStateOf(initialAdaptStrategies)

    override val currentDestination
        get() = destinationHistory.lastOrNull()

    override val scaffoldValue by derivedStateOf {
        calculateScaffoldValue(destinationHistory.lastIndex)
    }

    // Must be updated whenever `destinationHistory` changes to keep in sync.
    override val scaffoldState = MutableThreePaneScaffoldState(scaffoldValue)

    override fun peekPreviousScaffoldValue(
        backNavigationBehavior: BackNavigationBehavior
    ): ThreePaneScaffoldValue {
        val index = getPreviousDestinationIndex(backNavigationBehavior)
        return if (index == -1) scaffoldValue else calculateScaffoldValue(index)
    }

    override suspend fun navigateTo(pane: ThreePaneScaffoldRole, contentKey: T?) {
        destinationHistory.add(ThreePaneScaffoldDestinationItem(pane, contentKey))
        animateStateToCurrentScaffoldValue()
    }

    override fun canNavigateBack(backNavigationBehavior: BackNavigationBehavior): Boolean =
        getPreviousDestinationIndex(backNavigationBehavior) >= 0

    override suspend fun navigateBack(backNavigationBehavior: BackNavigationBehavior): Boolean {
        val previousDestinationIndex = getPreviousDestinationIndex(backNavigationBehavior)
        if (previousDestinationIndex < 0) {
            destinationHistory.clear()
            animateStateToCurrentScaffoldValue()
            return false
        }
        val targetSize = previousDestinationIndex + 1
        while (destinationHistory.size > targetSize) {
            destinationHistory.removeLast()
        }
        animateStateToCurrentScaffoldValue()
        return true
    }

    override suspend fun seekBack(backNavigationBehavior: BackNavigationBehavior, fraction: Float) {
        if (fraction == 0f) {
            animateStateToCurrentScaffoldValue()
        } else {
            val previousScaffoldValue = peekPreviousScaffoldValue(backNavigationBehavior)
            scaffoldState.seekTo(fraction, previousScaffoldValue, isPredictiveBackInProgress = true)
        }
    }

    private suspend fun animateStateToCurrentScaffoldValue() {
        scaffoldState.animateTo(scaffoldValue)
    }

    private fun getPreviousDestinationIndex(backNavBehavior: BackNavigationBehavior): Int {
        if (destinationHistory.size <= 1) {
            // No previous destination
            return -1
        }
        when (backNavBehavior) {
            BackNavigationBehavior.PopLatest -> return destinationHistory.lastIndex - 1
            BackNavigationBehavior.PopUntilScaffoldValueChange ->
                for (previousDestinationIndex in destinationHistory.lastIndex - 1 downTo 0) {
                    val previousValue = calculateScaffoldValue(previousDestinationIndex)
                    if (previousValue != scaffoldValue) {
                        return previousDestinationIndex
                    }
                }

            BackNavigationBehavior.PopUntilCurrentDestinationChange ->
                for (previousDestinationIndex in destinationHistory.lastIndex - 1 downTo 0) {
                    val destination = destinationHistory[previousDestinationIndex].pane
                    if (destination != currentDestination?.pane) {
                        return previousDestinationIndex
                    }
                }

            BackNavigationBehavior.PopUntilContentChange ->
                for (previousDestinationIndex in destinationHistory.lastIndex - 1 downTo 0) {
                    val contentKey = destinationHistory[previousDestinationIndex].contentKey
                    if (contentKey != currentDestination?.contentKey) {
                        return previousDestinationIndex
                    }
                    // A scaffold value change also counts as a content change.
                    val previousValue = calculateScaffoldValue(previousDestinationIndex)
                    if (previousValue != scaffoldValue) {
                        return previousDestinationIndex
                    }
                }
        }

        return -1
    }

    private fun calculateScaffoldValue(destinationIndex: Int) =
        if (destinationIndex == -1) {
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = scaffoldDirective.maxHorizontalPartitions,
                maxVerticalPartitions = scaffoldDirective.maxVerticalPartitions,
                adaptStrategies = adaptStrategies,
                currentDestination = null,
            )
        } else if (isDestinationHistoryAware) {
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = scaffoldDirective.maxHorizontalPartitions,
                maxVerticalPartitions = scaffoldDirective.maxVerticalPartitions,
                adaptStrategies = adaptStrategies,
                destinationHistory = destinationHistory.subList(0, destinationIndex + 1),
            )
        } else {
            calculateThreePaneScaffoldValue(
                maxHorizontalPartitions = scaffoldDirective.maxHorizontalPartitions,
                maxVerticalPartitions = scaffoldDirective.maxVerticalPartitions,
                adaptStrategies = adaptStrategies,
                currentDestination = destinationHistory[destinationIndex],
            )
        }

    companion object {
        /** To keep destination history saved */
        inline fun <reified T> saver(
            initialScaffoldDirective: PaneScaffoldDirective,
            initialAdaptStrategies: ThreePaneScaffoldAdaptStrategies,
            initialDestinationHistoryAware: Boolean,
        ): Saver<DefaultThreePaneScaffoldNavigator<T>, *> {
            val destinationItemSaver = destinationItemSaver<T>()
            return listSaver(
                save = {
                    it.destinationHistory.fastMap { destination ->
                        with(destinationItemSaver) { save(destination) }
                    }
                },
                restore = {
                    DefaultThreePaneScaffoldNavigator(
                        initialDestinationHistory =
                            it.fastMap { savedDestination ->
                                destinationItemSaver.restore(savedDestination!!)!!
                            },
                        initialScaffoldDirective = initialScaffoldDirective,
                        initialAdaptStrategies = initialAdaptStrategies,
                        initialIsDestinationHistoryAware = initialDestinationHistoryAware,
                    )
                },
            )
        }
    }
}

inline fun <reified T> destinationItemSaver(): Saver<ThreePaneScaffoldDestinationItem<T>, Any> =
    listSaver(
        save = {
            val contentKey = it.contentKey?.let { key ->
                runCatching { Json.encodeToString(key) }.getOrNull()
            }
            mutableListOf<Any>(it.pane).apply { contentKey?.let { key -> add(key) } }
        },
        restore = {
            ThreePaneScaffoldDestinationItem(
                pane = it[0] as ThreePaneScaffoldRole,
                contentKey = it.getOrNull(1)
                    ?.let { key -> Json.decodeFromString(key as String) as T }
            )
        },
    )

val DefaultListDetailPaneHistory: List<ThreePaneScaffoldDestinationItem<Nothing>> =
    listOf(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List))

package com.skyd.podaura.ext

import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

fun NavBackStackEntry.lifecycleIsResumed() =
    this.lifecycle.currentState == Lifecycle.State.RESUMED

fun NavController.popBackStackWithLifecycle(): Boolean {
    if (currentBackStackEntry?.lifecycleIsResumed() == true) {
        return popBackStack()
    }
    return true
}

val <T> ThreePaneScaffoldNavigator<T>.isDetailPaneVisible: Boolean
    get() = scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded

val <T> ThreePaneScaffoldNavigator<T>.isSinglePane: Boolean
    get() = scaffoldDirective.maxHorizontalPartitions == 1
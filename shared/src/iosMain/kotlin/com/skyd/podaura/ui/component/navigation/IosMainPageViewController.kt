package com.skyd.podaura.ui.component.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.skyd.compone.component.navigation.LocalGlobalNavBackStack
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.component.navigation.LocalResultStore
import com.skyd.compone.component.navigation.newNavBackStack
import com.skyd.compone.component.navigation.rememberResultStore
import com.skyd.podaura.ui.screen.MainNavHost
import com.skyd.podaura.ui.screen.SettingsProvider
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

/** Each external page launch owns a native container and an independent Compose navigation stack. */
internal class IosMainPageViewController(
    navigation: IosAppNavigationController,
    route: NavKey,
) : UIViewController(nibName = null, bundle = null) {
    val backStack = NavBackStack(route)
    private val backInput = IosPageBackInput()
    val canPopPage: Boolean get() = backStack.size <= 1 && backInput.canPopPage
    private val compose = ComposeUIViewController {
        val stack = newNavBackStack(backStack, parent = null)
        val events = rememberNavigationEventDispatcherOwner(
            enabled = navigation.visibleController == this,
        )
        DisposableEffect(events) {
            val dispatcher = events.navigationEventDispatcher
            dispatcher.addInput(backInput)
            onDispose { dispatcher.removeInput(backInput) }
        }
        CompositionLocalProvider(
            LocalIosAppNavigation provides navigation,
            LocalNavBackStack provides stack,
            LocalGlobalNavBackStack provides stack,
            LocalResultStore provides rememberResultStore(),
            LocalNavigationEventDispatcherOwner provides events,
        ) {
            navigation.pageContent {
                SettingsProvider {
                    MainNavHost()
                }
            }
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        addChildViewController(compose)
        compose.view.setFrame(view.bounds)
        compose.view.autoresizingMask =
            UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        view.addSubview(compose.view)
        compose.didMoveToParentViewController(this)
    }
}

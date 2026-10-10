package com.skyd.podaura

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.model.worker.rsssync.IosRssSync
import com.skyd.podaura.ui.component.navigation.IosAppNavigationController
import com.skyd.podaura.ui.component.navigation.LocalIosAppNavigation
import com.skyd.podaura.ui.notification.IosArticleNotificationLifecycle
import com.skyd.podaura.ui.notification.PlatformArticleNotification
import com.skyd.podaura.ui.player.IosPlayerSession
import com.skyd.podaura.ui.player.LocalIosPlayerSession
import com.skyd.podaura.ui.player.LocalPlayerSession
import com.skyd.podaura.ui.screen.AppEntrance
import kotlinx.cinterop.cValue
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowSceneGeometryPreferencesIOS
import platform.UIKit.setNeedsUpdateOfHomeIndicatorAutoHidden

internal object IosPlayerChrome {
    var controller: UIViewController? = null
    var fullscreen = false
        private set

    fun setFullscreen(value: Boolean) {
        fullscreen = value
        controller?.setNeedsStatusBarAppearanceUpdate()
        controller?.setNeedsUpdateOfHomeIndicatorAutoHidden()
    }

    fun requestOrientation(orientation: UIInterfaceOrientationMask) {
        if (NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(
                cValue<NSOperatingSystemVersion> {
                    majorVersion = 16; minorVersion = 0; patchVersion = 0
                }
            )
        ) {
            controller?.view?.window?.windowScene?.requestGeometryUpdateWithPreferences(
                UIWindowSceneGeometryPreferencesIOS(orientation),
                errorHandler = null
            )
        }
    }
}

private var iosAppInitialized = false

/** Called before launch completes, including launches initiated by a background refresh. */
fun initializeIosApp() {
    if (iosAppInitialized) return
    iosAppInitialized = true
    PlatformArticleNotification.initialize()
    initKoin()
    onAppStart()
    IosArticleNotificationLifecycle.initialize()
    IosRssSync.initialize()
}

@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    initializeIosApp()
    val session = IosPlayerSession()
    lateinit var navigation: IosAppNavigationController
    val root = ComposeUIViewController { IosApp(session, navigation) }
    navigation = IosAppNavigationController(
        root, session::onFullPlayerClosed,
        onPlayerWillClose = session::onFullPlayerWillClose,
        onPlayerCloseCancelled = session::onFullPlayerCloseCancelled,
        pageContent = { content ->
            CompositionLocalProvider(
                LocalPlayerSession provides session,
                LocalIosPlayerSession provides session,
                content = content,
            )
        },
    )
    session.navigationController = navigation
    return navigation
}

@Composable
private fun IosApp(session: IosPlayerSession, navigation: IosAppNavigationController) {
    DisposableEffect(session, navigation) {
        onDispose {
            session.close()
            if (IosPlayerChrome.controller === navigation) IosPlayerChrome.controller = null
        }
    }
    val events = rememberNavigationEventDispatcherOwner(enabled = navigation.isRootVisible)
    CompositionLocalProvider(
        LocalIosAppNavigation provides navigation,
        LocalPlayerSession provides session,
        LocalIosPlayerSession provides session,
        LocalNavigationEventDispatcherOwner provides events,
    ) {
        AppEntrance()
    }
}

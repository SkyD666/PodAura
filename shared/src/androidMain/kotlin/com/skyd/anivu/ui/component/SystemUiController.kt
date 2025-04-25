package com.skyd.anivu.ui.component

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.skyd.anivu.ext.tryWindow

@Stable
interface SystemUiController {

    /**
     * Control for the behavior of the system bars. This value should be one of the
     * [WindowInsetsControllerCompat] behavior constants:
     * [WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH] (Deprecated),
     * [WindowInsetsControllerCompat.BEHAVIOR_DEFAULT] and
     * [WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE].
     */
    var systemBarsBehavior: Int

    /**
     * Property which holds the status bar visibility. If set to true, show the status bar,
     * otherwise hide the status bar.
     */
    var isStatusBarVisible: Boolean

    /**
     * Property which holds the navigation bar visibility. If set to true, show the navigation bar,
     * otherwise hide the navigation bar.
     */
    var isNavigationBarVisible: Boolean

    /**
     * Property which holds the status & navigation bar visibility. If set to true, show both bars,
     * otherwise hide both bars.
     */
    var isSystemBarsVisible: Boolean
        get() = isNavigationBarVisible && isStatusBarVisible
        set(value) {
            isStatusBarVisible = value
            isNavigationBarVisible = value
        }

    /**
     * Property which holds whether the status bar icons + content are 'dark' or not.
     */
    var statusBarDarkContentEnabled: Boolean

    /**
     * Property which holds whether the navigation bar icons + content are 'dark' or not.
     */
    var navigationBarDarkContentEnabled: Boolean

    /**
     * Property which holds whether the status & navigation bar icons + content are 'dark' or not.
     */
    var systemBarsDarkContentEnabled: Boolean

    /**
     * Property which holds whether the system is ensuring that the navigation bar has enough
     * contrast when a fully transparent background is requested. Only has an affect when running
     * on Android API 29+ devices.
     */
    var isNavigationBarContrastEnforced: Boolean
}

/**
 * Remembers a [SystemUiController] for the given [window].
 *
 * If no [window] is provided, an attempt to find the correct [Window] is made.
 *
 * First, if the [LocalView]'s parent is a [DialogWindowProvider], then that dialog's [Window] will
 * be used.
 *
 * Second, we attempt to find [Window] for the [Activity] containing the [LocalView].
 *
 * If none of these are found (such as may happen in a preview), then the functionality of the
 * returned [SystemUiController] will be degraded, but won't throw an exception.
 */
@Composable
fun rememberSystemUiController(
    window: Window? = LocalView.current.tryWindow,
): SystemUiController {
    val view = LocalView.current
    return remember(view, window) { AndroidSystemUiController(view, window) }
}

/**
 * A helper class for setting the navigation and status bar colors for a [View], gracefully
 * degrading behavior based upon API level.
 *
 * Typically you would use [rememberSystemUiController] to remember an instance of this.
 */
internal class AndroidSystemUiController(
    private val view: View,
    private val window: Window?
) : SystemUiController {
    private val windowInsetsController = window?.let {
        WindowCompat.getInsetsController(it, view)
    }

    override var systemBarsBehavior: Int
        get() = windowInsetsController?.systemBarsBehavior ?: 0
        set(value) {
            windowInsetsController?.systemBarsBehavior = value
        }

    override var isStatusBarVisible: Boolean
        get() {
            return ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.statusBars()) == true
        }
        set(value) {
            if (value) {
                windowInsetsController?.show(WindowInsetsCompat.Type.statusBars())
            } else {
                windowInsetsController?.hide(WindowInsetsCompat.Type.statusBars())
            }
        }

    override var isNavigationBarVisible: Boolean
        get() {
            return ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.navigationBars()) == true
        }
        set(value) {
            if (value) {
                windowInsetsController?.show(WindowInsetsCompat.Type.navigationBars())
            } else {
                windowInsetsController?.hide(WindowInsetsCompat.Type.navigationBars())
            }
        }

    override var statusBarDarkContentEnabled: Boolean
        get() = windowInsetsController?.isAppearanceLightStatusBars == true
        set(value) {
            windowInsetsController?.isAppearanceLightStatusBars = value
        }

    override var navigationBarDarkContentEnabled: Boolean
        get() = windowInsetsController?.isAppearanceLightNavigationBars == true
        set(value) {
            windowInsetsController?.isAppearanceLightNavigationBars = value
        }

    override var systemBarsDarkContentEnabled: Boolean
        get() = statusBarDarkContentEnabled && navigationBarDarkContentEnabled
        set(value) {
            statusBarDarkContentEnabled = value
            navigationBarDarkContentEnabled = value
        }

    override var isNavigationBarContrastEnforced: Boolean
        get() = Build.VERSION.SDK_INT >= 29 && window?.isNavigationBarContrastEnforced == true
        set(value) {
            if (Build.VERSION.SDK_INT >= 29) {
                window?.isNavigationBarContrastEnforced = value
            }
        }
}
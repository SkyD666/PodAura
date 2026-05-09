package com.skyd.podaura.ui.component

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.skyd.podaura.ext.isLandscape
import com.skyd.podaura.ext.sensorLandOrientation
import com.skyd.podaura.ext.unspecifiedOrientation

@Composable
actual fun isLandscape(): Boolean {
    return LocalConfiguration.current.isLandscape
}

@Composable
actual fun rememberOrientationController(): OrientationController {
    val activity = LocalActivity.current

    return remember {
        object : OrientationController {
            override fun landscape() {
                activity?.sensorLandOrientation()
            }

            override fun unspecified() {
                activity?.unspecifiedOrientation()
            }
        }
    }
}

package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.skyd.podaura.ext.activity
import com.skyd.podaura.ext.isLandscape
import com.skyd.podaura.ext.sensorLandOrientation
import com.skyd.podaura.ext.unspecifiedOrientation

@Composable
actual fun isLandscape(): Boolean {
    return LocalConfiguration.current.isLandscape
}


@Composable
actual fun rememberOrientationController(): OrientationController {
    val context = LocalContext.current
    return remember {
        object : OrientationController {
            override fun landscape() = context.activity.sensorLandOrientation()
            override fun unspecified() = context.activity.unspecifiedOrientation()
        }
    }
}
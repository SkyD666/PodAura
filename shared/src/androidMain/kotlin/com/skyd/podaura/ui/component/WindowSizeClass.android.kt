package com.skyd.podaura.ui.component

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable

@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
    return calculateWindowSizeClass(activity = LocalActivity.current!!)
}

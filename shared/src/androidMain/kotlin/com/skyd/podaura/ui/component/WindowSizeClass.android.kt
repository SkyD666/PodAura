package com.skyd.podaura.ui.component

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.skyd.podaura.ext.activity

@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
    return calculateWindowSizeClass(activity = LocalContext.current.activity)
}
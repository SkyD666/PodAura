package com.skyd.anivu.ui.local

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController

val LocalNavController = compositionLocalOf<NavHostController> {
    error("LocalNavController not initialized!")
}
val LocalGlobalNavController = compositionLocalOf<NavHostController> {
    error("LocalGlobalNavController not initialized!")
}

val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> {
    error("LocalWindowSizeClass not initialized!")
}
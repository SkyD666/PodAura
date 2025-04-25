package com.skyd.anivu.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.skyd.anivu.ext.activity

@Composable
actual fun onEmptyPopBackStack(): () -> Unit {
    val context = LocalContext.current
    return {
        context.activity.finish()
    }
}
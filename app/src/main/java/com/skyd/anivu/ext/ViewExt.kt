package com.skyd.anivu.ext

import android.app.Activity
import android.view.View
import android.view.Window
import androidx.compose.ui.window.DialogWindowProvider

val View.activity: Activity
    get() = context.activity

val View.tryActivity: Activity?
    get() = context.tryActivity

val View.tryWindow: Window?
    get() = (parent as? DialogWindowProvider)?.window ?: context.tryWindow
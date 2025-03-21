package com.skyd.anivu.ui.component

import android.widget.Toast
import com.skyd.anivu.appContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val scope = CoroutineScope(Dispatchers.Main.immediate)

fun CharSequence.showToast(duration: Int = Toast.LENGTH_SHORT) {
    scope.launch {
        val toast = Toast.makeText(appContext, this@showToast, duration)
        toast.show()
    }
}
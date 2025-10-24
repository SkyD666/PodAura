package com.skyd.podaura.ui.component

import android.widget.Toast
import com.skyd.fundation.di.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val scope = CoroutineScope(Dispatchers.Main.immediate)

fun CharSequence.showToast(duration: Int = Toast.LENGTH_SHORT) {
    scope.launch {
        val toast = Toast.makeText(get(), this@showToast, duration)
        toast.show()
    }
}
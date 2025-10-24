package com.skyd.podaura.ui.component

import android.content.Context
import com.skyd.fundation.di.get
import com.skyd.podaura.ext.tickVibrate
import com.skyd.podaura.ext.vibrator

actual fun tickVibrate() {
    get<Context>().vibrator().tickVibrate()
}
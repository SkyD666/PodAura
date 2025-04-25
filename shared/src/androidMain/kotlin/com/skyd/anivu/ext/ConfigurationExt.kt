package com.skyd.anivu.ext

import android.content.res.Configuration


val Configuration.screenIsLand: Boolean
    get() = orientation == Configuration.ORIENTATION_LANDSCAPE
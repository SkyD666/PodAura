package com.skyd.podaura.util

import com.skyd.podaura.ext.currentTimeMillis
import kotlinx.datetime.Clock
import kotlin.random.Random


fun uniqueInt(): Int =
    (Clock.currentTimeMillis() + Random.nextInt() % 99999999).toInt()
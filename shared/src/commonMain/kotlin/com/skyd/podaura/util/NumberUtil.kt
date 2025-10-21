package com.skyd.podaura.util

import com.skyd.fundation.ext.currentTimeMillis
import kotlin.random.Random
import kotlin.time.Clock


fun uniqueInt(): Int =
    (Clock.currentTimeMillis() + Random.nextInt() % 99999999).toInt()
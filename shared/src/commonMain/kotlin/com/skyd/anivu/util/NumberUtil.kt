package com.skyd.anivu.util

import kotlinx.datetime.Clock
import kotlin.random.Random


fun uniqueInt(): Int =
    (Clock.System.now().toEpochMilliseconds() + Random.nextInt() % 99999999).toInt()
package com.skyd.podaura.ui.screen.about.update

import com.skyd.mvi.MviSingleEvent
import com.skyd.podaura.ext.currentTimeMillis
import kotlinx.datetime.Clock
import kotlin.random.Random

sealed interface UpdateEvent : MviSingleEvent {
    data class CheckError(
        val msg: String,
        private val random: Long = Random.nextLong() + Clock.currentTimeMillis(),
    ) : UpdateEvent

    data class CheckSuccess(private val random: Long = Random.nextLong() + Clock.currentTimeMillis()) :
        UpdateEvent
}

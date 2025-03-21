package com.skyd.anivu.ui.screen.about.update

import com.skyd.anivu.base.mvi.MviSingleEvent
import kotlin.random.Random

sealed interface UpdateEvent : MviSingleEvent {
    data class CheckError(
        val msg: String,
        private val random: Long = Random.nextLong() + System.currentTimeMillis(),
    ) : UpdateEvent

    data class CheckSuccess(private val random: Long = Random.nextLong() + System.currentTimeMillis()) :
        UpdateEvent
}

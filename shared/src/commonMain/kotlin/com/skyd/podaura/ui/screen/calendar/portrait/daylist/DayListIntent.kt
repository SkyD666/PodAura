package com.skyd.podaura.ui.screen.calendar.portrait.daylist

import com.skyd.mvi.MviIntent

sealed interface DayListIntent : MviIntent {
    data class Init(val day: Long) : DayListIntent
}
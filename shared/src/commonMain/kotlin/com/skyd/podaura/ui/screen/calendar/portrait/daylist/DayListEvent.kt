package com.skyd.podaura.ui.screen.calendar.portrait.daylist

import com.skyd.mvi.MviSingleEvent

sealed interface DayListEvent : MviSingleEvent {
    sealed interface InitResultEvent : DayListEvent {
        data class Failed(val msg: String) : InitResultEvent
    }
}
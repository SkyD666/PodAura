package com.skyd.podaura.ui.screen.calendar.daylist

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.calendar.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan

class DayListViewModel(
    private val calendarRepo: CalendarRepository
) : AbstractMviViewModel<DayListIntent, DayListState, DayListEvent>() {

    override val viewState: StateFlow<DayListState>

    init {
        val initialVS = DayListState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<DayListIntent.Init>().distinctUntilChanged(),
            intentFlow.filterNot { it is DayListIntent.Init }
        )
            .toArticlePartialStateChangeFlow()
            .debugLog("DayListPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<DayListPartialStateChange>.sendSingleEvent(): Flow<DayListPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is DayListPartialStateChange.Init.Failed ->
                    DayListEvent.InitResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<DayListIntent>.toArticlePartialStateChangeFlow(): Flow<DayListPartialStateChange> {
        return merge(
            filterIsInstance<DayListIntent.Init>().flatMapConcat { intent ->
                combine(
                    flowOf(
                        calendarRepo.requestArticlesInOneDay(day = intent.day)
                            .cachedIn(viewModelScope)
                    ),
                    calendarRepo.requestArticleHoursInOneDay(day = intent.day)
                ) { articleList, hours ->
                    DayListPartialStateChange.Init.Success(
                        articlePagingDataFlow = articleList,
                        hours = hours,
                    )
                }.startWith(DayListPartialStateChange.Init.Loading).catchMap {
                    DayListPartialStateChange.Init.Failed(it.message.toString())
                }
            },
        )
    }
}
package com.skyd.podaura.ui.screen.calendar.daylist

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow


internal sealed interface DayListPartialStateChange {
    fun reduce(oldState: DayListState): DayListState

    sealed interface Init : DayListPartialStateChange {
        override fun reduce(oldState: DayListState): DayListState {
            return when (this) {
                is Success -> oldState.copy(
                    articleListState = ArticleListState.Success(
                        articlePagingDataFlow = articlePagingDataFlow,
                        hours = hours,
                        loading = false,
                    ),
                )

                is Failed -> oldState.copy(
                    articleListState = ArticleListState.Failed(msg = msg, loading = false),
                )

                Loading -> oldState.copy(
                    articleListState = oldState.articleListState.let {
                        when (it) {
                            is ArticleListState.Failed -> it.copy(loading = true)
                            is ArticleListState.Init -> it.copy(loading = true)
                            is ArticleListState.Success -> it.copy(loading = true)
                        }
                    },
                )
            }
        }

        data class Success(
            val articlePagingDataFlow: Flow<PagingData<Any>>,
            val hours: List<Int>,
        ) : Init

        data class Failed(val msg: String) : Init
        data object Loading : Init
    }
}
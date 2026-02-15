package com.skyd.podaura.ui.screen.calendar.daylist

import androidx.paging.PagingData
import com.skyd.mvi.MviViewState
import kotlinx.coroutines.flow.Flow

data class DayListState(
    val articleListState: ArticleListState,
) : MviViewState {
    companion object {
        fun initial() = DayListState(
            articleListState = ArticleListState.Init(),
        )
    }
}

sealed class ArticleListState(open val loading: Boolean = false) {
    data class Success(
        val articlePagingDataFlow: Flow<PagingData<Any>>,
        val hours: List<Int>,
        override val loading: Boolean = false
    ) : ArticleListState()

    data class Init(override val loading: Boolean = false) : ArticleListState()
    data class Failed(val msg: String, override val loading: Boolean = false) :
        ArticleListState()
}
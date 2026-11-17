package com.skyd.podaura.ui.screen.search

import androidx.paging.PagingData
import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.feed.FeedViewBean
import kotlinx.coroutines.flow.Flow

data class SearchState(
    val feedSearchResultState: SearchResultState<FeedViewBean>,
    val articleSearchResultState: SearchResultState<ArticleWithFeed>,
    val editFeedUrl: String?,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = SearchState(
            feedSearchResultState = SearchResultState.Init,
            articleSearchResultState = SearchResultState.Init,
            editFeedUrl = null,
            loadingDialog = false,
        )
    }
}

sealed interface SearchResultState<out T : Any> {
    data class Success<T : Any>(val result: Flow<PagingData<T>>) : SearchResultState<T>
    data object Init : SearchResultState<Nothing>
    data object Loading : SearchResultState<Nothing>
    data class Failed(val msg: String) : SearchResultState<Nothing>
}

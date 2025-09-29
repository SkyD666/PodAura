package com.skyd.podaura.ui.screen.article

import androidx.paging.PagingData
import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.feed.FeedBean
import kotlinx.coroutines.flow.Flow

data class ArticleState(
    val articleFilterState: Int,
    val articleListState: ArticleListState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ArticleState(
            articleFilterState = FeedBean.DEFAULT_FILTER_MASK,
            articleListState = ArticleListState.Init(),
            loadingDialog = false,
        )
    }
}

sealed class ArticleListState(open val loading: Boolean = false) {
    data class Success(
        val articlePagingDataFlow: Flow<PagingData<ArticleWithFeed>>,
        override val loading: Boolean = false
    ) : ArticleListState()

    data class Init(override val loading: Boolean = false) : ArticleListState()
    data class Failed(val msg: String, override val loading: Boolean = false) :
        ArticleListState()
}
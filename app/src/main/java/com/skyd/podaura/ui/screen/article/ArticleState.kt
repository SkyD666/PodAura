package com.skyd.podaura.ui.screen.article

import androidx.paging.PagingData
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.repository.article.ArticleSort
import com.skyd.podaura.ui.mvi.MviViewState
import kotlinx.coroutines.flow.Flow

data class ArticleState(
    val articleFilterState: ArticleFilterState,
    val articleListState: ArticleListState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ArticleState(
            articleFilterState = ArticleFilterState(),
            articleListState = ArticleListState.Init(),
            loadingDialog = false,
        )
    }
}

data class ArticleFilterState(
    val favoriteFilter: Boolean? = null,
    val readFilter: Boolean? = null,
    val sortFilter: ArticleSort = ArticleSort.default,
) {
    val filterCount: Int
        get() = listOfNotNull(favoriteFilter, readFilter).size +
                if (sortFilter == ArticleSort.default) 0 else 1
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
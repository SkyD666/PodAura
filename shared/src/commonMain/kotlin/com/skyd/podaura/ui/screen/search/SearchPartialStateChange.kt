package com.skyd.podaura.ui.screen.search

import com.skyd.podaura.model.bean.article.ArticleDeleteResult
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.feed.FeedViewBean


internal sealed interface SearchPartialStateChange {
    fun reduce(oldState: SearchState): SearchState

    sealed interface LoadingDialog : SearchPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: SearchState) = oldState.copy(loadingDialog = true)
        }
    }

    data class FeedSearchResult(
        val state: SearchResultState<FeedViewBean>,
    ) : SearchPartialStateChange {
        override fun reduce(oldState: SearchState) = oldState.copy(feedSearchResultState = state)
    }

    data class ArticleSearchResult(
        val state: SearchResultState<ArticleWithFeed>,
    ) : SearchPartialStateChange {
        override fun reduce(oldState: SearchState) = oldState.copy(articleSearchResultState = state)
    }

    sealed interface FavoriteArticle : SearchPartialStateChange {
        override fun reduce(oldState: SearchState): SearchState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : FavoriteArticle
        data class Failed(val msg: String) : FavoriteArticle
    }

    sealed interface ReadArticle : SearchPartialStateChange {
        override fun reduce(oldState: SearchState): SearchState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : ReadArticle
        data class Failed(val msg: String) : ReadArticle
    }

    sealed interface DeleteArticle : SearchPartialStateChange {
        override fun reduce(oldState: SearchState): SearchState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val result: ArticleDeleteResult) : DeleteArticle
        data class Failed(val msg: String) : DeleteArticle
    }

    sealed interface UpdateQuery : SearchPartialStateChange {
        override fun reduce(oldState: SearchState): SearchState = oldState

        data object Success : UpdateQuery
    }

    sealed interface UpdateSort : SearchPartialStateChange {
        override fun reduce(oldState: SearchState): SearchState = oldState

        data object Success : UpdateSort
    }

    data class OnEditFeedDialog(val feedUrl: String?) : SearchPartialStateChange {
        override fun reduce(oldState: SearchState): SearchState {
            return oldState.copy(editFeedUrl = feedUrl)
        }
    }
}

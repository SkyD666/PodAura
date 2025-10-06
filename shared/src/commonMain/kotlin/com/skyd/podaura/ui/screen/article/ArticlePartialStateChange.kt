package com.skyd.podaura.ui.screen.article

import androidx.paging.PagingData
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import kotlinx.coroutines.flow.Flow


internal sealed interface ArticlePartialStateChange {
    fun reduce(oldState: ArticleState): ArticleState

    sealed interface LoadingDialog : ArticlePartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: ArticleState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface Init : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return when (this) {
                is Success -> oldState.copy(
                    articleFilterState = filterMask,
                    articleListState = ArticleListState.Success(
                        articlePagingDataFlow = articlePagingDataFlow,
                        loading = false,
                    ),
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    articleListState = ArticleListState.Failed(msg = msg, loading = false),
                    loadingDialog = false,
                )

                Loading -> oldState.copy(
                    articleListState = oldState.articleListState.let {
                        when (it) {
                            is ArticleListState.Failed -> it.copy(loading = true)
                            is ArticleListState.Init -> it.copy(loading = true)
                            is ArticleListState.Success -> it.copy(loading = true)
                        }
                    },
                    loadingDialog = false,
                )
            }
        }

        data class Success(
            val articlePagingDataFlow: Flow<PagingData<ArticleWithFeed>>,
            val filterMask: Int,
        ) : Init

        data class Failed(val msg: String) : Init
        data object Loading : Init
    }

    sealed interface RefreshArticleList : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return when (this) {
                is Success,
                is Failed -> {
                    val articleListState = oldState.articleListState
                    oldState.copy(
                        articleListState = when (articleListState) {
                            is ArticleListState.Init -> articleListState.copy(loading = false)
                            is ArticleListState.Failed -> articleListState.copy(loading = false)
                            is ArticleListState.Success -> articleListState.copy(
                                articlePagingDataFlow = articleListState.articlePagingDataFlow,
                                loading = false,
                            )
                        },
                        loadingDialog = false,
                    )
                }

                is Loading -> oldState.copy(
                    articleListState = oldState.articleListState.let {
                        when (it) {
                            is ArticleListState.Failed -> it.copy(loading = true)
                            is ArticleListState.Init -> it.copy(loading = true)
                            is ArticleListState.Success -> it.copy(loading = true)
                        }
                    },
                    loadingDialog = false,
                )
            }
        }

        data object Success : RefreshArticleList
        data object Loading : RefreshArticleList
        data class Failed(val msg: String) : RefreshArticleList
    }

    sealed interface FavoriteArticle : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
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

    sealed interface ReadArticle : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
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

    sealed interface DeleteArticle : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : DeleteArticle
        data class Failed(val msg: String) : DeleteArticle
    }

    sealed interface UpdateFilter : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return when (this) {
                is Success -> oldState.copy(
                    articleFilterState = filterMask,
                    loadingDialog = false,
                )
            }
        }

        data class Success(val filterMask: Int) : UpdateFilter
    }
}
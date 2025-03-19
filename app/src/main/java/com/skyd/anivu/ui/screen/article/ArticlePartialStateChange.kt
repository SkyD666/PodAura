package com.skyd.anivu.ui.screen.article

import androidx.paging.PagingData
import com.skyd.anivu.model.bean.article.ArticleWithFeed
import com.skyd.anivu.model.repository.article.ArticleSort
import kotlinx.coroutines.flow.Flow


internal sealed interface ArticlePartialStateChange {
    fun reduce(oldState: ArticleState): ArticleState

    sealed interface LoadingDialog : ArticlePartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: ArticleState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface ArticleList : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return when (this) {
                is Success -> oldState.copy(
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

        data class Success(val articlePagingDataFlow: Flow<PagingData<ArticleWithFeed>>) :
            ArticleList

        data class Failed(val msg: String) : ArticleList
        data object Loading : ArticleList
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

    sealed interface FavoriteFilterArticle : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return when (this) {
                is Success -> oldState.copy(
                    articleFilterState = oldState.articleFilterState.copy(
                        favoriteFilter = favoriteFilter,
                    ),
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val favoriteFilter: Boolean?) : FavoriteFilterArticle
        data class Failed(val msg: String) : FavoriteFilterArticle
    }

    sealed interface ReadFilterArticle : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return when (this) {
                is Success -> oldState.copy(
                    articleFilterState = oldState.articleFilterState.copy(
                        readFilter = readFilter,
                    ),
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val readFilter: Boolean?) : ReadFilterArticle
        data class Failed(val msg: String) : ReadFilterArticle
    }

    sealed interface UpdateSort : ArticlePartialStateChange {
        override fun reduce(oldState: ArticleState): ArticleState {
            return when (this) {
                is Success -> oldState.copy(
                    articleFilterState = oldState.articleFilterState.copy(
                        sortFilter = sortFilter
                    ),
                    loadingDialog = false,
                )
            }
        }

        data class Success(val sortFilter: ArticleSort) : UpdateSort
    }
}
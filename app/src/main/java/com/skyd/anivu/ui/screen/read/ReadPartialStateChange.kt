package com.skyd.anivu.ui.screen.read

import com.skyd.anivu.model.bean.article.ArticleWithFeed


internal sealed interface ReadPartialStateChange {
    fun reduce(oldState: ReadState): ReadState

    sealed interface LoadingDialog : ReadPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: ReadState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface ArticleResult : ReadPartialStateChange {
        override fun reduce(oldState: ReadState): ReadState {
            return when (this) {
                is Success -> oldState.copy(
                    articleState = ArticleState.Success(article = article),
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    articleState = ArticleState.Failed(msg = msg),
                    loadingDialog = false,
                )

                Loading -> oldState.copy(
                    articleState = ArticleState.Loading,
                    loadingDialog = false,
                )
            }
        }

        data class Success(val article: ArticleWithFeed) : ArticleResult
        data class Failed(val msg: String) : ArticleResult
        data object Loading : ArticleResult
    }

    sealed interface FavoriteArticle : ReadPartialStateChange {
        override fun reduce(oldState: ReadState): ReadState {
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

    sealed interface ReadArticle : ReadPartialStateChange {
        override fun reduce(oldState: ReadState): ReadState {
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

    sealed interface DownloadImage : ReadPartialStateChange {
        override fun reduce(oldState: ReadState): ReadState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val url: String) : DownloadImage
        data class Failed(val msg: String) : DownloadImage
    }

    sealed interface ShareImage : ReadPartialStateChange {
        override fun reduce(oldState: ReadState): ReadState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data object Success : ShareImage
        data class Failed(val msg: String) : ShareImage
    }

    sealed interface CopyImage : ReadPartialStateChange {
        override fun reduce(oldState: ReadState): ReadState {
            return when (this) {
                is Success,
                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val url: String) : CopyImage
        data class Failed(val msg: String) : CopyImage
    }
}

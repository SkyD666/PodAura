package com.skyd.podaura.ui.screen.read

import com.skyd.podaura.model.bean.article.ArticleWithFeed


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
                    articleState = ArticleState.Success(
                        article = article,
                        linkedContent = linkedContent,
                    ),
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

        data class Success(
            val article: ArticleWithFeed,
            val linkedContent: String,
        ) : ArticleResult
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

    sealed interface PlayTimestamp : ReadPartialStateChange {
        override fun reduce(oldState: ReadState): ReadState = oldState

        data class OpenPlayer(
            val articleId: String,
            val mediaUrl: String,
            val positionSeconds: Long,
        ) : PlayTimestamp

        data object MediaNotExists : PlayTimestamp
    }

}

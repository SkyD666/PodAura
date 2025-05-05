package com.skyd.podaura.ui.screen.read

import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.ui.mvi.MviViewState

data class ReadState(
    val articleState: ArticleState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ReadState(
            articleState = ArticleState.Init,
            loadingDialog = true,
        )
    }
}

sealed interface ArticleState {
    data class Success(val article: ArticleWithFeed) : ArticleState
    data object Init : ArticleState
    data object Loading : ArticleState
    data class Failed(val msg: String) : ArticleState
}
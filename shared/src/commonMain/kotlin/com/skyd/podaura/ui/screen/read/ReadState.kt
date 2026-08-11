package com.skyd.podaura.ui.screen.read

import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.repository.fullcontent.FullContent

data class ReadState(
    val articleState: ArticleState,
    val loadingDialog: Boolean,
    val fullContentLoading: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ReadState(
            articleState = ArticleState.Init,
            loadingDialog = true,
            fullContentLoading = false,
        )
    }
}

enum class ReadContentSource {
    Feed,
    FullText,
}

sealed interface ArticleState {
    data class Success(
        val article: ArticleWithFeed,
        val feedContent: String,
        val fullContent: FullContent? = null,
        val contentSource: ReadContentSource = ReadContentSource.Feed,
    ) : ArticleState {
        val displayedContent: String
            get() = if (contentSource == ReadContentSource.FullText) {
                fullContent?.html ?: feedContent
            } else {
                feedContent
            }

        val displayedSourceUrl: String?
            get() = if (contentSource == ReadContentSource.FullText) {
                fullContent?.sourceUrl
            } else {
                article.articleWithEnclosure.article.link
            }
    }

    data object Init : ArticleState
    data object Loading : ArticleState
    data class Failed(val msg: String) : ArticleState
}

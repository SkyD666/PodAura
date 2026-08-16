package com.skyd.podaura.ui.screen.read

import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.translation.TranslationError
import com.skyd.podaura.model.bean.translation.TranslationProfile
import com.skyd.podaura.model.repository.fullcontent.FullContent

data class ReadState(
    val articleState: ArticleState,
    val loadingDialog: Boolean,
    val fullContentLoading: Boolean,
    val translationState: TranslationState = TranslationState(),
    val translationProfiles: List<TranslationProfile> = emptyList(),
) : MviViewState {
    companion object {
        fun initial() = ReadState(
            articleState = ArticleState.Init,
            loadingDialog = true,
            fullContentLoading = false,
            translationState = TranslationState(),
            translationProfiles = emptyList(),
        )
    }
}

data class TranslationState(
    val status: TranslationStatus = TranslationStatus.Idle,
    val contentSource: ReadContentSource? = null,
    val profileId: String? = null,
    val sourceLanguage: String? = null,
    val targetLanguage: String? = null,
    val translatedTitle: String? = null,
    val translatedHtml: String? = null,
    val displayMode: TranslationDisplayMode = TranslationDisplayMode.Original,
)

enum class TranslationDisplayMode {
    Original,
    Translated,
}

sealed interface TranslationStatus {
    data object Idle : TranslationStatus
    data object Loading : TranslationStatus
    data object Success : TranslationStatus
    data class Failed(val error: TranslationError) : TranslationStatus
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

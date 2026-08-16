package com.skyd.podaura.ui.screen.read

import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.translation.ArticleTranslation
import com.skyd.podaura.model.bean.translation.TranslationError
import com.skyd.podaura.model.bean.translation.TranslationProfile
import com.skyd.podaura.model.repository.fullcontent.FullContent


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
                is Success -> {
                    val previous = oldState.articleState as? ArticleState.Success
                    val sameArticle = previous
                        ?.article?.articleWithEnclosure?.article?.articleId ==
                            article.articleWithEnclosure.article.articleId
                    oldState.copy(
                        articleState = ArticleState.Success(
                            article = article,
                            feedContent = feedContent,
                            fullContent = previous?.fullContent.takeIf { sameArticle },
                            contentSource = previous?.contentSource
                                ?.takeIf { sameArticle }
                                ?: ReadContentSource.Feed,
                        ),
                        loadingDialog = false,
                        translationState = if (sameArticle) oldState.translationState
                        else TranslationState(),
                    )
                }

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
            val feedContent: String,
        ) : ArticleResult

        data class Failed(val msg: String) : ArticleResult
        data object Loading : ArticleResult
    }

    sealed interface FullContentResult : ReadPartialStateChange {
        override fun reduce(oldState: ReadState): ReadState = when (this) {
            Loading -> oldState.copy(fullContentLoading = true)
            is Failed -> oldState.copy(fullContentLoading = false)
            is Success -> {
                val articleState = oldState.articleState as? ArticleState.Success
                    ?: return oldState.copy(fullContentLoading = false)
                oldState.copy(
                    articleState = articleState.copy(
                        fullContent = content,
                        contentSource = ReadContentSource.FullText,
                    ),
                    fullContentLoading = false,
                    translationState = TranslationState(
                        profileId = oldState.translationState.profileId,
                        targetLanguage = oldState.translationState.targetLanguage,
                    ),
                )
            }
        }

        data object Loading : FullContentResult
        data class Success(val content: FullContent) : FullContentResult
        data class Failed(val msg: String) : FullContentResult
    }

    data class ContentSourceChanged(
        val source: ReadContentSource,
    ) : ReadPartialStateChange {
        override fun reduce(oldState: ReadState): ReadState {
            val articleState = oldState.articleState as? ArticleState.Success ?: return oldState
            if (source == ReadContentSource.FullText && articleState.fullContent == null) return oldState
            return oldState.copy(
                articleState = articleState.copy(contentSource = source),
                translationState = TranslationState(
                    profileId = oldState.translationState.profileId,
                    targetLanguage = oldState.translationState.targetLanguage,
                ),
            )
        }
    }

    data class TranslationProfilesChanged(
        val profiles: List<TranslationProfile>,
    ) : ReadPartialStateChange {
        override fun reduce(oldState: ReadState): ReadState = oldState.copy(
            translationProfiles = profiles,
        )
    }

    sealed interface TranslationResult : ReadPartialStateChange {
        data class Loading(
            val contentSource: ReadContentSource,
            val profileId: String,
            val targetLanguage: String,
        ) : TranslationResult {
            override fun reduce(oldState: ReadState): ReadState = oldState.copy(
                translationState = TranslationState(
                    status = TranslationStatus.Loading,
                    contentSource = contentSource,
                    profileId = profileId,
                    targetLanguage = targetLanguage,
                    displayMode = TranslationDisplayMode.Original,
                )
            )
        }

        data class Success(
            val contentSource: ReadContentSource,
            val profileId: String,
            val targetLanguage: String,
            val translation: ArticleTranslation,
        ) : TranslationResult {
            override fun reduce(oldState: ReadState): ReadState {
                val article = oldState.articleState as? ArticleState.Success ?: return oldState
                if (article.contentSource != contentSource) return oldState
                return oldState.copy(
                    translationState = TranslationState(
                        status = TranslationStatus.Success,
                        contentSource = contentSource,
                        profileId = profileId,
                        sourceLanguage = translation.detectedSourceLanguage,
                        targetLanguage = targetLanguage,
                        translatedTitle = translation.title,
                        translatedHtml = translation.html,
                        displayMode = TranslationDisplayMode.Translated,
                    )
                )
            }
        }

        data class Failed(
            val contentSource: ReadContentSource,
            val profileId: String,
            val targetLanguage: String,
            val error: TranslationError,
        ) : TranslationResult {
            override fun reduce(oldState: ReadState): ReadState = oldState.copy(
                translationState = TranslationState(
                    status = TranslationStatus.Failed(error),
                    contentSource = contentSource,
                    profileId = profileId,
                    targetLanguage = targetLanguage,
                    displayMode = TranslationDisplayMode.Original,
                )
            )
        }

        data class CacheMiss(
            val contentSource: ReadContentSource,
            val profileId: String?,
            val targetLanguage: String?,
        ) : TranslationResult {
            override fun reduce(oldState: ReadState): ReadState {
                val article = oldState.articleState as? ArticleState.Success ?: return oldState
                if (article.contentSource != contentSource) return oldState
                return oldState.copy(
                    translationState = TranslationState(
                        contentSource = contentSource,
                        profileId = profileId,
                        targetLanguage = targetLanguage,
                    )
                )
            }
        }

        data object Cancelled : TranslationResult {
            override fun reduce(oldState: ReadState): ReadState {
                if (oldState.translationState.status !is TranslationStatus.Loading) {
                    return oldState
                }
                return oldState.copy(
                    translationState = TranslationState(
                        contentSource = oldState.translationState.contentSource,
                        profileId = oldState.translationState.profileId,
                        targetLanguage = oldState.translationState.targetLanguage,
                    )
                )
            }
        }

        data object CacheCleared : TranslationResult {
            override fun reduce(oldState: ReadState): ReadState = oldState.copy(
                translationState = TranslationState(
                    profileId = oldState.translationState.profileId,
                    targetLanguage = oldState.translationState.targetLanguage,
                )
            )
        }
    }

    data class TranslationDisplayModeChanged(
        val mode: TranslationDisplayMode,
    ) : ReadPartialStateChange {
        override fun reduce(oldState: ReadState): ReadState {
            if (mode == TranslationDisplayMode.Translated &&
                oldState.translationState.status !is TranslationStatus.Success
            ) return oldState
            return oldState.copy(
                translationState = oldState.translationState.copy(displayMode = mode)
            )
        }
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

package com.skyd.podaura.ui.screen.read

import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.ifNullOfBlank
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.bean.translation.ArticleTranslationResult
import com.skyd.podaura.model.bean.translation.TranslationContentSource
import com.skyd.podaura.model.bean.translation.TranslationError
import com.skyd.podaura.model.repository.ReadRepository
import com.skyd.podaura.model.repository.article.IArticleRepository
import com.skyd.podaura.model.repository.fullcontent.IFullContentRepository
import com.skyd.podaura.model.repository.translation.TranslationProfileRepository
import com.skyd.podaura.model.repository.translation.TranslationRepository
import com.skyd.podaura.ui.component.webview.linkifyTimestamps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.read_screen_article_id_illegal
import podaura.shared.generated.resources.read_screen_full_content_failed
import kotlin.coroutines.cancellation.CancellationException

class ReadViewModel(
    private val readRepo: ReadRepository,
    private val articleRepo: IArticleRepository,
    private val fullContentRepo: IFullContentRepository,
    private val translationRepo: TranslationRepository,
    private val translationProfileRepo: TranslationProfileRepository,
) : AbstractMviViewModel<ReadIntent, ReadState, ReadEvent>() {

    override val viewState: StateFlow<ReadState>

    init {
        val initialVS = ReadState.initial()

        val intentChanges = merge(
            intentFlow.filterIsInstance<ReadIntent.Init>().take(1),
            intentFlow.filterNot { it is ReadIntent.Init }
        )
            .toReadPartialStateChangeFlow()

        viewState = merge(
            intentChanges,
            translationProfileRepo.observeEnabled().map {
                ReadPartialStateChange.TranslationProfilesChanged(it)
            },
        )
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .toState(initialVS)
    }

    private fun Flow<ReadPartialStateChange>.sendSingleEvent(): Flow<ReadPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is ReadPartialStateChange.FavoriteArticle.Failed ->
                    ReadEvent.FavoriteArticleResultEvent.Failed(change.msg)

                is ReadPartialStateChange.ReadArticle.Failed ->
                    ReadEvent.ReadArticleResultEvent.Failed(change.msg)

                is ReadPartialStateChange.FullContentResult.Failed ->
                    ReadEvent.FullContentResultEvent.Failed(change.msg)

                is ReadPartialStateChange.PlayTimestamp.OpenPlayer ->
                    ReadEvent.PlayTimestampResultEvent.OpenPlayer(
                        articleId = change.articleId,
                        mediaUrl = change.mediaUrl,
                        positionSeconds = change.positionSeconds,
                    )

                ReadPartialStateChange.PlayTimestamp.MediaNotExists ->
                    ReadEvent.PlayTimestampResultEvent.MediaNotExists

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<ReadIntent>.toReadPartialStateChangeFlow(): Flow<ReadPartialStateChange> {
        return merge(
            filterIsInstance<ReadIntent.Init>().flatMapConcat { intent ->
                articleRepo.readArticle(intent.articleId, read = true).flatMapConcat {
                    readRepo.requestArticleWithFeed(intent.articleId)
                }.flatMapConcat {
                    flow {
                        if (it == null) {
                            emit(
                                ReadPartialStateChange.ArticleResult.Failed(
                                    getString(Res.string.read_screen_article_id_illegal)
                                )
                            )
                        } else {
                            val article = it.articleWithEnclosure.article
                            val feedContent = withContext(Dispatchers.Default) {
                                linkifyTimestamps(
                                    article.content.ifNullOfBlank { article.description.orEmpty() }
                                )
                            }
                            emit(
                                ReadPartialStateChange.ArticleResult.Success(
                                    article = it,
                                    feedContent = feedContent,
                                )
                            )
                            val refreshedState = viewState.value
                            val refreshedArticle = refreshedState.articleState
                                    as? ArticleState.Success
                            emit(
                                findCachedTranslation(
                                    article = refreshedArticle ?: ArticleState.Success(
                                        article = it,
                                        feedContent = feedContent,
                                    ),
                                    source = refreshedArticle?.contentSource
                                        ?: ReadContentSource.Feed,
                                    profileId = refreshedState.translationState.profileId,
                                    targetLanguage = refreshedState.translationState.targetLanguage,
                                )
                            )
                        }
                    }
                }.startWith(ReadPartialStateChange.ArticleResult.Loading)
            },
            filterIsInstance<ReadIntent.Favorite>().flatMapConcat { intent ->
                articleRepo.favoriteArticle(intent.articleId, intent.favorite).map {
                    ReadPartialStateChange.FavoriteArticle.Success
                }.startWith(ReadPartialStateChange.LoadingDialog.Show).catchMap {
                    ReadPartialStateChange.FavoriteArticle.Failed(it.message.toString())
                }
            },
            filterIsInstance<ReadIntent.Read>().flatMapConcat { intent ->
                articleRepo.readArticle(intent.articleId, intent.read).map {
                    ReadPartialStateChange.ReadArticle.Success
                }.startWith(ReadPartialStateChange.LoadingDialog.Show).catchMap {
                    ReadPartialStateChange.ReadArticle.Failed(it.message.toString())
                }
            },
            filterIsInstance<ReadIntent.FetchFullContent>().flatMapConcat { intent ->
                flow { emit(fullContentRepo.fetch(intent.url)) }.flatMapConcat { content ->
                    flow {
                        val linkedHtml = withContext(Dispatchers.Default) {
                            linkifyTimestamps(content.html)
                        }
                        val linkedContent = content.copy(html = linkedHtml)
                        emit(
                            ReadPartialStateChange.FullContentResult.Success(linkedContent)
                        )
                        val article = viewState.value.articleState as? ArticleState.Success
                        if (article != null) {
                            val translation = viewState.value.translationState
                            emit(
                                findCachedTranslation(
                                    article = article.copy(
                                        fullContent = linkedContent,
                                        contentSource = ReadContentSource.FullText,
                                    ),
                                    source = ReadContentSource.FullText,
                                    profileId = translation.profileId,
                                    targetLanguage = translation.targetLanguage,
                                )
                            )
                        }
                    }
                }.startWith(ReadPartialStateChange.FullContentResult.Loading).catch {
                    if (it is CancellationException) throw it
                    emit(
                        ReadPartialStateChange.FullContentResult.Failed(
                            getString(Res.string.read_screen_full_content_failed)
                        )
                    )
                }
            },
            filter {
                it is ReadIntent.Translate ||
                        it is ReadIntent.CancelTranslation ||
                        it is ReadIntent.SelectContentSource ||
                        it is ReadIntent.FetchFullContent ||
                        it is ReadIntent.RemoveTranslationCache
            }.flatMapLatest { intent ->
                when (intent) {
                    is ReadIntent.Translate -> translate(intent)
                    is ReadIntent.SelectContentSource -> selectContentSource(intent.source)
                    ReadIntent.RemoveTranslationCache -> flow {
                        translationRepo.clearCache()
                        emit(ReadPartialStateChange.TranslationResult.CacheCleared)
                    }

                    ReadIntent.CancelTranslation,
                    is ReadIntent.FetchFullContent -> flow {
                        emit(ReadPartialStateChange.TranslationResult.Cancelled)
                    }

                    else -> flow { }
                }
            },
            filterIsInstance<ReadIntent.SelectTranslationDisplayMode>().map { intent ->
                ReadPartialStateChange.TranslationDisplayModeChanged(intent.mode)
            },
            filterIsInstance<ReadIntent.PlayTimestamp>().map { intent ->
                intent.mediaUrl?.let { mediaUrl ->
                    ReadPartialStateChange.PlayTimestamp.OpenPlayer(
                        articleId = intent.articleId,
                        mediaUrl = mediaUrl,
                        positionSeconds = intent.positionSeconds,
                    )
                } ?: ReadPartialStateChange.PlayTimestamp.MediaNotExists
            },
        )
    }

    private fun translate(intent: ReadIntent.Translate): Flow<ReadPartialStateChange> = flow {
        val article = viewState.value.articleState as? ArticleState.Success
        if (article == null) {
            emit(
                ReadPartialStateChange.TranslationResult.Failed(
                    contentSource = ReadContentSource.Feed,
                    profileId = intent.profileId,
                    targetLanguage = intent.targetLanguage,
                    error = TranslationError.InvalidConfiguration,
                )
            )
            return@flow
        }
        val source = article.contentSource
        emit(
            ReadPartialStateChange.TranslationResult.Loading(
                contentSource = source,
                profileId = intent.profileId,
                targetLanguage = intent.targetLanguage,
            )
        )
        when (val result = translationRepo.translate(
            articleId = article.article.articleWithEnclosure.article.articleId,
            contentSource = source.toTranslationContentSource(),
            title = article.article.articleWithEnclosure.article.title,
            html = article.contentFor(source) ?: return@flow,
            profileId = intent.profileId,
            targetLanguage = intent.targetLanguage,
        )) {
            is ArticleTranslationResult.Success -> emit(
                ReadPartialStateChange.TranslationResult.Success(
                    contentSource = source,
                    profileId = intent.profileId,
                    targetLanguage = intent.targetLanguage,
                    translation = result.translation,
                )
            )

            is ArticleTranslationResult.Failure -> emit(
                ReadPartialStateChange.TranslationResult.Failed(
                    contentSource = source,
                    profileId = intent.profileId,
                    targetLanguage = intent.targetLanguage,
                    error = result.error,
                )
            )
        }
    }.catch {
        if (it is CancellationException) throw it
        val source = (viewState.value.articleState as? ArticleState.Success)
            ?.contentSource ?: ReadContentSource.Feed
        emit(
            ReadPartialStateChange.TranslationResult.Failed(
                contentSource = source,
                profileId = intent.profileId,
                targetLanguage = intent.targetLanguage,
                error = TranslationError.NetworkUnavailable,
            )
        )
    }

    private fun selectContentSource(
        source: ReadContentSource,
    ): Flow<ReadPartialStateChange> = flow {
        val article = viewState.value.articleState as? ArticleState.Success ?: return@flow
        if (article.contentFor(source) == null) return@flow
        val previousTranslation = viewState.value.translationState
        emit(ReadPartialStateChange.ContentSourceChanged(source))
        emit(
            findCachedTranslation(
                article = article.copy(contentSource = source),
                source = source,
                profileId = previousTranslation.profileId,
                targetLanguage = previousTranslation.targetLanguage,
            )
        )
    }

    private suspend fun findCachedTranslation(
        article: ArticleState.Success,
        source: ReadContentSource,
        profileId: String? = null,
        targetLanguage: String? = null,
    ): ReadPartialStateChange.TranslationResult {
        val requestedProfile = profileId?.let { translationProfileRepo.find(it) }
            ?.takeIf { it.enabled }
        val profile = requestedProfile ?: translationProfileRepo.findDefault()
        val language = targetLanguage ?: profile?.targetLanguage
        if (profile == null || language == null) {
            return ReadPartialStateChange.TranslationResult.CacheMiss(
                contentSource = source,
                profileId = profile?.id,
                targetLanguage = language,
            )
        }
        val html = article.contentFor(source)
            ?: return ReadPartialStateChange.TranslationResult.CacheMiss(
                source,
                profile.id,
                language,
            )
        val translation = translationRepo.findCached(
            articleId = article.article.articleWithEnclosure.article.articleId,
            contentSource = source.toTranslationContentSource(),
            title = article.article.articleWithEnclosure.article.title,
            html = html,
            profile = profile,
            targetLanguage = language,
        ) ?: return ReadPartialStateChange.TranslationResult.CacheMiss(
            source,
            profile.id,
            language,
        )
        return ReadPartialStateChange.TranslationResult.Success(
            contentSource = source,
            profileId = profile.id,
            targetLanguage = language,
            translation = translation,
        )
    }

    private fun ArticleState.Success.contentFor(source: ReadContentSource): String? =
        when (source) {
            ReadContentSource.Feed -> feedContent
            ReadContentSource.FullText -> fullContent?.html
        }

    private fun ReadContentSource.toTranslationContentSource(): TranslationContentSource =
        when (this) {
            ReadContentSource.Feed -> TranslationContentSource.Feed
            ReadContentSource.FullText -> TranslationContentSource.FullText
        }
}

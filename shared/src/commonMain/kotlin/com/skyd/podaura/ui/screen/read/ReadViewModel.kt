package com.skyd.podaura.ui.screen.read

import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.ifNullOfBlank
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.ReadRepository
import com.skyd.podaura.model.repository.article.IArticleRepository
import com.skyd.podaura.model.repository.fullcontent.IFullContentRepository
import com.skyd.podaura.ui.component.webview.linkifyTimestamps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.read_screen_article_id_illegal
import podaura.shared.generated.resources.read_screen_full_content_failed

class ReadViewModel(
    private val readRepo: ReadRepository,
    private val articleRepo: IArticleRepository,
    private val fullContentRepo: IFullContentRepository,
) : AbstractMviViewModel<ReadIntent, ReadState, ReadEvent>() {

    override val viewState: StateFlow<ReadState>

    init {
        val initialVS = ReadState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<ReadIntent.Init>().take(1),
            intentFlow.filterNot { it is ReadIntent.Init }
        )
            .toReadPartialStateChangeFlow()
            .debugLog("ReadPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
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
                }.map {
                    if (it == null) {
                        ReadPartialStateChange.ArticleResult.Failed(
                            getString(Res.string.read_screen_article_id_illegal)
                        )
                    } else {
                        val article = it.articleWithEnclosure.article
                        val feedContent = withContext(Dispatchers.Default) {
                            linkifyTimestamps(
                                article.content.ifNullOfBlank { article.description.orEmpty() }
                            )
                        }
                        ReadPartialStateChange.ArticleResult.Success(
                            article = it,
                            feedContent = feedContent,
                        )
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
                flow { emit(fullContentRepo.fetch(intent.url)) }.map { content ->
                    val linkedHtml = withContext(Dispatchers.Default) {
                        linkifyTimestamps(content.html)
                    }
                    ReadPartialStateChange.FullContentResult.Success(
                        content.copy(html = linkedHtml)
                    )
                }.startWith(ReadPartialStateChange.FullContentResult.Loading).catch {
                    emit(
                        ReadPartialStateChange.FullContentResult.Failed(
                            getString(Res.string.read_screen_full_content_failed)
                        )
                    )
                }
            },
            filterIsInstance<ReadIntent.SelectContentSource>().map { intent ->
                ReadPartialStateChange.ContentSourceChanged(intent.source)
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
}

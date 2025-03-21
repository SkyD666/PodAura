package com.skyd.anivu.ui.screen.read

import com.skyd.anivu.R
import com.skyd.anivu.appContext
import com.skyd.anivu.base.mvi.AbstractMviViewModel
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.model.repository.ReadRepository
import com.skyd.anivu.model.repository.article.IArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@HiltViewModel
class ReadViewModel @Inject constructor(
    private val readRepo: ReadRepository,
    private val articleRepo: IArticleRepository,
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
                    ReadEvent.FavoriteArticleResultEvent.Failed(change.msg)

                is ReadPartialStateChange.ShareImage.Failed ->
                    ReadEvent.ShareImageResultEvent.Failed(change.msg)

                is ReadPartialStateChange.CopyImage.Success ->
                    ReadEvent.CopyImageResultEvent.Success(change.url)

                is ReadPartialStateChange.CopyImage.Failed ->
                    ReadEvent.CopyImageResultEvent.Failed(change.msg)

                is ReadPartialStateChange.DownloadImage.Success ->
                    ReadEvent.DownloadImageResultEvent.Success(change.url)

                is ReadPartialStateChange.DownloadImage.Failed ->
                    ReadEvent.DownloadImageResultEvent.Failed(change.msg)

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
                            appContext.getString(R.string.read_screen_article_id_illegal)
                        )
                    } else {
                        ReadPartialStateChange.ArticleResult.Success(article = it)
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
            filterIsInstance<ReadIntent.ShareImage>().flatMapConcat { intent ->
                readRepo.shareImage(intent.url).map {
                    ReadPartialStateChange.ShareImage.Success
                }.startWith(ReadPartialStateChange.LoadingDialog.Show).catchMap {
                    ReadPartialStateChange.ShareImage.Failed(it.message.toString())
                }
            },
            filterIsInstance<ReadIntent.CopyImage>().flatMapConcat { intent ->
                readRepo.copyImage(intent.url).map {
                    ReadPartialStateChange.CopyImage.Success(intent.url)
                }.startWith(ReadPartialStateChange.LoadingDialog.Show).catchMap {
                    ReadPartialStateChange.CopyImage.Failed(it.message.toString())
                }
            },
            filterIsInstance<ReadIntent.DownloadImage>().flatMapConcat { intent ->
                readRepo.downloadImage(intent.url, intent.title).map {
                    ReadPartialStateChange.DownloadImage.Success(intent.url)
                }.startWith(ReadPartialStateChange.LoadingDialog.Show).catchMap {
                    ReadPartialStateChange.DownloadImage.Failed(it.message.toString())
                }
            },
        )
    }
}
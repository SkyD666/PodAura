package com.skyd.podaura.ui.screen.article

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.flattenFirst
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.BatchProgress
import com.skyd.podaura.model.repository.article.ArticleRepository
import com.skyd.podaura.model.repository.playlist.AddToPlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan

class ArticleViewModel(
    private val articleRepo: ArticleRepository,
    private val addToPlaylistRepo: AddToPlaylistRepository,
) : AbstractMviViewModel<ArticleIntent, ArticleState, ArticleEvent>() {

    override val viewState: StateFlow<ArticleState>

    init {
        val initialVS = ArticleState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<ArticleIntent.Init>().distinctUntilChanged(),
            intentFlow.filterNot { it is ArticleIntent.Init }
        )
            .toArticlePartialStateChangeFlow()
            .debugLog("ArticlePartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<ArticlePartialStateChange>.sendSingleEvent(): Flow<ArticlePartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is ArticlePartialStateChange.Selection.Downloaded ->
                    ArticleEvent.SelectionResultEvent.Downloaded(change.result)

                is ArticlePartialStateChange.Selection.Completed ->
                    ArticleEvent.SelectionResultEvent.Completed(change.result)

                is ArticlePartialStateChange.Selection.Cancelled ->
                    ArticleEvent.SelectionResultEvent.Cancelled(change.progress)

                is ArticlePartialStateChange.Selection.PlaylistPrepared ->
                    ArticleEvent.SelectionResultEvent.PlaylistPrepared(change.result)

                is ArticlePartialStateChange.Selection.Failed ->
                    ArticleEvent.SelectionResultEvent.Failed(change.msg)

                is ArticlePartialStateChange.Init.Failed ->
                    ArticleEvent.InitArticleListResultEvent.Failed(change.msg)

                is ArticlePartialStateChange.RefreshArticleList.Failed ->
                    ArticleEvent.RefreshArticleListResultEvent.Failed(change.msg)

                is ArticlePartialStateChange.FavoriteArticle.Failed ->
                    ArticleEvent.FavoriteArticleResultEvent.Failed(change.msg)

                is ArticlePartialStateChange.ReadArticle.Failed ->
                    ArticleEvent.ReadArticleResultEvent.Failed(change.msg)

                is ArticlePartialStateChange.DeleteArticle.Failed ->
                    ArticleEvent.DeleteArticleResultEvent.Failed(change.msg)

                is ArticlePartialStateChange.DeleteArticle.Success -> {
                    if (change.result.downloadProtectedCount == 0) return@onEach
                    ArticleEvent.DeleteArticleResultEvent.ProtectedByDownload
                }

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<ArticleIntent>.toArticlePartialStateChangeFlow(): Flow<ArticlePartialStateChange> {
        return merge(
            toSelectionRequestPartialStateChangeFlow(),
            filterIsInstance<ArticleIntent.Selection.Enter>().map {
                ArticlePartialStateChange.Selection.Enter(it.articleId)
            },
            filterIsInstance<ArticleIntent.Selection.Toggle>().map {
                ArticlePartialStateChange.Selection.Toggle(it.articleId)
            },
            filterIsInstance<ArticleIntent.Selection.Clear>().map {
                ArticlePartialStateChange.Selection.Clear
            },
            filterIsInstance<ArticleIntent.Selection.DismissConfirmation>().map {
                ArticlePartialStateChange.Selection.DismissConfirmation
            },
            filterIsInstance<ArticleIntent.Selection.DismissPlaylist>().map {
                ArticlePartialStateChange.Selection.DismissPlaylist
            },
            filterIsInstance<ArticleIntent.Init>().flatMapConcat { intent ->
                combine(
                    articleRepo.requestFilterMask(),
                    flowOf(
                        articleRepo.requestArticleList(
                            feedUrls = intent.feedUrls,
                            groupIds = intent.groupIds,
                            articleIds = intent.articleIds,
                        ).cachedIn(viewModelScope)
                    )
                ) { filterMask, articleList ->
                    ArticlePartialStateChange.Init.Success(
                        articlePagingDataFlow = articleList,
                        filterMask = filterMask,
                    )
                }.startWith(ArticlePartialStateChange.Init.Loading).catchMap {
                    ArticlePartialStateChange.Init.Failed(it.message.toString())
                }
            },
            filterIsInstance<ArticleIntent.UpdateFilter>()
                .filter { !viewState.value.selectionState.active }
                .flatMapConcat { intent ->
                    articleRepo.updateFilterMask(
                        feedUrls = intent.feedUrls,
                        groupIds = intent.groupIds,
                        articleIds = intent.articleIds,
                        filterMask = intent.filterMask,
                    ).map {
                        ArticlePartialStateChange.UpdateFilter.Success(intent.filterMask)
                    }
                },
            filterIsInstance<ArticleIntent.Refresh>().flatMapConcat { intent ->
                articleRepo.requestRealFeedUrls(
                    feedUrls = intent.feedUrls,
                    groupIds = intent.groupIds,
                    articleIds = intent.articleIds,
                ).flatMapConcat { realFeedUrls ->
                    articleRepo.refreshArticleList(realFeedUrls, full = false)
                }.map {
                    ArticlePartialStateChange.RefreshArticleList.Success
                }.startWith(ArticlePartialStateChange.RefreshArticleList.Loading).catchMap {
                    it.printStackTrace()
                    ArticlePartialStateChange.RefreshArticleList.Failed(it.message.toString())
                }
            },
            filterIsInstance<ArticleIntent.Favorite>().flatMapConcat { intent ->
                articleRepo.favoriteArticle(intent.articleId, intent.favorite).map {
                    ArticlePartialStateChange.FavoriteArticle.Success
                }.startWith(ArticlePartialStateChange.LoadingDialog.Show).catchMap {
                    ArticlePartialStateChange.FavoriteArticle.Failed(it.message.toString())
                }
            },
            filterIsInstance<ArticleIntent.Read>().flatMapConcat { intent ->
                articleRepo.readArticle(intent.articleId, intent.read).map {
                    ArticlePartialStateChange.ReadArticle.Success
                }.startWith(ArticlePartialStateChange.LoadingDialog.Show).catchMap {
                    ArticlePartialStateChange.ReadArticle.Failed(it.message.toString())
                }
            },
            filterIsInstance<ArticleIntent.Delete>().flatMapConcat { intent ->
                articleRepo.deleteArticle(intent.articleId).map {
                    ArticlePartialStateChange.DeleteArticle.Success(result = it)
                }.startWith(ArticlePartialStateChange.LoadingDialog.Show).catchMap {
                    ArticlePartialStateChange.DeleteArticle.Failed(it.message.toString())
                }
            },
            filterIsInstance<ArticleIntent.OnEditFeedDialog>().flatMapConcat { intent ->
                flowOf(ArticlePartialStateChange.OnEditFeedDialog(intent.feedUrl))
            },
        )
    }

    private fun Flow<ArticleIntent>.toSelectionRequestPartialStateChangeFlow(): Flow<ArticlePartialStateChange> {
        val selectionIntents = filterIsInstance<ArticleIntent.Selection>().filter {
            viewState.value.selectionState.let { it.active && !it.busy }
        }
        val editableIntents = selectionIntents.filter {
            viewState.value.selectionState.confirmation == null
        }
        val requests = merge(
            editableIntents.filterIsInstance<ArticleIntent.Selection.SelectAll>()
                .map { intent ->
                    articleRepo.requestSelectionIds(
                        feedUrls = intent.feedUrls,
                        groupIds = intent.groupIds,
                        articleIds = intent.articleIds,
                        filterMask = intent.filterMask,
                    ).map {
                        ArticlePartialStateChange.Selection.Selected(it)
                    }.startWith(ArticlePartialStateChange.Selection.Loading()).catchMap {
                        ArticlePartialStateChange.Selection.Failed(it.message.toString())
                    }
                },
            editableIntents.filterIsInstance<ArticleIntent.Selection.Read>()
                .filter { it.articleIds.isNotEmpty() }
                .map { articleRepo.readSelectedArticles(it.articleIds, it.read).toBatchChanges() },
            editableIntents.filterIsInstance<ArticleIntent.Selection.Favorite>()
                .filter { it.articleIds.isNotEmpty() }
                .map {
                    articleRepo.favoriteSelectedArticles(it.articleIds, it.favorite)
                        .toBatchChanges()
                },
            editableIntents.filterIsInstance<ArticleIntent.Selection.PreparePlaylist>()
                .filter { it.articleIds.isNotEmpty() }
                .map { intent ->
                    articleRepo.prepareSelectedPlaylistMedia(intent.articleIds, intent.filterMask)
                        .map { ArticlePartialStateChange.Selection.PlaylistPrepared(it) }
                        .startWith(ArticlePartialStateChange.Selection.Loading(BatchProgress(intent.articleIds.size)))
                        .catchMap { ArticlePartialStateChange.Selection.Failed(it.message.toString()) }
                },
            editableIntents.filterIsInstance<ArticleIntent.Selection.AddToPlaylist>()
                .filter { it.medias.isNotEmpty() }
                .map {
                    addToPlaylistRepo.insertSelectedPlaylistMedias(it.playlistId, it.medias)
                        .toBatchChanges()
                },
            editableIntents.filterIsInstance<ArticleIntent.Selection.Download>()
                .filter { it.articleIds.isNotEmpty() }
                .map { intent ->
                    channelFlow<ArticlePartialStateChange> {
                        val report: suspend (BatchProgress) -> Unit = {
                            send(ArticlePartialStateChange.Selection.Progress(it))
                        }
                        val plan = articleRepo.prepareSelectedDownloads(
                            intent.articleIds, intent.downloader, report,
                        ).first()
                        if (intent.articleIds.size > 100) {
                            send(ArticlePartialStateChange.Selection.Confirmation(plan))
                        } else {
                            val result = articleRepo
                                .downloadSelectedArticles(plan, intent.downloader, report).first()
                            send(ArticlePartialStateChange.Selection.Downloaded(result))
                        }
                    }.buffer(0)
                        .startWith(ArticlePartialStateChange.Selection.Loading(BatchProgress(intent.articleIds.size)))
                        .catchMap { ArticlePartialStateChange.Selection.Failed(it.message.toString()) }
                },
            selectionIntents.filterIsInstance<ArticleIntent.Selection.ConfirmDownload>()
                .filter { it.plan == viewState.value.selectionState.confirmation }
                .map { intent ->
                    channelFlow<ArticlePartialStateChange> {
                        val result =
                            articleRepo.downloadSelectedArticles(intent.plan, intent.downloader) {
                                send(ArticlePartialStateChange.Selection.Progress(it))
                            }.first()
                        send(ArticlePartialStateChange.Selection.Downloaded(result))
                    }.buffer(0).startWith(ArticlePartialStateChange.Selection.Loading())
                        .catchMap { ArticlePartialStateChange.Selection.Failed(it.message.toString()) }
                },
        )
        return filter { it is ArticleIntent.Init || it is ArticleIntent.Selection.Exit }
            .startWith(ArticleIntent.Selection.Enter())
            .flatMapLatest { intent ->
                val reset =
                    if (intent == ArticleIntent.Selection.Exit && viewState.value.selectionState.busy) {
                        ArticlePartialStateChange.Selection.Cancelled(viewState.value.selectionState.progress)
                    } else {
                        ArticlePartialStateChange.Selection.Exit
                    }
                requests.flattenFirst().startWith(reset)
            }
    }

    private fun Flow<BatchProgress>.toBatchChanges(): Flow<ArticlePartialStateChange> =
        buffer(0).map { progress ->
            if (progress.remainingCount == 0) ArticlePartialStateChange.Selection.Completed(progress)
            else ArticlePartialStateChange.Selection.Progress(progress)
        }.startWith(ArticlePartialStateChange.Selection.Loading()).catchMap {
            ArticlePartialStateChange.Selection.Failed(it.message.toString())
        }
}

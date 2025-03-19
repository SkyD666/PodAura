package com.skyd.anivu.ui.screen.article

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.skyd.anivu.base.mvi.AbstractMviViewModel
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.model.repository.article.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import javax.inject.Inject

@HiltViewModel
class ArticleViewModel @Inject constructor(
    private val articleRepo: ArticleRepository
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
                is ArticlePartialStateChange.ArticleList.Failed ->
                    ArticleEvent.InitArticleListResultEvent.Failed(change.msg)

                is ArticlePartialStateChange.RefreshArticleList.Failed ->
                    ArticleEvent.RefreshArticleListResultEvent.Failed(change.msg)

                is ArticlePartialStateChange.FavoriteArticle.Failed ->
                    ArticleEvent.FavoriteArticleResultEvent.Failed(change.msg)

                is ArticlePartialStateChange.ReadArticle.Failed ->
                    ArticleEvent.ReadArticleResultEvent.Failed(change.msg)

                is ArticlePartialStateChange.DeleteArticle.Failed ->
                    ArticleEvent.DeleteArticleResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<ArticleIntent>.toArticlePartialStateChangeFlow(): Flow<ArticlePartialStateChange> {
        return merge(
            filterIsInstance<ArticleIntent.Init>().flatMapConcat { intent ->
                flowOf(
                    articleRepo.requestArticleList(
                        feedUrls = intent.urls,
                        groupIds = intent.groupIds,
                        articleIds = intent.articleIds,
                    ).cachedIn(viewModelScope)
                ).map {
                    ArticlePartialStateChange.ArticleList.Success(articlePagingDataFlow = it)
                }.startWith(ArticlePartialStateChange.ArticleList.Loading).catchMap {
                    ArticlePartialStateChange.ArticleList.Failed(it.message.toString())
                }
            },
            filterIsInstance<ArticleIntent.UpdateSort>().flatMapConcat { intent ->
                flowOf(articleRepo.updateSort(intent.articleSort)).map {
                    ArticlePartialStateChange.UpdateSort.Success(intent.articleSort)
                }
            },
            filterIsInstance<ArticleIntent.Refresh>().flatMapConcat { intent ->
                articleRepo.refreshArticleList(intent.feedUrls, intent.groupIds).map {
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
                    ArticlePartialStateChange.DeleteArticle.Success
                }.startWith(ArticlePartialStateChange.LoadingDialog.Show).catchMap {
                    ArticlePartialStateChange.DeleteArticle.Failed(it.message.toString())
                }
            },
            filterIsInstance<ArticleIntent.FilterFavorite>().flatMapConcat { intent ->
                flowOf(articleRepo.filterFavorite(intent.favorite)).map {
                    ArticlePartialStateChange.FavoriteFilterArticle.Success(intent.favorite)
                }.startWith(ArticlePartialStateChange.LoadingDialog.Show).catchMap {
                    ArticlePartialStateChange.FavoriteFilterArticle.Failed(it.message.toString())
                }
            },
            filterIsInstance<ArticleIntent.FilterRead>().flatMapConcat { intent ->
                flowOf(articleRepo.filterRead(intent.read)).map {
                    ArticlePartialStateChange.ReadFilterArticle.Success(intent.read)
                }.startWith(ArticlePartialStateChange.LoadingDialog.Show).catchMap {
                    ArticlePartialStateChange.ReadFilterArticle.Failed(it.message.toString())
                }
            },
        )
    }
}
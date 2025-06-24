package com.skyd.podaura.ui.screen.search

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.SearchRepository
import com.skyd.podaura.model.repository.article.IArticleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take

class SearchViewModel(
    private val searchRepo: SearchRepository,
    private val articleRepo: IArticleRepository
) : AbstractMviViewModel<SearchIntent, SearchState, SearchEvent>() {

    override val viewState: StateFlow<SearchState>

    init {
        val initialVS = SearchState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<SearchIntent.ListenSearchFeed>().take(1),
            intentFlow.filterIsInstance<SearchIntent.ListenSearchArticle>().take(1),
            intentFlow.filterNot {
                it is SearchIntent.ListenSearchFeed || it is SearchIntent.ListenSearchArticle
            }
        )
            .toSearchPartialStateChangeFlow()
            .debugLog("SearchPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<SearchPartialStateChange>.sendSingleEvent(): Flow<SearchPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is SearchPartialStateChange.FavoriteArticle.Failed ->
                    SearchEvent.FavoriteArticleResultEvent.Failed(change.msg)

                is SearchPartialStateChange.ReadArticle.Failed ->
                    SearchEvent.ReadArticleResultEvent.Failed(change.msg)

                is SearchPartialStateChange.DeleteArticle.Failed ->
                    SearchEvent.DeleteArticleResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<SearchIntent>.toSearchPartialStateChangeFlow(): Flow<SearchPartialStateChange> {
        return merge(
            filterIsInstance<SearchIntent.ListenSearchFeed>().flatMapConcat {
                flowOf(searchRepo.listenSearchFeed().cachedIn(viewModelScope)).map {
                    @Suppress("UNCHECKED_CAST")
                    SearchPartialStateChange.SearchResult.Success(result = it as Flow<PagingData<Any>>)
                }.startWith(SearchPartialStateChange.SearchResult.Loading)
                    .catchMap { SearchPartialStateChange.SearchResult.Failed(it.message.toString()) }
            },
            filterIsInstance<SearchIntent.ListenSearchArticle>().flatMapConcat { intent ->
                flowOf(
                    searchRepo.listenSearchArticle(
                        intent.feedUrls, intent.groupIds, intent.articleIds,
                    ).cachedIn(viewModelScope)
                ).map {
                    @Suppress("UNCHECKED_CAST")
                    SearchPartialStateChange.SearchResult.Success(result = it as Flow<PagingData<Any>>)
                }.startWith(SearchPartialStateChange.SearchResult.Loading)
                    .catchMap { SearchPartialStateChange.SearchResult.Failed(it.message.toString()) }
            },
            filterIsInstance<SearchIntent.UpdateQuery>().flatMapConcat { intent ->
                flowOf(searchRepo.updateQuery(intent.query)).map {
                    SearchPartialStateChange.UpdateQuery.Success
                }
            },
            filterIsInstance<SearchIntent.UpdateSort>().flatMapConcat { intent ->
                flowOf(searchRepo.updateSort(intent.dateDesc)).map {
                    SearchPartialStateChange.UpdateSort.Success
                }
            },
            filterIsInstance<SearchIntent.Favorite>().flatMapConcat { intent ->
                articleRepo.favoriteArticle(intent.articleId, intent.favorite).map {
                    SearchPartialStateChange.FavoriteArticle.Success
                }.startWith(SearchPartialStateChange.LoadingDialog.Show)
                    .catchMap { SearchPartialStateChange.FavoriteArticle.Failed(it.message.toString()) }
            },
            filterIsInstance<SearchIntent.Read>().flatMapConcat { intent ->
                articleRepo.readArticle(intent.articleId, intent.read).map {
                    SearchPartialStateChange.ReadArticle.Success
                }.startWith(SearchPartialStateChange.LoadingDialog.Show)
                    .catchMap { SearchPartialStateChange.ReadArticle.Failed(it.message.toString()) }
            },
            filterIsInstance<SearchIntent.Delete>().flatMapConcat { intent ->
                articleRepo.deleteArticle(intent.articleId).map {
                    SearchPartialStateChange.DeleteArticle.Success
                }.startWith(SearchPartialStateChange.LoadingDialog.Show)
                    .catchMap { SearchPartialStateChange.DeleteArticle.Failed(it.message.toString()) }
            },
        )
    }
}
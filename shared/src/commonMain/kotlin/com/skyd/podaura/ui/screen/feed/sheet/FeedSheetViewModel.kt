package com.skyd.podaura.ui.screen.feed.sheet

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.article.IArticleRepository
import com.skyd.podaura.model.repository.feed.sheet.IFeedSheetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan

class FeedSheetViewModel(
    private val feedSheetRepo: IFeedSheetRepository,
    private val articleRepo: IArticleRepository,
) : AbstractMviViewModel<FeedSheetIntent, FeedSheetState, FeedSheetEvent>() {

    override val viewState: StateFlow<FeedSheetState>

    init {
        val initialVS = FeedSheetState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<FeedSheetIntent.Init>().distinctUntilChanged(),
            intentFlow.filterNot { it is FeedSheetIntent.Init }
        )
            .toFeedPartialStateChangeFlow()
            .debugLog("FeedSheetPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<FeedSheetPartialStateChange>.sendSingleEvent(): Flow<FeedSheetPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is FeedSheetPartialStateChange.EditFeed.Success ->
                    FeedSheetEvent.EditFeedResultEvent.Success(change.feed)

                is FeedSheetPartialStateChange.EditFeed.Failed ->
                    FeedSheetEvent.EditFeedResultEvent.Failed(change.msg)

                is FeedSheetPartialStateChange.ClearFeedArticles.Success ->
                    FeedSheetEvent.ClearFeedArticlesResultEvent.Success(change.feed)

                is FeedSheetPartialStateChange.ClearFeedArticles.Failed ->
                    FeedSheetEvent.ClearFeedArticlesResultEvent.Failed(change.msg)

                is FeedSheetPartialStateChange.RemoveFeed.Failed ->
                    FeedSheetEvent.RemoveFeedResultEvent.Failed(change.msg)

                is FeedSheetPartialStateChange.RefreshFeed.Success ->
                    FeedSheetEvent.RefreshFeedResultEvent.Success(change.feeds)

                is FeedSheetPartialStateChange.RefreshFeed.Failed ->
                    FeedSheetEvent.RefreshFeedResultEvent.Failed(change.msg)

                is FeedSheetPartialStateChange.CreateGroup.Failed ->
                    FeedSheetEvent.CreateGroupResultEvent.Failed(change.msg)

                is FeedSheetPartialStateChange.MoveFeedsToGroup.Failed ->
                    FeedSheetEvent.MoveFeedsToGroupResultEvent.Failed(change.msg)

                is FeedSheetPartialStateChange.ReadAll.Success ->
                    FeedSheetEvent.ReadAllResultEvent.Success(change.feeds)

                is FeedSheetPartialStateChange.ReadAll.Failed ->
                    FeedSheetEvent.ReadAllResultEvent.Failed(change.msg)

                is FeedSheetPartialStateChange.MuteFeed.Success ->
                    FeedSheetEvent.MuteFeedResultEvent.Success(change.mute)

                is FeedSheetPartialStateChange.MuteFeed.Failed ->
                    FeedSheetEvent.MuteFeedResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<FeedSheetIntent>.toFeedPartialStateChangeFlow(): Flow<FeedSheetPartialStateChange> {
        return merge(
            filterIsInstance<FeedSheetIntent.Init>().flatMapConcat { intent ->
                combine(
                    flowOf(feedSheetRepo.requestGroups().cachedIn(viewModelScope)),
                    feedSheetRepo.getFeed(intent.feedUrl),
                ) { groups, editFeedDialogBean ->
                    FeedSheetPartialStateChange.Init.Success(
                        editFeedDialogBean = editFeedDialogBean,
                        groups = groups,
                    )
                }.startWith(FeedSheetPartialStateChange.Init.Loading)
                    .catchMap { FeedSheetPartialStateChange.Init.Failed(it.message.orEmpty()) }
            },
            merge(
                filterIsInstance<FeedSheetIntent.EditFeedUrl>().map { intent ->
                    feedSheetRepo.editFeedUrl(oldUrl = intent.oldUrl, newUrl = intent.newUrl)
                },
                filterIsInstance<FeedSheetIntent.EditFeedGroup>().map { intent ->
                    feedSheetRepo.editFeedGroup(url = intent.url, groupId = intent.groupId)
                },
                filterIsInstance<FeedSheetIntent.EditFeedCustomDescription>().map { intent ->
                    feedSheetRepo.editFeedCustomDescription(
                        url = intent.url, customDescription = intent.customDescription,
                    )
                },
                filterIsInstance<FeedSheetIntent.EditFeedCustomIcon>().map { intent ->
                    feedSheetRepo.editFeedCustomIcon(
                        url = intent.url,
                        customIcon = intent.customIcon
                    )
                },
                filterIsInstance<FeedSheetIntent.EditFeedSortXmlArticlesOnUpdate>().map { intent ->
                    feedSheetRepo.editFeedSortXmlArticlesOnUpdate(
                        url = intent.url,
                        sort = intent.sort
                    )
                },
                filterIsInstance<FeedSheetIntent.EditFeedNickname>().map { intent ->
                    feedSheetRepo.editFeedNickname(url = intent.url, nickname = intent.nickname)
                },
            ).flatMapConcat { flow ->
                flow.map { FeedSheetPartialStateChange.EditFeed.Success(it) }
                    .startWith(FeedSheetPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedSheetPartialStateChange.EditFeed.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedSheetIntent.ClearFeedArticles>().flatMapConcat { intent ->
                feedSheetRepo.clearFeedArticles(intent.url).flatMapConcat {
                    feedSheetRepo.getFeedViewsByUrls(listOf(intent.url))
                }.map {
                    FeedSheetPartialStateChange.ClearFeedArticles.Success(it.first())
                }.startWith(FeedSheetPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedSheetPartialStateChange.ClearFeedArticles.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedSheetIntent.RemoveFeed>().flatMapConcat { intent ->
                feedSheetRepo.removeFeed(intent.url).map {
                    if (it > 0) FeedSheetPartialStateChange.RemoveFeed.Success
                    else FeedSheetPartialStateChange.RemoveFeed.Failed("Remove failed!")
                }.startWith(FeedSheetPartialStateChange.LoadingDialog.Show)
            },
            filterIsInstance<FeedSheetIntent.ReadAllInFeed>().flatMapConcat { intent ->
                feedSheetRepo.readAllInFeed(intent.feedUrl).flatMapConcat {
                    feedSheetRepo.getFeedViewsByUrls(listOf(intent.feedUrl))
                }.map { FeedSheetPartialStateChange.ReadAll.Success(it) }
                    .startWith(FeedSheetPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedSheetPartialStateChange.ReadAll.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedSheetIntent.RefreshFeed>().flatMapConcat { intent ->
                val urls = listOf(intent.url)
                articleRepo.refreshArticleList(feedUrls = urls, full = intent.full).flatMapConcat {
                    feedSheetRepo.getFeedViewsByUrls(urls)
                }.map { FeedSheetPartialStateChange.RefreshFeed.Success(it) }
                    .startWith(FeedSheetPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedSheetPartialStateChange.RefreshFeed.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedSheetIntent.CreateGroup>().flatMapConcat { intent ->
                feedSheetRepo.createGroup(intent.group).map {
                    FeedSheetPartialStateChange.CreateGroup.Success
                }.startWith(FeedSheetPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedSheetPartialStateChange.CreateGroup.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedSheetIntent.MuteFeed>().flatMapConcat { intent ->
                feedSheetRepo.muteFeed(intent.feedUrl, intent.mute).map {
                    FeedSheetPartialStateChange.MuteFeed.Success(intent.mute)
                }.startWith(FeedSheetPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedSheetPartialStateChange.MuteFeed.Failed(it.message.toString()) }
            },
        )
    }
}
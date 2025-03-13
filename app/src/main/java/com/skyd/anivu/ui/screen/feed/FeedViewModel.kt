package com.skyd.anivu.ui.screen.feed

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.skyd.anivu.base.mvi.AbstractMviViewModel
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.model.repository.ArticleRepository
import com.skyd.anivu.model.repository.feed.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepo: FeedRepository,
    private val articleRepo: ArticleRepository,
) : AbstractMviViewModel<FeedIntent, FeedState, FeedEvent>() {

    override val viewState: StateFlow<FeedState>

    init {
        val initialVS = FeedState.initial()

        viewState = merge(
            intentFlow.filterIsInstance<FeedIntent.Init>().take(1),
            intentFlow.filterNot { it is FeedIntent.Init }
        )
            .toFeedPartialStateChangeFlow()
            .debugLog("FeedPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<FeedPartialStateChange>.sendSingleEvent(): Flow<FeedPartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is FeedPartialStateChange.AddFeed.Success ->
                    FeedEvent.AddFeedResultEvent.Success(change.feed)

                is FeedPartialStateChange.AddFeed.Failed ->
                    FeedEvent.AddFeedResultEvent.Failed(change.msg)

                is FeedPartialStateChange.EditFeed.Success ->
                    FeedEvent.EditFeedResultEvent.Success(change.feed)

                is FeedPartialStateChange.EditFeed.Failed ->
                    FeedEvent.EditFeedResultEvent.Failed(change.msg)

                is FeedPartialStateChange.ClearFeedArticles.Success ->
                    FeedEvent.ClearFeedArticlesResultEvent.Success(change.feed)

                is FeedPartialStateChange.ClearFeedArticles.Failed ->
                    FeedEvent.ClearFeedArticlesResultEvent.Failed(change.msg)

                is FeedPartialStateChange.RemoveFeed.Failed ->
                    FeedEvent.RemoveFeedResultEvent.Failed(change.msg)

                is FeedPartialStateChange.RefreshFeed.Success ->
                    FeedEvent.RefreshFeedResultEvent.Success(change.feeds)

                is FeedPartialStateChange.RefreshFeed.Failed ->
                    FeedEvent.RefreshFeedResultEvent.Failed(change.msg)

                is FeedPartialStateChange.FeedList.Failed ->
                    FeedEvent.InitFeetListResultEvent.Failed(change.msg)

                is FeedPartialStateChange.CreateGroup.Failed ->
                    FeedEvent.CreateGroupResultEvent.Failed(change.msg)

                is FeedPartialStateChange.ClearGroupArticles.Failed ->
                    FeedEvent.ClearGroupArticlesResultEvent.Failed(change.msg)

                is FeedPartialStateChange.DeleteGroup.Failed ->
                    FeedEvent.DeleteGroupResultEvent.Failed(change.msg)

                is FeedPartialStateChange.MoveFeedsToGroup.Failed ->
                    FeedEvent.MoveFeedsToGroupResultEvent.Failed(change.msg)

                is FeedPartialStateChange.EditGroup.Success ->
                    FeedEvent.EditGroupResultEvent.Success(change.group)

                is FeedPartialStateChange.EditGroup.Failed ->
                    FeedEvent.EditGroupResultEvent.Failed(change.msg)

                is FeedPartialStateChange.ReadAll.Success ->
                    FeedEvent.ReadAllResultEvent.Success(change.feeds)

                is FeedPartialStateChange.ReadAll.Failed ->
                    FeedEvent.ReadAllResultEvent.Failed(change.msg)

                is FeedPartialStateChange.MuteFeed.Success ->
                    FeedEvent.MuteFeedResultEvent.Success(change.mute)

                is FeedPartialStateChange.MuteFeed.Failed ->
                    FeedEvent.MuteFeedResultEvent.Failed(change.msg)

                is FeedPartialStateChange.MuteFeedsInGroup.Failed ->
                    FeedEvent.MuteFeedsInGroupResultEvent.Failed(change.msg)

                is FeedPartialStateChange.CollapseAllGroup.Failed ->
                    FeedEvent.CollapseAllGroupResultEvent.Failed(change.msg)

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<FeedIntent>.toFeedPartialStateChangeFlow(): Flow<FeedPartialStateChange> {
        return merge(
            filterIsInstance<FeedIntent.Init>().flatMapConcat {
                combine(
                    feedRepo.allGroupCollapsed(),
                    flowOf(feedRepo.requestGroups().cachedIn(viewModelScope)),
                    flowOf(feedRepo.requestGroupAnyPaging().cachedIn(viewModelScope)),
                ) { allGroupCollapsed, groups, list ->
                    FeedPartialStateChange.FeedList.Success(
                        allGroupCollapsed = allGroupCollapsed,
                        groups = groups,
                        dataPagingDataFlow = list,
                    )
                }.startWith(FeedPartialStateChange.FeedList.Loading)
            },
            filterIsInstance<FeedIntent.AddFeed>().flatMapConcat { intent ->
                feedRepo.setFeed(
                    url = intent.url,
                    nickname = intent.nickname,
                    groupId = intent.group.groupId,
                ).map {
                    FeedPartialStateChange.AddFeed.Success(it)
                }.startWith(FeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedPartialStateChange.AddFeed.Failed(it.message.toString()) }
            },
            merge(
                filterIsInstance<FeedIntent.EditFeedUrl>().map { intent ->
                    feedRepo.editFeedUrl(oldUrl = intent.oldUrl, newUrl = intent.newUrl)
                },
                filterIsInstance<FeedIntent.EditFeedGroup>().map { intent ->
                    feedRepo.editFeedGroup(url = intent.url, groupId = intent.groupId)
                },
                filterIsInstance<FeedIntent.EditFeedCustomDescription>().map { intent ->
                    feedRepo.editFeedCustomDescription(
                        url = intent.url, customDescription = intent.customDescription,
                    )
                },
                filterIsInstance<FeedIntent.EditFeedCustomIcon>().map { intent ->
                    feedRepo.editFeedCustomIcon(url = intent.url, customIcon = intent.customIcon)
                },
                filterIsInstance<FeedIntent.EditFeedSortXmlArticlesOnUpdate>().map { intent ->
                    feedRepo.editFeedSortXmlArticlesOnUpdate(url = intent.url, sort = intent.sort)
                },
                filterIsInstance<FeedIntent.EditFeedNickname>().map { intent ->
                    feedRepo.editFeedNickname(url = intent.url, nickname = intent.nickname)
                },
            ).flatMapConcat { flow ->
                flow.map { FeedPartialStateChange.EditFeed.Success(it) }
                    .startWith(FeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedPartialStateChange.EditFeed.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedIntent.ClearFeedArticles>().flatMapConcat { intent ->
                feedRepo.clearFeedArticles(intent.url).flatMapConcat {
                    feedRepo.getFeedViewsByUrls(listOf(intent.url))
                }.map {
                    FeedPartialStateChange.ClearFeedArticles.Success(it.first())
                }.startWith(FeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedPartialStateChange.ClearFeedArticles.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedIntent.RemoveFeed>().flatMapConcat { intent ->
                feedRepo.removeFeed(intent.url).map {
                    if (it > 0) FeedPartialStateChange.RemoveFeed.Success
                    else FeedPartialStateChange.RemoveFeed.Failed("Remove failed!")
                }.startWith(FeedPartialStateChange.LoadingDialog.Show)
            },
            merge(
                filterIsInstance<FeedIntent.ReadAllInGroup>().map { intent ->
                    feedRepo.readAllInGroup(intent.groupId).flatMapConcat {
                        feedRepo.getFeedViewsByGroupId(intent.groupId)
                    }
                },
                filterIsInstance<FeedIntent.ReadAllInFeed>().map { intent ->
                    feedRepo.readAllInFeed(intent.feedUrl).flatMapConcat {
                        feedRepo.getFeedViewsByUrls(listOf(intent.feedUrl))
                    }
                },
            ).flatMapConcat { flow ->
                flow.map { FeedPartialStateChange.ReadAll.Success(it) }
                    .startWith(FeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedPartialStateChange.ReadAll.Failed(it.message.toString()) }
            },
            merge(
                filterIsInstance<FeedIntent.RefreshFeed>().map { intent ->
                    val urls = listOf(intent.url)
                    articleRepo.refreshArticleList(urls).flatMapConcat {
                        feedRepo.getFeedViewsByUrls(urls)
                    }
                },
                filterIsInstance<FeedIntent.RefreshGroupFeed>().map { intent ->
                    articleRepo.refreshGroupArticles(intent.groupId).flatMapConcat {
                        feedRepo.getFeedViewsByGroupId(intent.groupId)
                    }
                },
            ).flatMapConcat { flow ->
                flow.map { FeedPartialStateChange.RefreshFeed.Success(it) }
                    .startWith(FeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedPartialStateChange.RefreshFeed.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedIntent.CreateGroup>().flatMapConcat { intent ->
                feedRepo.createGroup(intent.group).map {
                    FeedPartialStateChange.CreateGroup.Success
                }.startWith(FeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedPartialStateChange.CreateGroup.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedIntent.ChangeGroupExpanded>().flatMapConcat { intent ->
                feedRepo.changeGroupExpanded(intent.group.groupId, intent.expanded).map {
                    FeedPartialStateChange.GroupExpandedChanged.Success
                }.catchMap<FeedPartialStateChange.GroupExpandedChanged> {
                    FeedPartialStateChange.GroupExpandedChanged.Failed(it.message.toString())
                }
            },
            filterIsInstance<FeedIntent.ClearGroupArticles>().flatMapConcat { intent ->
                feedRepo.clearGroupArticles(intent.groupId).map {
                    FeedPartialStateChange.ClearGroupArticles.Success
                }.startWith(FeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedPartialStateChange.ClearGroupArticles.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedIntent.DeleteGroup>().flatMapConcat { intent ->
                feedRepo.deleteGroup(intent.groupId).map {
                    FeedPartialStateChange.DeleteGroup.Success
                }.startWith(FeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedPartialStateChange.DeleteGroup.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedIntent.RenameGroup>().flatMapConcat { intent ->
                feedRepo.renameGroup(intent.groupId, intent.name).map {
                    FeedPartialStateChange.EditGroup.Success(it)
                }.startWith(FeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedPartialStateChange.EditGroup.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedIntent.MoveFeedsToGroup>().flatMapConcat { intent ->
                feedRepo.moveGroupFeedsTo(intent.fromGroupId, intent.toGroupId).map {
                    FeedPartialStateChange.MoveFeedsToGroup.Success
                }.startWith(FeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedPartialStateChange.MoveFeedsToGroup.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedIntent.MuteFeed>().flatMapConcat { intent ->
                feedRepo.muteFeed(intent.feedUrl, intent.mute).map {
                    FeedPartialStateChange.MuteFeed.Success(intent.mute)
                }.startWith(FeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedPartialStateChange.MuteFeed.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedIntent.MuteFeedsInGroup>().flatMapConcat { intent ->
                feedRepo.muteFeedsInGroup(intent.groupId, intent.mute).map {
                    FeedPartialStateChange.MuteFeedsInGroup.Success
                }.startWith(FeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedPartialStateChange.MuteFeedsInGroup.Failed(it.message.toString()) }
            },
            filterIsInstance<FeedIntent.CollapseAllGroup>().flatMapConcat { intent ->
                feedRepo.collapseAllGroup(intent.collapse).map {
                    FeedPartialStateChange.CollapseAllGroup.Success
                }.startWith(FeedPartialStateChange.LoadingDialog.Show)
                    .catchMap { FeedPartialStateChange.CollapseAllGroup.Failed(it.message.toString()) }
            },
        )
    }
}
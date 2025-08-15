package com.skyd.podaura.di

import com.skyd.podaura.ui.activity.player.PlayerViewModel
import com.skyd.podaura.ui.screen.article.ArticleViewModel
import com.skyd.podaura.ui.screen.download.DownloadViewModel
import com.skyd.podaura.ui.screen.feed.FeedViewModel
import com.skyd.podaura.ui.screen.feed.reorder.group.ReorderGroupViewModel
import com.skyd.podaura.ui.screen.history.HistoryViewModel
import com.skyd.podaura.ui.screen.history.search.HistorySearchViewModel
import com.skyd.podaura.ui.screen.media.MediaViewModel
import com.skyd.podaura.ui.screen.media.list.MediaListViewModel
import com.skyd.podaura.ui.screen.media.search.MediaSearchViewModel
import com.skyd.podaura.ui.screen.playlist.PlaylistViewModel
import com.skyd.podaura.ui.screen.playlist.addto.AddToPlaylistViewModel
import com.skyd.podaura.ui.screen.playlist.medialist.PlaylistMediaListViewModel
import com.skyd.podaura.ui.screen.playlist.medialist.list.ListViewModel
import com.skyd.podaura.ui.screen.read.ReadViewModel
import com.skyd.podaura.ui.screen.search.SearchViewModel
import com.skyd.podaura.ui.screen.settings.data.DataViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { PlayerViewModel(get(), get()) }
    viewModel { ArticleViewModel(get()) }
    viewModel { DownloadViewModel(get()) }
    viewModel { FeedViewModel(get(), get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { HistorySearchViewModel(get()) }
    viewModel { MediaViewModel(get()) }
    viewModel { MediaListViewModel(get()) }
    viewModel { MediaSearchViewModel(get()) }
    viewModel { PlaylistViewModel(get()) }
    viewModel { AddToPlaylistViewModel(get(), get()) }
    viewModel { PlaylistMediaListViewModel(get(), get(), get()) }
    viewModel { ListViewModel() }
    viewModel { ReadViewModel(get(), get()) }
    viewModel { SearchViewModel(get(), get()) }
    viewModel { DataViewModel(get()) }
}
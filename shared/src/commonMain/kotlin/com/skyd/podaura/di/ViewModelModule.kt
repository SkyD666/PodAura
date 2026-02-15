package com.skyd.podaura.di

import com.skyd.podaura.ui.screen.about.update.UpdateViewModel
import com.skyd.podaura.ui.screen.article.ArticleViewModel
import com.skyd.podaura.ui.screen.calendar.daylist.DayListViewModel
import com.skyd.podaura.ui.screen.download.DownloadViewModel
import com.skyd.podaura.ui.screen.feed.FeedViewModel
import com.skyd.podaura.ui.screen.feed.autodl.AutoDownloadRuleViewModel
import com.skyd.podaura.ui.screen.feed.mute.MuteFeedViewModel
import com.skyd.podaura.ui.screen.feed.reorder.feed.ReorderFeedViewModel
import com.skyd.podaura.ui.screen.feed.reorder.group.ReorderGroupViewModel
import com.skyd.podaura.ui.screen.feed.requestheaders.RequestHeadersViewModel
import com.skyd.podaura.ui.screen.filepicker.FilePickerViewModel
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
import com.skyd.podaura.ui.screen.settings.data.importexport.ImportExportViewModel
import com.skyd.podaura.ui.screen.settings.data.importexport.importopml.ImportOpmlViewModel
import com.skyd.podaura.ui.screen.settings.rssconfig.updatenotification.UpdateNotificationViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { UpdateViewModel(get()) }
    viewModel { AutoDownloadRuleViewModel(get()) }
    viewModel { MuteFeedViewModel(get()) }
    viewModel { RequestHeadersViewModel(get()) }
    viewModel { ImportExportViewModel(get(), get()) }
    viewModel { ImportOpmlViewModel(get()) }
    viewModel { UpdateNotificationViewModel(get()) }
    viewModel { FilePickerViewModel(get()) }
    viewModel { ReorderGroupViewModel(get()) }
    viewModel { ReorderFeedViewModel(get()) }
    viewModel { ArticleViewModel(get()) }
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
    viewModel { DownloadViewModel(get()) }
    viewModel { DayListViewModel(get()) }
}
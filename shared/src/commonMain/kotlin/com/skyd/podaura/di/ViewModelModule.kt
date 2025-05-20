package com.skyd.podaura.di

import com.skyd.podaura.ui.screen.about.update.UpdateViewModel
import com.skyd.podaura.ui.screen.feed.autodl.AutoDownloadRuleViewModel
import com.skyd.podaura.ui.screen.feed.mute.MuteFeedViewModel
import com.skyd.podaura.ui.screen.feed.requestheaders.RequestHeadersViewModel
import com.skyd.podaura.ui.screen.filepicker.FilePickerViewModel
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
}
package com.skyd.podaura.di

import com.skyd.podaura.ui.activity.player.PlayerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { PlayerViewModel(get(), get()) }
}
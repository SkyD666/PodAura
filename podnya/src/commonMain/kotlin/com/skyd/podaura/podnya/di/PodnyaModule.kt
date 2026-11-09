package com.skyd.podaura.podnya.di

import com.skyd.podaura.podnya.capabilities.articles.di.articleSearchModule
import com.skyd.podaura.podnya.capabilities.feeds.model.repository.LocalFeedSearchRepository
import com.skyd.podaura.podnya.model.repository.AssistantConversationRepository
import com.skyd.podaura.podnya.settings.AssistantSettingsRepository
import com.skyd.podaura.podnya.settings.AssistantSettingsViewModel
import com.skyd.podaura.podnya.ui.screen.chat.PodnyaViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.onClose

val podnyaModule = module {
    includes(assistantPlatformModule, articleSearchModule)
    single { LocalFeedSearchRepository(get()) }
    single { AssistantSettingsRepository(get(), get()) }
    single { AssistantConversationRepository(get()) }.onClose { it?.close() }
    viewModelOf(::PodnyaViewModel)
    viewModelOf(::AssistantSettingsViewModel)
}

expect val assistantPlatformModule: Module

package com.skyd.podaura.nano.capabilities.feeds

import com.skyd.podaura.nano.capabilities.feeds.model.FeedHistoryCodec
import com.skyd.podaura.nano.capabilities.feeds.model.repository.LocalFeedSearchRepository
import com.skyd.podaura.nano.capabilities.feeds.ui.FeedSearchUi
import com.skyd.podaura.nano.core.AssistantHistoryCodec
import com.skyd.podaura.nano.ui.AssistantCapabilityUi
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val feedSearchModule = module {
    includes(feedSearchRuntimeModule)
    val key = named(FeedHistoryCodec.capabilityId)
    single<AssistantHistoryCodec>(key) { FeedHistoryCodec }
    single<AssistantCapabilityUi>(key) { FeedSearchUi }
    single { LocalFeedSearchRepository(get()) }
}

internal expect val feedSearchRuntimeModule: Module

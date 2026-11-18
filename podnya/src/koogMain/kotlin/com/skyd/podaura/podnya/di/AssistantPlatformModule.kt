package com.skyd.podaura.podnya.di

import com.skyd.podaura.podnya.capabilities.articles.ArticleSearchCapability
import com.skyd.podaura.podnya.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.podnya.capabilities.feeds.FeedSearchCapability
import com.skyd.podaura.podnya.capabilities.feeds.model.repository.LocalFeedSearchRepository
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.runtime.KoogAssistantRuntime
import com.skyd.podaura.podnya.runtime.initializePodnyaLogging
import org.koin.dsl.module

actual val assistantPlatformModule = module {
    initializePodnyaLogging()
    single<AssistantRuntime> {
        val articles = get<LocalArticleSearchRepository>()
        val feeds = get<LocalFeedSearchRepository>()
        KoogAssistantRuntime(capabilities = { request ->
            listOfNotNull(
                ArticleSearchCapability.create(articles, request),
                FeedSearchCapability.create(feeds, request)
            )
        })
    }
}

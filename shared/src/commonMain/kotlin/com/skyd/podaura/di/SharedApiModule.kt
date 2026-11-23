package com.skyd.podaura.di

import com.skyd.podaura.api.model.repository.IArticleRepository
import com.skyd.podaura.api.model.repository.IAssistantDownloadRepository
import com.skyd.podaura.api.model.repository.IFeedRepository
import com.skyd.podaura.model.repository.article.ArticleQueryRepository
import com.skyd.podaura.model.repository.download.AssistantDownloadRepository
import com.skyd.podaura.model.repository.feed.FeedQueryRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose

private val assistantDirectLink = named("assistantDirectLink")

/** Public host capabilities used by independently built features. */
val feedQueryModule = module {
    single<IFeedRepository> { FeedQueryRepository(get()) }
}

val articleQueryModule = module {
    single<IArticleRepository> { ArticleQueryRepository(get(), get()) }
}

val assistantDownloadHostModule = module {
    includes(feedQueryModule)
    single(assistantDirectLink) {
        HttpClient {
            followRedirects = false
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 10_000
            }
        }
    }.onClose { it?.close() }
    single<IAssistantDownloadRepository> {
        AssistantDownloadRepository(
            articleDao = get(),
            downloader = get(),
            directLinkClient = get(assistantDirectLink),
        )
    }
}

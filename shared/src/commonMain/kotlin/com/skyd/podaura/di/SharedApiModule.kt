package com.skyd.podaura.di

import com.skyd.podaura.api.model.repository.IArticleRepository
import com.skyd.podaura.api.model.repository.IFeedRepository
import com.skyd.podaura.model.repository.article.ArticleQueryRepository
import com.skyd.podaura.model.repository.feed.FeedQueryRepository
import org.koin.dsl.module

/** Public host capabilities used by independently built features. */
val sharedApiModule = module {
    single<IFeedRepository> { FeedQueryRepository(get()) }
    single<IArticleRepository> { ArticleQueryRepository(get(), get()) }
}

package com.skyd.podaura.nano.capabilities.articles

import com.skyd.podaura.nano.capabilities.articles.model.ArticleHistoryCodec
import com.skyd.podaura.nano.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.nano.capabilities.articles.ui.ArticleSearchUi
import com.skyd.podaura.nano.core.AssistantHistoryCodec
import com.skyd.podaura.nano.ui.AssistantCapabilityUi
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val articleSearchModule = module {
    includes(articleSearchRuntimeModule)
    val key = named(ArticleHistoryCodec.capabilityId)
    single<AssistantHistoryCodec>(key) { ArticleHistoryCodec }
    single<AssistantCapabilityUi>(key) { ArticleSearchUi }
    single { LocalArticleSearchRepository(get()) }
}

internal expect val articleSearchRuntimeModule: Module

package com.skyd.podaura.podnya.di

import com.skyd.podaura.podnya.capabilities.articles.ui.ArticleSearchUi
import com.skyd.podaura.podnya.capabilities.feeds.ui.FeedSearchUi
import com.skyd.podaura.podnya.ui.AssistantCapabilityUi

/** Register capability UIs here; the conversation screen has no domain-specific branches. */
internal val podnyaCapabilityUis: List<AssistantCapabilityUi> = listOf(
    ArticleSearchUi, FeedSearchUi
)

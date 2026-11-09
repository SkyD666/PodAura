package com.skyd.podaura.podnya.capabilities.articles.model.bean

import com.skyd.podaura.api.model.bean.ArticleSearchScope
import com.skyd.podaura.podnya.core.AssistantContext

/** Article execution context; tools and data sources receive only its search scope. */
data class ArticleSearchContext(
    val scope: ArticleSearchScope = ArticleSearchScope.All,
) : AssistantContext {
    override fun snapshot(): ArticleSearchContext = copy(
        scope = when (val current = scope) {
            ArticleSearchScope.All -> current
            is ArticleSearchScope.Restricted -> current.copy(
                feedUrls = current.feedUrls.toSet(), articleIds = current.articleIds.toSet(),
            )
        }
    )
}

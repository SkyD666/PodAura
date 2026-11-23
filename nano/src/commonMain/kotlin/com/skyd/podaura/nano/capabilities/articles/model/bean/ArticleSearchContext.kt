package com.skyd.podaura.nano.capabilities.articles.model.bean

import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.nano.capabilities.ArticleTargetContext

/** Article execution context; tools and data sources receive only its search scope. */
data class ArticleSearchContext(
    override val scope: ArticleSearchScope = ArticleSearchScope.All,
) : ArticleTargetContext {
    override val retainedBytes: Int
        get() = when (val current = scope) {
            ArticleSearchScope.All -> 0
            is ArticleSearchScope.Restricted -> (current.feedUrls + current.articleIds).sumOf { it.encodeToByteArray().size + 32 }
        }

    override fun snapshot(): ArticleSearchContext = copy(
        scope = when (val current = scope) {
            ArticleSearchScope.All -> current
            is ArticleSearchScope.Restricted -> current.copy(
                feedUrls = current.feedUrls.toSet(), articleIds = current.articleIds.toSet(),
            )
        }
    )
}

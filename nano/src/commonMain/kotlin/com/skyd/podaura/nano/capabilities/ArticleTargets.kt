package com.skyd.podaura.nano.capabilities

import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.nano.core.AssistantContext

interface ArticleTargetContext : AssistantContext {
    val scope: ArticleSearchScope
}

/** Local IDs in recommendation order and live evidence from this turn's tool calls. */
internal interface ArticleTargetSource {
    val recentArticleIds: List<String>
    val rememberedArticles: Map<String, String>
    val focusedArticleId: String?
    fun currentArticles(): Map<String, String>
}

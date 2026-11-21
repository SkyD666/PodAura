package com.skyd.podaura.podnya.capabilities.articles

import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleField
import kotlinx.serialization.Serializable

@Serializable
internal enum class LookupField(val field: ArticleField? = null) {
    KEEP, TITLE(ArticleField.TITLE), SOURCE(ArticleField.SOURCE), DATE(ArticleField.DATE), UNSUPPORTED,
}

@Serializable
internal data class ArticleFieldRequest(
    val articleId: String? = null,
    val articleField: LookupField? = null,
)

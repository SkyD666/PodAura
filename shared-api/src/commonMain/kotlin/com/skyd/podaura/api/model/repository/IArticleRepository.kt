package com.skyd.podaura.api.model.repository

import com.skyd.podaura.api.model.bean.article.ArticleQuery
import com.skyd.podaura.api.model.bean.article.ArticleRecord
import com.skyd.podaura.api.model.bean.article.ArticleSearchScope

/** Read-only article storage supplied by the host app. Returns raw fields so each caller can prepare the text it needs. */
interface IArticleRepository {
    /**
     * Apply the scope, keyword match and hard filters to current local rows without modifying them.
     * Return unique IDs ordered by date descending (null dates last), then ID ascending, with at most
     * [ArticleQuery.limit] rows.
     * An empty keyword list must not perform text matching. A keyword query with no enabled search
     * fields throws [ArticleSearchFieldsDisabledException].
     */
    suspend fun query(scope: ArticleSearchScope, query: ArticleQuery): List<ArticleRecord>
}

/** A keyword search needs at least one enabled text field. */
class ArticleSearchFieldsDisabledException : Exception("No article search fields are enabled")

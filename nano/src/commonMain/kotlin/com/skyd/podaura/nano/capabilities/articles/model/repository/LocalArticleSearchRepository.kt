package com.skyd.podaura.nano.capabilities.articles.model.repository

import com.fleeksoft.ksoup.Ksoup
import com.skyd.podaura.api.model.bean.article.ArticleQuery
import com.skyd.podaura.api.model.bean.article.ArticleRecord
import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.api.model.repository.ArticleSearchFieldsDisabledException
import com.skyd.podaura.api.model.repository.IArticleRepository
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchError
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchLimits
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchPage
import com.skyd.podaura.nano.capabilities.articles.model.bean.AssistantArticle
import com.skyd.podaura.nano.capabilities.articles.model.bean.normalizeArticleTitle
import com.skyd.podaura.nano.core.AssistantException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class LocalArticleSearchRepository(
    private val repository: IArticleRepository,
) {
    suspend fun exists(articleId: String): Boolean = readLocally {
        repository.query(
            scope = ArticleSearchScope.All,
            query = ArticleQuery(withinArticleIds = listOf(articleId), limit = 1)
        ).any { it.articleId == articleId }
    }

    suspend fun record(scope: ArticleSearchScope, articleId: String): ArticleRecord? = readLocally {
        repository.query(
            scope = scope,
            query = ArticleQuery(withinArticleIds = listOf(articleId), limit = 1),
        ).singleOrNull { it.articleId == articleId }
    }

    suspend fun search(scope: ArticleSearchScope, query: ArticleQuery): ArticleSearchPage =
        readLocally {
            validateQuery(query)
            // Fetch one extra row here; the public repository limit is an exact upper bound.
            val rows = repository.query(scope, query.copy(limit = query.limit + 1))
            ArticleSearchPage(
                items = rows.take(query.limit).map { it.toAssistantArticle(query.keywords) },
                hasMore = rows.size > query.limit,
            )
        }

    /** Recheck IDs, scope and filters against current local data without marking articles as read. */
    suspend fun resolve(
        scope: ArticleSearchScope,
        articleIds: List<String>,
        query: ArticleQuery,
    ): List<AssistantArticle> = readLocally {
        validateQuery(query)
        require(articleIds.size <= ArticleSearchLimits.CANDIDATES)
        val withinArticleIds = query.withinArticleIds
        val permitted =
            articleIds.distinct().filter { withinArticleIds == null || it in withinArticleIds }
        if (permitted.isEmpty()) return@readLocally emptyList()
        // Do not rerun a text ranking here. Recheck the original scope and hard filters against
        // current rows; changes to title/body after a search must not erase legitimate provenance.
        permitted.chunked(ArticleSearchLimits.SEARCH_RESULTS).flatMap { ids ->
            currentCoroutineContext().ensureActive()
            repository.query(
                scope, query.copy(
                    keywords = emptyList(), withinArticleIds = ids,
                    limit = ArticleSearchLimits.SEARCH_RESULTS
                )
            )
                .filter { it.articleId in ids }
                .map { it.toAssistantArticle(query.keywords) }
        }
    }

    private suspend fun ArticleRecord.toAssistantArticle(keywords: List<String>): AssistantArticle {
        currentCoroutineContext().ensureActive()
        val body = content?.takeIf(String::isNotBlank) ?: description.orEmpty()
        // Bound HTML parsing independently from the much smaller model-visible snippet.
        val plain = body.take(64 * 1024).readable().ifBlank {
            description.orEmpty().take(64 * 1024).readable()
        }
        val match =
            keywords.map { plain.indexOf(it, ignoreCase = true) }.filter { it >= 0 }.minOrNull()
        val start = (match?.minus(80) ?: 0).coerceAtLeast(0)
        currentCoroutineContext().ensureActive()
        return AssistantArticle(
            articleId = articleId,
            title = normalizeArticleTitle(title).take(200),
            sourceName = (sourceNickname ?: sourceTitle).orEmpty().take(4096).readable().take(100),
            dateMillis = dateMillis,
            snippet = plain.drop(start).take(ArticleSearchLimits.SNIPPET_CHARS),
        )
    }

    private fun validateQuery(query: ArticleQuery) {
        require(query.limit in 1..ArticleSearchLimits.SEARCH_RESULTS)
        require(query.keywords.size <= 6 && query.keywords.all { it.isNotBlank() && it.length <= 80 })
        val withinArticleIds = query.withinArticleIds
        require(withinArticleIds == null || withinArticleIds.size <= ArticleSearchLimits.CANDIDATES)
        require(query.excludeArticleIds.size <= ArticleSearchLimits.CANDIDATES)
        val from = query.publishedFromMillis
        val before = query.publishedBeforeMillis
        require(from == null || before == null || from < before)
    }

    private fun String.readable(): String = Ksoup.parse(html = this).text()

    private suspend fun <T> readLocally(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: ArticleSearchFieldsDisabledException) {
            throw AssistantException(ArticleSearchError.SEARCH_FIELDS_DISABLED)
        } catch (expected: AssistantException) {
            throw expected
        } catch (_: Exception) {
            throw AssistantException(ArticleSearchError.LOCAL_SEARCH)
        }
    }
}

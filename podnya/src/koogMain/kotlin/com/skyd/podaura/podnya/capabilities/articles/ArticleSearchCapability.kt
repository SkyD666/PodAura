package com.skyd.podaura.podnya.capabilities.articles

import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.structure.json.JsonStructure
import com.skyd.podaura.api.model.bean.ArticleSearchScope
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleRecommendation
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchContent
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchContext
import com.skyd.podaura.podnya.capabilities.articles.model.bean.AssistantAnswer
import com.skyd.podaura.podnya.capabilities.articles.model.repository.ArticleSearchResultValidator
import com.skyd.podaura.podnya.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantContext
import com.skyd.podaura.podnya.core.AssistantHistorySnapshot
import com.skyd.podaura.podnya.core.AssistantHistoryTurn
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.runtime.CapabilityResult
import com.skyd.podaura.podnya.runtime.KoogCapability
import com.skyd.podaura.podnya.runtime.podnyaJson
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

internal class ArticleSearchCapability private constructor(
    private val source: LocalArticleSearchRepository,
    private val tool: SearchArticlesTool,
) : KoogCapability {
    private val logger = KotlinLogging.logger("com.skyd.podaura.podnya.ArticleSearchCapability")
    override val id = ArticleSearchContent.CAPABILITY_ID
    override val tools = listOf(tool)
    override val failure get() = tool.localFailure
    override val selectionSchema = JsonStructure.create<ArticleSelection>().schema.schema
    override val instructions =
        """Use search_articles to find or recheck local articles and podcast episodes.
For podcasts about a topic, search episode content using topic keywords.
Translate requested dates using today's date and time zone. Article dateMillis is Unix milliseconds (UTC).
Preserve the first query's time/read/favorite/within/exclude conditions when trying synonyms.
Use provided knownIds for history references; search within those IDs again before recommending them.
Snippets are partial. Do not claim complete reading or external fact-checking. The app enforces scope."""
    override val selectionInstructions =
        """Select and rank local articles already retrieved for this turn.
Match the requested scope: relevant discussions or song segments qualify; require a dedicated topic only if asked.
For "contains X" or "mentions X", an explicit subject mention in either the title or snippet is enough.
Return podcast episodes as articles. Explain limited coverage in reason; reject ambiguous hits and unsupported connections.
Select only candidate IDs; the app rechecks scope and hard filters. No further model-side verification is needed.
Return articles with articleId and a nonblank reason of at most 120 characters. At most 20 articles.
NO_MATCH needs a completed search. It differs from a failed search or unavailable articles."""

    override fun admit(call: MessagePart.Tool.Call) = tool.admit(call)

    override fun snapshot(textLimit: Int) = buildJsonObject {
        put("successfulSearches", tool.successfulSearches)
        put("queries", podnyaJson.encodeToJsonElement(tool.records))
        put("knownIds", podnyaJson.encodeToJsonElement(tool.knownIds.toList()))
        put("candidates", buildJsonArray {
            tool.evidence.values.forEach { evidence ->
                add(buildJsonObject {
                    put("hit", podnyaJson.encodeToJsonElement(evidence.hit.bounded(textLimit)))
                    put(
                        "queryIndex",
                        tool.records.indexOfLast { evidence.hit.articleId in it.resultIds }
                    )
                })
            }
        })
    }

    override fun history(snapshot: AssistantHistorySnapshot, textLimit: Int) =
        podnyaJson.encodeToJsonElement(
            articleSearchHistory(
                listOf(AssistantHistoryTurn("", "", snapshot))
            ).single().articles.map {
                it.copy(article = it.article.bounded(textLimit))
            }
        )

    override suspend fun validate(kind: AssistantAnswerKind, output: JsonObject): CapabilityResult {
        val selection = podnyaJson.decodeFromJsonElement<ArticleSelection>(output)
        val answer = ArticleSearchResultValidator(source).validate(
            AssistantAnswer(kind, "", selection.articles),
            tool.scope,
            tool.evidence,
            tool.successfulSearches
        )
        logger.debug {
            "Article selection: searches=${tool.successfulSearches}, candidates=${tool.evidence.size}, " +
                    "selected=${selection.articles.size}, validated=${answer.articles.size}"
        }
        val content = ArticleSearchContent.recommendations(answer)
        val reply = buildJsonObject {
            put("successfulSearches", tool.successfulSearches)
            put(
                "articles",
                podnyaJson.encodeToJsonElement(answer.articles.map {
                    it.copy(article = it.article.bounded(160))
                })
            )
            put("hadCandidates", answer.hadCandidates)
            put("resultsChanged", answer.resultsChanged)
        }
        return CapabilityResult(content, reply, answer.articles.size)
    }

    companion object {
        fun create(
            source: LocalArticleSearchRepository,
            request: AssistantTurnRequest
        ): ArticleSearchCapability? {
            val scope = when (val context = request.context) {
                AssistantContext.Empty -> ArticleSearchScope.All
                is ArticleSearchContext -> context.snapshot().scope
                else -> return null
            }
            val knownIds = articleSearchHistory(request.history).flatMap { it.articles }
                .map { it.article.articleId }.toSet()
            return ArticleSearchCapability(
                source,
                SearchArticlesTool(source, scope, TimeZone.currentSystemDefault(), knownIds)
            )
        }
    }
}

@Serializable
private data class ArticleSelection(val articles: List<ArticleRecommendation>)

package com.skyd.podaura.nano.capabilities.articles

import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.structure.json.JsonStructure
import co.touchlab.kermit.Logger
import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.nano.capabilities.ArticleTargetSource
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleRecommendation
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleReferences
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchContent
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchContext
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchLimits
import com.skyd.podaura.nano.capabilities.articles.model.bean.AssistantAnswer
import com.skyd.podaura.nano.capabilities.articles.model.repository.ArticleSearchResultValidator
import com.skyd.podaura.nano.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.nano.core.AssistantAnswerKind
import com.skyd.podaura.nano.core.AssistantContext
import com.skyd.podaura.nano.core.AssistantHistorySnapshot
import com.skyd.podaura.nano.core.AssistantHistoryTurn
import com.skyd.podaura.nano.core.AssistantTurnRequest
import com.skyd.podaura.nano.runtime.CapabilityResult
import com.skyd.podaura.nano.runtime.KoogCapability
import com.skyd.podaura.nano.runtime.KoogCapabilityFactory
import com.skyd.podaura.nano.runtime.nanoJson
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal class ArticleSearchCapability private constructor(
    private val source: LocalArticleSearchRepository,
    private val tool: SearchArticlesTool,
    private val historyEvidence: ArticleHistoryEvidence,
    override val lookupTool: ArticleFieldLookup?,
) : KoogCapability, ArticleTargetSource {
    private val logger = Logger.withTag("ArticleSearchCapability")
    override val id = ArticleSearchContent.CAPABILITY_ID
    override val tools = listOfNotNull(tool, lookupTool)
    override val failure get() = tool.localFailure ?: lookupTool?.failure
    override val selectionSchema = JsonStructure.create<ArticleSelection>().schema.schema
    override val instructions =
        """Use search_articles for local articles and podcast episodes. Search episode topics in content. Set withinArticleIds only when the user wants to search among specific earlier results. For additional recommendations, search the authorized scope and use excludeArticleIds for requested exclusions."""
    override val selectionInstructions =
        """Select and rank local articles already retrieved for this turn. Match the requested breadth using titles and partial snippets. A mention can satisfy a request for mentions, but cannot establish a whole article's focus.
Return articles with current articleId and a nonblank reason of at most ${ArticleSearchLimits.REASON_CHARS} characters, explaining the evidence for the match."""

    override fun admit(call: MessagePart.Tool.Call) =
        if (call.tool == lookupTool?.name) lookupTool.admit(call) else tool.admit(call)

    override val recentArticleIds get() = historyEvidence.recentTitles.keys.toList()
    override val rememberedArticles get() = historyEvidence.rememberedTitles
    override val focusedArticleId get() = historyEvidence.focusedId

    override fun currentArticles(): Map<String, String> =
        tool.evidence.mapValues { it.value.hit.title }

    override fun snapshot(textLimit: Int) = buildJsonObject {
        put("successfulSearches", tool.successfulSearches)
        put("queries", nanoJson.encodeToJsonElement(tool.records))
        put("knownIds", nanoJson.encodeToJsonElement(tool.knownIds.toList()))
        put("candidates", buildJsonArray {
            tool.evidence.values.forEach { evidence ->
                add(buildJsonObject {
                    put("hit", nanoJson.encodeToJsonElement(evidence.hit.bounded(textLimit)))
                    put(
                        "queryIndex",
                        tool.records.indexOfLast { evidence.hit.articleId in it.resultIds }
                    )
                })
            }
        })
    }

    override fun toolSnapshot(textLimit: Int, lookupTitleLimit: Int) = buildJsonObject {
        snapshot(textLimit).forEach { (key, value) -> put(key, value) }
        lookupTool?.let { put("lookup", it.snapshot(lookupTitleLimit)) }
    }

    override fun history(snapshot: AssistantHistorySnapshot, textLimit: Int) =
        if (snapshot.payload.startsWith("{")) {
            ArticleReferences.from(snapshot)?.let { nanoJson.encodeToJsonElement(it.forPrompt()) }
        } else {
            nanoJson.encodeToJsonElement(
                articleSearchHistory(
                    listOf(AssistantHistoryTurn(question = "", reply = "", content = snapshot))
                ).single().articles.map { it.copy(article = it.article.bounded(textLimit)) }
            )
        }

    override suspend fun validate(kind: AssistantAnswerKind, output: JsonObject): CapabilityResult {
        val selection = nanoJson.decodeFromJsonElement<ArticleSelection>(output)
        val answer = ArticleSearchResultValidator(source).validate(
            AssistantAnswer(kind, "", selection.articles),
            tool.scope,
            tool.evidence,
            tool.successfulSearches
        )
        logger.d {
            "Article selection: searches=${tool.successfulSearches}, candidates=${tool.evidence.size}, " +
                    "selected=${selection.articles.size}, validated=${answer.articles.size}"
        }
        val content = ArticleSearchContent.recommendations(answer)
        val reply = buildJsonObject {
            put("successfulSearches", tool.successfulSearches)
            put(
                "articles",
                nanoJson.encodeToJsonElement(answer.articles.map {
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
            request: AssistantTurnRequest,
        ): ArticleSearchCapability? {
            val scope = when (val context = request.context) {
                AssistantContext.Empty -> ArticleSearchScope.All
                is ArticleSearchContext -> context.snapshot().scope
                else -> return null
            }
            val history = articleSearchHistory(request.history)
            val knownIds = history.flatMap { it.articles }.map { it.article.articleId }.toSet()
            val references = latestArticleReferences(request)
            return ArticleSearchCapability(
                source = source,
                tool = SearchArticlesTool(
                    source = source,
                    scope = scope,
                    timeZone = TimeZone.currentSystemDefault(),
                    historyIds = knownIds,
                ),
                historyEvidence = articleHistoryEvidence(latest = references, history = history),
                lookupTool = references?.takeIf { it.items.isNotEmpty() }?.let {
                    ArticleFieldLookup(
                        source = source,
                        scope = scope,
                        references = it,
                    )
                },
            )
        }
    }
}

@Serializable
private data class ArticleSelection(val articles: List<ArticleRecommendation>)

internal actual val articleSearchRuntimeModule = module {
    single<KoogCapabilityFactory>(named(ArticleSearchContent.CAPABILITY_ID)) {
        val source = get<LocalArticleSearchRepository>()
        KoogCapabilityFactory { request ->
            ArticleSearchCapability.create(
                source = source,
                request = request
            )
        }
    }
}

package com.skyd.podaura.podnya.capabilities.feeds

import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.structure.json.JsonStructure
import com.skyd.podaura.podnya.capabilities.SubscriptionReference
import com.skyd.podaura.podnya.capabilities.SubscriptionTargetSource
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedRecommendation
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchContent
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchLimits
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.RecommendedFeed
import com.skyd.podaura.podnya.capabilities.feeds.model.repository.FeedSearchResultValidator
import com.skyd.podaura.podnya.capabilities.feeds.model.repository.LocalFeedSearchRepository
import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantContext
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantHistorySnapshot
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.runtime.CapabilityResult
import com.skyd.podaura.podnya.runtime.KoogCapability
import com.skyd.podaura.podnya.runtime.KoogCapabilityFactory
import com.skyd.podaura.podnya.runtime.podnyaJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal class FeedSearchCapability private constructor(private val source: LocalFeedSearchRepository) :
    KoogCapability, SubscriptionTargetSource {
    private val tool = SearchFeedsTool(source, permittedUrls = null)
    override val id = FeedSearchContent.CAPABILITY_ID
    override val tools = listOf(tool)
    override val failure get() = tool.localFailure
    override val selectionSchema = JsonStructure.create<FeedSelection>().schema.schema
    override val instructions =
        """Use search_feeds for saved subscriptions. Episode topics require local episode evidence. Show metadata alone cannot rule out matching episodes."""
    override val selectionInstructions =
        """Select saved subscriptions whose title or description supports the user's requested breadth. Distinguish relevant coverage from a show's main focus.
Return feeds with this turn's feedId and a nonblank reason of at most ${FeedSearchLimits.REASON_CHARS} characters."""

    override fun admit(call: MessagePart.Tool.Call) = tool.admit(call)

    override fun currentSubscriptions() = tool.evidence.mapValues { (_, entry) ->
        SubscriptionReference(title = entry.feed.title, feedUrl = entry.feedUrl)
    }

    override fun snapshot(textLimit: Int) = buildJsonObject {
        put("successfulSearches", tool.successfulSearches)
        put("queries", podnyaJson.encodeToJsonElement(tool.records))
        put("feeds", podnyaJson.encodeToJsonElement(tool.evidence.values.map {
            it.feed.copy(description = it.feed.description.take(textLimit))
        }))
    }

    override fun history(snapshot: AssistantHistorySnapshot, textLimit: Int) = runCatching {
        podnyaJson.encodeToJsonElement(
            podnyaJson.decodeFromString<List<RecommendedFeed>>(snapshot.payload).take(20).map {
                it.copy(
                    feed = it.feed.copy(
                        title = it.feed.title.take(200),
                        description = it.feed.description.take(textLimit)
                    ),
                    reason = it.reason.take(120)
                )
            })
    }.getOrNull()

    override suspend fun validate(kind: AssistantAnswerKind, output: JsonObject): CapabilityResult {
        val selection = podnyaJson.decodeFromJsonElement<FeedSelection>(output)
        if (tool.successfulSearches == 0 || (kind == AssistantAnswerKind.RESULTS) != selection.feeds.isNotEmpty()) {
            throw AssistantException(AssistantError.INVALID_OUTPUT)
        }
        val content = FeedSearchResultValidator(source).validate(selection.feeds, tool.evidence)
        val reply = buildJsonObject {
            put("successfulSearches", tool.successfulSearches)
            put("feeds", podnyaJson.encodeToJsonElement(content.feeds.map {
                it.copy(feed = it.feed.copy(description = it.feed.description.take(160)))
            }))
            put("hadCandidates", content.hadCandidates)
            put("resultsChanged", content.resultsChanged)
        }
        return CapabilityResult(content, reply, content.feeds.size)
    }

    companion object {
        fun create(
            source: LocalFeedSearchRepository,
            request: AssistantTurnRequest
        ): FeedSearchCapability? =
            if (request.context == AssistantContext.Empty) FeedSearchCapability(source) else null
    }
}

@Serializable
private data class FeedSelection(val feeds: List<FeedRecommendation>)

internal actual val feedSearchRuntimeModule = module {
    single<KoogCapabilityFactory>(named(FeedSearchContent.CAPABILITY_ID)) {
        val source = get<LocalFeedSearchRepository>()
        KoogCapabilityFactory { request ->
            FeedSearchCapability.create(
                source = source,
                request = request
            )
        }
    }
}

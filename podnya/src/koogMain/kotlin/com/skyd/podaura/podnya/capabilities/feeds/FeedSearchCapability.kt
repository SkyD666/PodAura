package com.skyd.podaura.podnya.capabilities.feeds

import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.structure.json.JsonStructure
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedRecommendation
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchContent
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
import com.skyd.podaura.podnya.runtime.podnyaJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

internal class FeedSearchCapability private constructor(private val source: LocalFeedSearchRepository) :
    KoogCapability {
    private val tool = SearchFeedsTool(source, permittedUrls = null)
    override val id = FeedSearchContent.CAPABILITY_ID
    override val tools = listOf(tool)
    override val failure get() = tool.localFailure
    override val selectionSchema = JsonStructure.create<FeedSelection>().schema.schema
    override val instructions =
        """Use search_feeds for subscriptions/podcasts already saved in the app.
Search titles, nicknames and descriptions, preserving muted filters when trying synonyms.
Empty keywords lists saved subscriptions. This capability cannot discover online feeds or add subscriptions.
Feed IDs are temporary per-turn references, not article IDs. Search again before recommending history items.
Subscription URLs are kept on the device."""
    override val selectionInstructions =
        """Select relevant subscriptions from this turn's feeds snapshot.
Match the user's stated breadth: "related to X" does not require a show dedicated entirely to X unless explicitly requested.
A broader show's relevant coverage may qualify when supported by its title or description; explain that scope in reason. Do not infer unseen episode contents or specialization from incidental mentions or ambiguous keyword hits.
Return feeds with feedId and a nonblank reason of at most 120 characters. At most 20 subscriptions.
Use only current candidate IDs. NO_MATCH requires a completed subscription search.
The app rechecks saved records; no model-side verification or extra tool call is needed."""

    override fun admit(call: MessagePart.Tool.Call) = tool.admit(call)

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

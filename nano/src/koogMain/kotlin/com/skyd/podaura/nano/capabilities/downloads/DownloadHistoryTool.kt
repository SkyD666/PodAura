package com.skyd.podaura.nano.capabilities.downloads

import ai.koog.agents.core.tools.Tool
import ai.koog.serialization.typeToken
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadRequestEvidence
import com.skyd.podaura.nano.capabilities.downloads.model.PendingDownloadSelection
import com.skyd.podaura.nano.core.AssistantLimits
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
internal data class DownloadHistoryPage(val turnId: Long, val offset: Int = 0)

/** Read one user-authored message and a page of its outstanding options. */
internal class DownloadHistoryTool(
    private val evidence: DownloadRequestEvidence
) : Tool<DownloadHistoryPage, String>(
    argsType = typeToken<DownloadHistoryPage>(),
    resultType = typeToken<String>(),
    name = "read_download_history",
    description = "Read the original user message at turnId or page through its pending choices and scope clarifications. Use nextOffset for the next page.",
) {
    val messages = mutableMapOf<Long, String>()
    val pages = mutableMapOf<DownloadHistoryPage, PendingDownloadSelection>()

    override suspend fun execute(args: DownloadHistoryPage): String {
        if (args.turnId <= 0 || args.offset < 0 || evidence.currentTurnId?.let { args.turnId >= it } == true) {
            return """{"error":"INVALID_HISTORY_REFERENCE"}"""
        }
        val batch = evidence.pendingSelections.singleOrNull { it.originTurnId == args.turnId }
        val message = messages[args.turnId] ?: evidence.userMessage?.invoke(args.turnId)
        if (message == null && batch == null) return """{"error":"TURN_UNAVAILABLE"}"""
        if (message != null) messages[args.turnId] = message
        val page = buildJsonObject {
            put("turnId", args.turnId)
            if (message != null) put("userMessage", message)
            if (batch != null) {
                pages[args] = batch
                put(
                    "pending",
                    batch.page(
                        offset = args.offset,
                        limit = AssistantLimits.RESULT_ITEMS,
                        textLimit = AssistantLimits.INPUT_CHARS
                    )
                )
            }
        }
        return page.toString()
    }
}

internal fun PendingDownloadSelection.page(offset: Int, limit: Int, textLimit: Int): JsonObject {
    val labelLimit = textLimit.coerceIn(24, 150)
    val clauseLimit = if (textLimit >= 400) AssistantLimits.INPUT_CHARS else labelLimit
    val entries = sequence {
        for (scope in scopeClarifications) {
            yield(buildJsonObject {
                put("scopeClarificationId", scope.id)
                put("field", scope.field.name)
                put("originalRequest", scope.originalClause.take(clauseLimit))
                if (scope.feed != null) put("feedId", "scope-${scope.id}")
                if (scope.excludedClauses.isNotEmpty()) {
                    put(
                        "excludedClauses",
                        buildJsonArray {
                            scope.excludedClauses.forEach {
                                add(JsonPrimitive(it.take(clauseLimit)))
                            }
                        }
                    )
                }
                scope.requestDate?.let { put("requestDate", it) }
            })
        }
        for (choice in choices) {
            for (option in choice.options) {
                yield(buildJsonObject {
                    put("choiceId", choice.id)
                    put("choiceTitle", choice.title.take(labelLimit))
                    put("optionId", option.id)
                    put("title", option.title.take(labelLimit))
                })
            }
        }
    }.drop(offset).take(limit + 1).toList()
    return buildJsonObject {
        put("turnId", originTurnId)
        put("entries", buildJsonArray { entries.take(limit).forEach(::add) })
        if (entries.size > limit) put("nextOffset", offset + limit)
    }
}

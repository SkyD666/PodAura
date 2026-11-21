package com.skyd.podaura.podnya.capabilities.articles

import ai.koog.agents.core.tools.Tool
import ai.koog.prompt.message.MessagePart
import ai.koog.serialization.typeToken
import com.skyd.podaura.api.model.bean.ArticleQuery
import com.skyd.podaura.api.model.bean.ArticleSearchScope
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleField
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleReference
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleReferences
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchLimits
import com.skyd.podaura.podnya.capabilities.articles.model.bean.forArticlePrompt
import com.skyd.podaura.podnya.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.runtime.ReplyLookupTool
import com.skyd.podaura.podnya.runtime.ValidatedAssistantReply
import com.skyd.podaura.podnya.runtime.podnyaJson
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_article_choose_field
import com.skyd.podnya.resources.podnya_article_field_choose
import com.skyd.podnya.resources.podnya_article_field_missing
import com.skyd.podnya.resources.podnya_article_field_unsupported
import com.skyd.podnya.resources.podnya_article_reference_unavailable
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.getString
import kotlin.time.Instant

internal class ArticleFieldLookup(
    private val source: LocalArticleSearchRepository,
    private val scope: ArticleSearchScope,
    private val references: ArticleReferences,
    private val question: String,
) : Tool<ArticleFieldRequest, String>(
    argsType = typeToken<ArticleFieldRequest>(),
    resultType = typeToken<String>(),
    name = "lookup_article_field",
    description = """Read one field for an item in the articles.lookup snapshot. Use its id for articleId and its position for the displayed list number. choiceIds identifies the last clarification options.
Resolve each title and each display-list position in the current request independently against the lookup items. When both are given, they must refer to the same item. If any reference is missing or ambiguous, or the references point to different items, pass null for articleId so the app can ask the user. An episode's number is part of its identity; list positions come from the recommendation order. Use selectedId only for a clear implicit reference with needsSelection false.
Set articleField to TITLE, SOURCE (feed or podcast name), DATE (publication), or UNSUPPORTED for other fields. Use KEEP when the user only identifies an item for a pending field, and null if the requested field is unclear. Include the known field even when asking the user to choose the item.""",
), ReplyLookupTool {
    override var reply: ValidatedAssistantReply? = null
        private set
    var failure: AssistantFailure? = null
        private set

    private val items =
        references.items.take(ArticleSearchLimits.RECOMMENDATIONS).withIndex().toList()
    private val restricted = scope as? ArticleSearchScope.Restricted
    private var authorized =
        restricted?.let { scope -> items.filter { it.value.id in scope.articleIds } } ?: items
    private var authorizationFailure: AssistantFailure? = null

    override suspend fun prepare() {
        val scope = restricted?.takeIf { it.feedUrls.isNotEmpty() } ?: return
        val unchecked = items.map { it.value.id }.filterNot { it in scope.articleIds }
        if (unchecked.isEmpty()) return
        val fromFeeds = try {
            source.resolve(scope = scope, articleIds = unchecked, query = ArticleQuery())
                .map { it.articleId }.toSet()
        } catch (error: AssistantException) {
            authorizationFailure = error.error
            return
        }
        authorized = items.filter { it.value.id in scope.articleIds || it.value.id in fromFeeds }
    }

    fun snapshot(titleLimit: Int) = buildJsonObject {
        val ids = authorized.map { it.value.id }.toSet()
        put("items", buildJsonArray {
            for ((index, item) in authorized) {
                add(buildJsonObject {
                    put("id", item.id)
                    put("position", index + 1)
                    put("title", item.title.forArticlePrompt(titleLimit))
                    item.displayedTitle?.let {
                        put(
                            "displayedTitle",
                            it.forArticlePrompt(titleLimit)
                        )
                    }
                })
            }
        })
        put("selectedId", references.selectedId?.takeIf { it in ids })
        put("pendingField", references.pendingField?.name)
        put("needsSelection", references.needsSelection)
        put("choiceIds", podnyaJson.encodeToJsonElement(references.choiceIds.filter { it in ids }))
    }

    private fun choicesFrom(items: List<IndexedValue<ArticleReference>>) =
        items.filter { it.value.id in references.choiceIds }.ifEmpty { items }

    private fun IndexedValue<ArticleReference>.choiceLabel() =
        "${index + 1}. ${value.title.trim()}".trimEnd()

    override suspend fun resolveExact(): ValidatedAssistantReply? {
        if (!references.needsSelection) return null
        val input = question.trim()
        val number = input.toIntOrNull()?.takeIf { it.toString() == input }
        // Interpret the original display before filtering: an expired #2 cannot turn into a title match for "2".
        val numbered = choicesFrom(items).filter {
            number == it.index + 1 || input.equals(it.choiceLabel(), ignoreCase = true)
        }
        val titled = items.filter { (_, item) ->
            input.equals(item.title.trim(), ignoreCase = true) ||
                    input.equals(item.displayedTitle?.trim(), ignoreCase = true)
        }
        val matches = (numbered + titled).distinct()
        if (matches.isEmpty() && number == null) return null
        val visibleMatches = matches.filter { it in authorized }
        return resolve(
            selected = visibleMatches.singleOrNull()?.value?.takeIf { matches.size == 1 },
            field = LookupField.KEEP,
            choices = visibleMatches.takeIf { it.size > 1 }?.sortedBy { it.index } ?: choicesFrom(
                authorized
            ),
        )
    }

    fun admit(call: MessagePart.Tool.Call): MessagePart.Tool.Result? {
        val args = try {
            podnyaJson.decodeFromString<ArticleFieldRequest>(call.args)
        } catch (_: SerializationException) {
            null
        }
        return if (args == null) {
            MessagePart.Tool.Result(
                id = call.id,
                tool = call.tool,
                output = """{"error":"INVALID_ARGUMENT"}""",
                isError = true
            )
        } else null
    }

    override suspend fun execute(args: ArticleFieldRequest): String {
        try {
            reply = resolve(
                selected = authorized.singleOrNull { it.value.id == args.articleId }?.value,
                field = args.articleField,
                choices = choicesFrom(authorized),
            )
        } catch (e: AssistantException) {
            failure = e.error
        }
        return reply?.text.orEmpty()
    }

    private suspend fun resolve(
        selected: ArticleReference?,
        field: LookupField?,
        choices: List<IndexedValue<ArticleReference>>,
    ): ValidatedAssistantReply {
        val requestedField =
            if (field == LookupField.KEEP) references.pendingField else field?.field
        val state = references.copy(
            selectedId = selected?.id ?: references.selectedId,
            needsSelection = selected == null,
            pendingField = requestedField,
            choiceIds = if (selected == null) choices.map { it.value.id } else emptyList(),
        )
        if (field == LookupField.UNSUPPORTED) {
            return answer(
                kind = AssistantAnswerKind.UNSUPPORTED,
                text = getString(Res.string.podnya_article_field_unsupported),
                state = state.copy(
                    selectedId = selected?.id
                        ?: references.selectedId.takeUnless { references.needsSelection },
                    needsSelection = false,
                    choiceIds = emptyList(),
                ),
            )
        }
        if (selected == null) {
            authorizationFailure?.let { throw AssistantException(it) }
            if (choices.isEmpty()) {
                return answer(
                    kind = AssistantAnswerKind.NO_MATCH,
                    text = getString(Res.string.podnya_article_reference_unavailable),
                    state = state.copy(
                        selectedId = null,
                        pendingField = null,
                        needsSelection = false
                    ),
                )
            }
            return answer(
                kind = AssistantAnswerKind.CLARIFICATION,
                text = buildString {
                    append(getString(Res.string.podnya_article_field_choose))
                    for (choice in choices) append('\n').append(choice.choiceLabel())
                }.take(AssistantLimits.REPLY_CHARS),
                state = state,
            )
        }
        if (requestedField == null) {
            return answer(
                kind = AssistantAnswerKind.CLARIFICATION,
                text = getString(Res.string.podnya_article_choose_field),
                state = state,
            )
        }
        val row = source.record(scope = scope, articleId = selected.id) ?: return answer(
            kind = AssistantAnswerKind.NO_MATCH,
            text = getString(Res.string.podnya_article_reference_unavailable),
            state = state.copy(pendingField = null),
        )
        val answered = state.copy(pendingField = null)
        val value = when (requestedField) {
            ArticleField.TITLE -> row.title
            ArticleField.SOURCE -> row.sourceNickname?.takeIf { it.isNotBlank() } ?: row.sourceTitle
            ArticleField.DATE -> row.dateMillis?.let {
                Instant.fromEpochMilliseconds(it)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            }
        }
        val fieldText = value?.takeIf { it.isNotBlank() }?.let {
            if (it.length > AssistantLimits.REPLY_CHARS) it.take(AssistantLimits.REPLY_CHARS - 1) + "…" else it
        }
        return answer(
            kind = AssistantAnswerKind.RESULTS,
            text = fieldText ?: getString(Res.string.podnya_article_field_missing),
            state = if (requestedField == ArticleField.TITLE && fieldText != null) {
                answered.copy(items = answered.items.map { item ->
                    if (item.id == selected.id) item.copy(displayedTitle = fieldText.takeUnless { it == item.title }) else item
                })
            } else {
                answered
            },
        )
    }

    private fun answer(kind: AssistantAnswerKind, text: String, state: ArticleReferences) =
        ValidatedAssistantReply(kind = kind, text = text, results = emptyMap(), reference = state)
}

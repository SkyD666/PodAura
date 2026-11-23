package com.skyd.podaura.nano.capabilities.articles.model.bean

import com.skyd.podaura.nano.capabilities.articles.model.articleHistoryJson
import com.skyd.podaura.nano.core.AssistantAnswerKind
import com.skyd.podaura.nano.core.AssistantContent
import com.skyd.podaura.nano.core.AssistantHistorySnapshot
import com.skyd.podaura.nano.model.bean.REFERENCE_KIND_PREFIX
import kotlinx.serialization.Serializable

@Serializable
internal enum class ArticleField { TITLE, SOURCE, DATE }

@Serializable
internal data class ArticleReference(
    val id: String,
    val title: String,
    // Preserve the title returned by a previous field lookup.
    val displayedTitle: String? = null,
)

/** The last displayed order, the item currently in focus, and any unanswered field question. */
@Serializable
internal data class ArticleReferences(
    val items: List<ArticleReference>,
    val selectedId: String? = null,
    val pendingField: ArticleField? = null,
    val lastDisplayedTitle: ArticleReference? = null,
    val needsSelection: Boolean = pendingField != null,
    val choiceIds: List<String> = emptyList(),
) : AssistantContent {
    override val retainedBytes
        get() = items.sumOf {
            it.id.encodeToByteArray().size + it.title.encodeToByteArray().size +
                    (it.displayedTitle?.encodeToByteArray()?.size ?: 0) + 32
        } + (selectedId?.encodeToByteArray()?.size ?: 0) +
                (lastDisplayedTitle?.let { it.id.encodeToByteArray().size + it.title.encodeToByteArray().size }
                    ?: 0) + choiceIds.sumOf { it.encodeToByteArray().size + 8 } + 128

    override fun historySnapshot() = AssistantHistorySnapshot(
        capabilityId = ArticleSearchContent.CAPABILITY_ID,
        summary = "",
        payload = articleHistoryJson.encodeToString(this),
    )

    fun forPrompt() = copy(
        items = items.map { it.copy(displayedTitle = it.displayedTitle?.forArticlePrompt(200)) },
        lastDisplayedTitle = lastDisplayedTitle?.let {
            ArticleReference(id = it.id, title = it.title.forArticlePrompt(200))
        },
    )

    override fun promptHistorySnapshot() = forPrompt().historySnapshot()

    companion object {
        fun from(snapshot: AssistantHistorySnapshot?): ArticleReferences? {
            if (snapshot?.capabilityId != ArticleSearchContent.CAPABILITY_ID) return null
            if (snapshot.payload.startsWith("{")) {
                val decoded =
                    runCatching { articleHistoryJson.decodeFromString<ArticleReferences>(snapshot.payload) }
                        .getOrNull() ?: return null
                val state = if (snapshot.summary.startsWith(REFERENCE_KIND_PREFIX) &&
                    snapshot.summary.removePrefix(REFERENCE_KIND_PREFIX) != AssistantAnswerKind.CLARIFICATION.name
                ) {
                    decoded.copy(
                        selectedId = decoded.selectedId.takeUnless { decoded.needsSelection },
                        pendingField = null,
                        needsSelection = false,
                        choiceIds = emptyList(),
                    )
                } else decoded
                val legacy = state.lastDisplayedTitle ?: return state
                return state.copy(
                    items = state.items.map { item ->
                        if (item.id == legacy.id) {
                            item.copy(displayedTitle = item.displayedTitle ?: legacy.title)
                        } else {
                            item
                        }
                    },
                    lastDisplayedTitle = null,
                )
            }
            val items = ArticleSearchContent.historyArticles(snapshot)
                .take(ArticleSearchLimits.RECOMMENDATIONS)
                .takeIf { articles -> articles.all { it.article.articleId.isNotBlank() && it.article.articleId.length <= 256 } }
                ?: return null
            val refs = items.map {
                ArticleReference(id = it.article.articleId, title = it.article.title.take(200))
            }
            return ArticleReferences(items = refs, selectedId = refs.singleOrNull()?.id)
        }
    }
}

internal fun String.forArticlePrompt(maxChars: Int): String {
    if (length <= maxChars) return this
    val startChars = maxChars / 2
    val prefix = take(startChars).dropLastWhile(Char::isHighSurrogate)
    val suffix = takeLast(maxChars - startChars - 1).dropWhile(Char::isLowSurrogate)
    return "$prefix…$suffix"
}

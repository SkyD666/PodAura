package com.skyd.podaura.nano.capabilities.downloads

import ai.koog.agents.core.tools.Tool
import ai.koog.serialization.typeToken
import com.skyd.podaura.api.model.bean.download.AssistantDownloadArticle
import com.skyd.podaura.api.model.bean.download.AssistantDownloadAttachment
import com.skyd.podaura.api.model.bean.download.AssistantDownloadTarget
import com.skyd.podaura.nano.capabilities.downloads.selection.downloadLinkLabel
import com.skyd.podaura.nano.core.AssistantLimits
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
internal data class AttachmentPage(val articleId: String, val offset: Int = 0)

internal class DownloadAttachmentsTool(
    private val article: suspend (String) -> AssistantDownloadArticle?,
) : Tool<AttachmentPage, String>(
    argsType = typeToken<AttachmentPage>(),
    resultType = typeToken<String>(),
    name = "list_download_attachments",
    description = "List actual files for a verified articleId. Use nextOffset for more files.",
) {
    private data class Entry(val id: String, val file: AssistantDownloadAttachment)

    private val listed = mutableMapOf<AssistantDownloadTarget.Attachment, Entry>()
    private val totals = mutableMapOf<String, Int>()
    private var nextId = 1
    val targets get() = listed.entries.associate { (target, entry) -> entry.id to target }

    override suspend fun execute(args: AttachmentPage): String {
        if (args.offset < 0) return """{"error":"INVALID_OFFSET"}"""
        val row = article(args.articleId) ?: return """{"error":"ARTICLE_UNAVAILABLE"}"""
        val limit = AssistantLimits.RESULT_ITEMS
        val files = row.attachments.associateBy { it.url }
        totals[row.articleId] = files.size
        listed.keys.removeAll { it.articleId == row.articleId && it.url !in files }
        val page = files.values.drop(args.offset).take(limit)

        return buildJsonObject {
            put("articleId", row.articleId)
            put("totalFiles", files.size)
            put("files", buildJsonArray {
                for (file in page) {
                    val target = AssistantDownloadTarget.Attachment(
                        articleId = row.articleId,
                        url = file.url
                    )
                    val id = listed[target]?.id ?: "file-${nextId++}"
                    val entry = Entry(id = id, file = file)
                    listed[target] = entry
                    add(entry.describe(row.articleId, 150))
                }
            })
            if (args.offset < files.size - page.size) put("nextOffset", args.offset + page.size)
        }.toString()
    }

    fun snapshot(labelLimit: Int) = buildJsonArray {
        for ((articleId, total) in totals) {
            add(buildJsonObject {
                put("articleId", articleId)
                put("totalFiles", total)
                put("files", buildJsonArray {
                    listed.forEach { (target, entry) ->
                        if (target.articleId == articleId) add(
                            entry.describe(
                                articleId,
                                labelLimit
                            )
                        )
                    }
                })
            })
        }
    }

    private fun Entry.describe(articleId: String, labelLimit: Int) = buildJsonObject {
        put("articleId", articleId)
        put("attachmentId", id)
        val name = downloadLinkLabel(file.url, maxLength = Int.MAX_VALUE)
        val prefix = labelLimit / 2
        put(
            "fileName",
            if (name.length <= labelLimit) name else name.take(prefix) + "…" + name.takeLast(
                labelLimit - prefix - 1
            )
        )
        file.type?.let { put("mimeType", it.take(100)) }
        if (file.length > 0) put("bytes", file.length)
        put("mediaKind", file.mediaKind.name)
    }
}

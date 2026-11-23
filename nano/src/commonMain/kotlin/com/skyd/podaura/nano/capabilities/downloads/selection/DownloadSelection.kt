package com.skyd.podaura.nano.capabilities.downloads.selection

import com.skyd.podaura.api.model.bean.download.AssistantDownloadTarget
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadChoice
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadExclusion
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadIssue
import com.skyd.podaura.nano.core.AssistantHandoff
import io.ktor.http.Url
import kotlinx.serialization.Serializable

data class ValidatedDownloadPlan(
    val targets: List<AssistantDownloadTarget>,
    val choices: List<DownloadChoice>,
    val issues: List<DownloadIssue>,
    val resolvedChoiceIds: Set<String> = emptySet(),
    val originTurnId: Long? = null,
) : AssistantHandoff

internal fun downloadLinkLabel(input: String, maxLength: Int = 180): String {
    val parsed = runCatching { Url(input) }.getOrNull()
    return parsed?.segments?.lastOrNull { it.isNotBlank() }?.take(maxLength)
        ?: parsed?.host?.take(maxLength).orEmpty()
}

@Serializable
internal data class DownloadSelection(
    val recommendationPositions: List<Int> = emptyList(),
    val articleIds: List<String> = emptyList(),
    val articleAlternatives: List<List<String>> = emptyList(),
    val feeds: List<DownloadFeedSelection> = emptyList(),
    val directLinks: List<DownloadLink> = emptyList(),
    val choices: List<DownloadChoiceSelection> = emptyList(),
    val attachmentIds: List<String> = emptyList(),
    val allAttachments: Boolean = false,
    val attachmentFormat: String? = null,
    val exclusions: List<DownloadExclusion> = emptyList(),
    val scopeClarificationId: String? = null,
)

/** The model supplies a URL and its verbatim surrounding text. The source message stays opaque to the app. */
@Serializable
internal data class DownloadLink(
    val url: String,
    val before: String = "",
    val after: String = "",
)

@Serializable
internal data class DownloadFeedSelection(
    val feedId: String? = null,
    val publishedFrom: String? = null,
    val publishedBefore: String? = null,
    val latest: Int? = null,
    val allEpisodes: Boolean = false,
    val attachmentFormat: String? = null,
)

@Serializable
internal data class DownloadChoiceSelection(
    val choiceId: String,
    val optionIds: List<String> = emptyList(),
    val allOptions: Boolean = false,
)

package com.skyd.podaura.nano.capabilities.downloads.model

import com.skyd.podaura.api.model.bean.download.AssistantDownloadTarget
import com.skyd.podaura.nano.capabilities.SubscriptionReference
import kotlinx.serialization.Serializable

@Serializable
data class DownloadChoice(val id: String, val title: String, val options: List<DownloadOption>)

@Serializable
data class DownloadOption(
    val id: String,
    val title: String,
    val targets: List<AssistantDownloadTarget>
)

@Serializable
data class DownloadIssue(
    val title: String,
    val kind: DownloadIssueKind,
    val scopeClarification: DownloadScopeClarification? = null,
)

@Serializable
enum class DownloadIssueKind {
    MISSING_ATTACHMENT, FILE_LINK_REQUIRED, LINK_CHECK_FAILED, ARTICLE_UNAVAILABLE, NO_MATCH, STALE_CHOICE,
    CLARIFICATION_REQUIRED,
}

@Serializable
enum class DownloadScopeField { SUBSCRIPTION, RANGE }

typealias DownloadFeedReference = SubscriptionReference

@Serializable
enum class DownloadExclusionKind { EPISODE, FORMAT, FILENAME }

@Serializable
data class DownloadExclusion(
    val kind: DownloadExclusionKind,
    val value: String,
    val episodeTitle: String? = null,
)

@Serializable
data class DownloadScopeClarification(
    val id: String,
    val originalClause: String,
    val field: DownloadScopeField,
    val feed: DownloadFeedReference? = null,
    val attachmentFormat: String? = null,
    val excludedClauses: List<String> = emptyList(),
    val requestDate: String? = null,
    val verifiedExclusionSubjects: Set<String> = emptySet(),
    val verifiedExclusionFilenames: Set<String> = emptySet(),
    val exclusions: List<DownloadExclusion> = emptyList(),
    val verifiedFileExclusions: Set<DownloadExclusion> = emptySet(),
    val allAttachments: Boolean = false,
    val allEpisodes: Boolean = false,
    val latest: Int? = null,
    val publishedFrom: String? = null,
    val publishedBefore: String? = null,
)

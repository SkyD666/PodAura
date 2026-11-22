package com.skyd.podaura.podnya.capabilities.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadContent
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadIssue
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadIssueKind
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadSubmission
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadSubmissionState
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.ui.AssistantCapabilityUi
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_download_choices_on_origin
import com.skyd.podnya.resources.podnya_download_choose
import com.skyd.podnya.resources.podnya_download_clarification_required
import com.skyd.podnya.resources.podnya_download_failed
import com.skyd.podnya.resources.podnya_download_file_link_required
import com.skyd.podnya.resources.podnya_download_limit
import com.skyd.podnya.resources.podnya_download_link_check_failed
import com.skyd.podnya.resources.podnya_download_missing_attachment
import com.skyd.podnya.resources.podnya_download_no_match
import com.skyd.podnya.resources.podnya_download_open
import com.skyd.podnya.resources.podnya_download_refresh
import com.skyd.podnya.resources.podnya_download_result
import com.skyd.podnya.resources.podnya_download_stale_choice
import com.skyd.podnya.resources.podnya_download_submitting
import com.skyd.podnya.resources.podnya_download_unavailable_article
import com.skyd.podnya.resources.podnya_download_unconfirmed
import org.jetbrains.compose.resources.stringResource

internal object DownloadUi : AssistantCapabilityUi {
    override fun supports(content: AssistantContent) = content is DownloadContent

    override fun itemKeys(
        turnId: String,
        content: AssistantContent,
        showFallbackText: Boolean
    ): List<String> {
        if (content !is DownloadContent) return emptyList()
        return buildList {
            add("$turnId-download")
            content.visibleIssues.indices.forEach { add("$turnId-download-issue-$it") }
            content.availableChoices.forEach { add("$turnId-download-choice-${it.id}") }
        }
    }

    override fun LazyListScope.render(
        turnId: String,
        content: AssistantContent,
        showFallbackText: Boolean,
        formatDate: (Long) -> String,
        onAction: (AssistantAction) -> Unit,
    ) {
        if (content !is DownloadContent) return
        val messageId = turnId.substringBefore('-').toLong()
        item(key = "$turnId-download") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content.submissions.forEach { submission ->
                    Text(
                        text = submissionText(submission),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (content.choicesAddedToOrigin) {
                    Text(
                        text = stringResource(Res.string.podnya_download_choices_on_origin),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        onAction(
                            OpenDownloads(
                                originTurnId = content.originTurnId ?: messageId,
                                submissionIds = content.submissions.map { it.id },
                            )
                        )
                    }) {
                        Text(stringResource(Res.string.podnya_download_open))
                    }
                    if (content.submissions.any { it.receipt == null }) {
                        TextButton(onClick = { onAction(RefreshDownloadStatus(messageId)) }) {
                            Text(stringResource(Res.string.podnya_download_refresh))
                        }
                    }
                }
            }
        }
        content.visibleIssues.forEachIndexed { index, issue ->
            item(key = "$turnId-download-issue-$index") {
                Text(text = issueText(issue), style = MaterialTheme.typography.bodyMedium)
            }
        }
        content.availableChoices.forEach { choice ->
            item(key = "$turnId-download-choice-${choice.id}") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.podnya_download_choose, choice.title),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    choice.options.forEach { option ->
                        TextButton(onClick = {
                            onAction(
                                SelectDownloadOption(
                                    turnId = messageId,
                                    choiceId = choice.id,
                                    optionId = option.id
                                )
                            )
                        }) {
                            Text("${option.id}. ${option.title}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun submissionText(submission: DownloadSubmission): String {
    val receipt = submission.receipt
    return when {
        receipt != null -> {
            val result = stringResource(
                Res.string.podnya_download_result,
                receipt.added,
                receipt.existing,
                receipt.failed
            )
            if (receipt.limitExceeded) {
                stringResource(
                    Res.string.podnya_download_limit,
                    receipt.matchedCount,
                    receipt.remainingCount
                ) + "\n" + result
            } else {
                result
            }
        }

        submission.state == DownloadSubmissionState.SUBMITTING -> stringResource(Res.string.podnya_download_submitting)
        submission.state == DownloadSubmissionState.FAILED -> stringResource(
            Res.string.podnya_download_failed, submission.requestedCount,
        )

        else -> stringResource(Res.string.podnya_download_unconfirmed)
    }
}

@Composable
private fun issueText(issue: DownloadIssue): String = when (issue.kind) {
    DownloadIssueKind.MISSING_ATTACHMENT -> stringResource(
        Res.string.podnya_download_missing_attachment,
        issue.title
    )

    DownloadIssueKind.FILE_LINK_REQUIRED -> stringResource(Res.string.podnya_download_file_link_required)
    DownloadIssueKind.LINK_CHECK_FAILED -> stringResource(Res.string.podnya_download_link_check_failed)
    DownloadIssueKind.ARTICLE_UNAVAILABLE -> stringResource(
        Res.string.podnya_download_unavailable_article,
        issue.title
    )

    DownloadIssueKind.NO_MATCH -> stringResource(Res.string.podnya_download_no_match)
    DownloadIssueKind.STALE_CHOICE -> stringResource(Res.string.podnya_download_stale_choice)
    DownloadIssueKind.CLARIFICATION_REQUIRED -> stringResource(
        Res.string.podnya_download_clarification_required,
        issue.title
    )
}

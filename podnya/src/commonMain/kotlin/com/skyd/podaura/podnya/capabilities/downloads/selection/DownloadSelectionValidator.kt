package com.skyd.podaura.podnya.capabilities.downloads.selection

import com.skyd.podaura.api.model.bean.download.AttachmentMediaKind
import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.api.model.bean.download.AssistantDownloadArticle
import com.skyd.podaura.api.model.bean.download.AssistantDownloadAttachment
import com.skyd.podaura.api.model.bean.download.AssistantDownloadScope
import com.skyd.podaura.api.model.bean.download.AssistantDownloadTarget
import com.skyd.podaura.api.model.repository.IAssistantDownloadRepository
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadChoice
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadExclusion
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadExclusionKind
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadFeedReference
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadIssue
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadIssueKind
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadOption
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadScopeClarification
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadScopeField
import com.skyd.podaura.podnya.capabilities.downloads.model.downloadEvidence
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.Uuid

/** Checks a structured selection against local data. The model interprets the user's message. */
internal class DownloadSelectionValidator(
    private val repository: IAssistantDownloadRepository,
    private val scope: ArticleSearchScope,
    private val timeZone: TimeZone,
) {
    suspend fun validate(
        selection: DownloadSelection,
        request: AssistantTurnRequest,
        knownArticles: Map<String, String>,
        feeds: Map<String, DownloadFeedReference>,
        recentArticleIds: List<String> = emptyList(),
        currentArticleIds: Set<String> = emptySet(),
        priorArticleIds: Set<String> = knownArticles.keys - currentArticleIds,
        contextFeedUrls: Set<String> = emptySet(),
        citedMessages: Collection<String> = emptyList(),
        listedAttachments: Map<String, AssistantDownloadTarget.Attachment> = emptyMap(),
    ): ValidatedDownloadPlan {
        checkSelection(selection.exclusions.all {
            it.value.isNotBlank() && (it.episodeTitle == null || it.episodeTitle.isNotBlank() && it.kind != DownloadExclusionKind.EPISODE) &&
                    (it.kind != DownloadExclusionKind.FORMAT || it.value.isFormat())
        })
        checkSelection(selection.attachmentFormat == null || selection.attachmentFormat.isFormat())
        val picked = selection.attachmentIds.map { listedAttachments[it] ?: invalidSelection() }.toSet()
        val pending = request.downloadEvidence.pendingSelections
        val clarification = selection.scopeClarificationId?.let { id ->
            pending.flatMap { batch ->
                batch.scopeClarifications.filter { it.id == id }.map { batch.originTurnId to it }
            }.singleOrNull() ?: invalidSelection()
        }
        val saved = clarification?.second
        if (selection.choices.isNotEmpty()) {
            checkSelection(
                selection == DownloadSelection(
                    choices = selection.choices,
                    exclusions = selection.exclusions
                )
            )
            return resolveChoices(selection, request)
        }
        if (saved != null) {
            checkSelection(
                selection.feeds.size == 1 && selection.articleIds.isEmpty() &&
                        selection.articleAlternatives.isEmpty() &&
                        selection.directLinks.isEmpty() &&
                        selection.recommendationPositions.isEmpty()
            )
            // Older records carry the original exclusions as text. The model must supply their structured interpretation.
            if (saved.excludedClauses.isNotEmpty() && saved.exclusions.isEmpty() && selection.exclusions.isEmpty()) {
                return ValidatedDownloadPlan(
                    targets = emptyList(),
                    choices = emptyList(),
                    issues = listOf(
                        DownloadIssue(
                            title = saved.originalClause,
                            kind = DownloadIssueKind.CLARIFICATION_REQUIRED
                        )
                    ),
                    originTurnId = clarification.first,
                )
            }
        }
        val exclusions = (saved?.exclusions.orEmpty() + selection.exclusions).distinct()
        val positioned = selection.recommendationPositions.map { position ->
            recentArticleIds.getOrNull(position - 1) ?: invalidSelection()
        }
        val singles = (selection.articleIds + positioned).distinct()
        val alternatives = selection.articleAlternatives.map {
            it.distinct().also { group -> checkSelection(group.size >= 2) }
        }
        checkSelection(singles.none { id -> alternatives.any { id in it } })
        val ids = (singles + alternatives.flatten()).distinct()
        checkSelection(ids.all(knownArticles::containsKey))
        checkSelection(ids.isNotEmpty() || selection.feeds.isNotEmpty() || selection.directLinks.isNotEmpty())

        val choices = mutableListOf<DownloadChoice>()
        val targets = mutableListOf<AssistantDownloadTarget>()
        val issues = mutableListOf<DownloadIssue>()

        // A single proposed ID cannot disambiguate two verified episodes with the same title.
        fun titleKey(id: String) = knownArticles.getValue(id).trim().lowercase()
        val byTitle = knownArticles.keys.groupBy(::titleKey)
        val groups = alternatives.toMutableList()
        val direct = singles.filter { id ->
            val named = byTitle.getValue(titleKey(id))
            val previous = named.filter(priorArticleIds::contains)
            val sameTitle =
                if (id in currentArticleIds) named.filter(currentArticleIds::contains) else previous
            if (id in positioned || singles.containsAll(sameTitle) ||
                id in previous && singles.containsAll(previous)
            ) {
                true
            } else {
                if (groups.none { it.containsAll(sameTitle) }) groups += sameTitle
                false
            }
        }
        val wanted = (direct + groups.flatten()).toSet()
        val articles = if (wanted.isEmpty()) {
            emptyMap()
        } else {
            repository.articlesByIds(wanted.toList())
                .filter { it.articleId in wanted && authorized(it) }.associateBy { it.articleId }
        }
        for (id in wanted - articles.keys) {
            issues += DownloadIssue(
                title = knownArticles.getValue(id),
                kind = DownloadIssueKind.ARTICLE_UNAVAILABLE
            )
        }
        val allAttachments = selection.allAttachments || saved?.allAttachments == true
        checkSelection(picked.isEmpty() || !allAttachments)
        val format = saved?.attachmentFormat ?: selection.attachmentFormat
        val feedRows = mutableListOf<Pair<DownloadFeedSelection, List<AssistantDownloadArticle>>>()
        for (proposal in selection.feeds.distinct()) {
            checkSelection(proposal.attachmentFormat == null || proposal.attachmentFormat.isFormat())
            val candidate = proposal.feedId?.let { feeds[it] ?: invalidSelection() }
            val feed = candidate?.takeIf { proposed ->
                saved?.feed?.feedUrl == proposed.feedUrl ||
                        contextFeedUrls.singleOrNull() == proposed.feedUrl ||
                        feeds.values.none {
                            it.feedUrl != proposed.feedUrl && it.title.equals(
                                proposed.title, ignoreCase = true
                            )
                        }
            }
            if (saved?.feed != null) checkSelection(feed?.feedUrl == saved.feed.feedUrl)
            if (feed != null) {
                checkSelection(scope !is ArticleSearchScope.Restricted || feed.feedUrl in scope.feedUrls)
            }
            val filter = proposal.copy(
                publishedFrom = saved?.publishedFrom ?: proposal.publishedFrom,
                publishedBefore = saved?.publishedBefore ?: proposal.publishedBefore,
                latest = saved?.latest ?: proposal.latest,
                allEpisodes = proposal.allEpisodes || saved?.allEpisodes == true,
                attachmentFormat = saved?.attachmentFormat ?: proposal.attachmentFormat ?: format,
            )
            checkSelection(filter.latest == null || filter.latest > 0)
            val from = filter.publishedFrom?.let {
                LocalDate.parse(it).atStartOfDayIn(timeZone).toEpochMilliseconds()
            }
            val before = filter.publishedBefore?.let {
                LocalDate.parse(it).atStartOfDayIn(timeZone).toEpochMilliseconds()
            }
            checkSelection(from == null || before == null || from < before)
            if (feed == null || !filter.allEpisodes && from == null && before == null && filter.latest == null) {
                val pendingScope = DownloadScopeClarification(
                    id = Uuid.random().toString(),
                    originalClause = saved?.originalClause ?: request.question,
                    field = if (feed == null) DownloadScopeField.SUBSCRIPTION else DownloadScopeField.RANGE,
                    feed = feed,
                    attachmentFormat = filter.attachmentFormat,
                    exclusions = exclusions,
                    allAttachments = allAttachments,
                    allEpisodes = filter.allEpisodes,
                    requestDate = saved?.requestDate ?: Clock.System.now()
                        .toLocalDateTime(timeZone).date.toString(),
                    latest = filter.latest,
                    publishedFrom = filter.publishedFrom,
                    publishedBefore = filter.publishedBefore,
                )
                issues += DownloadIssue(
                    title = feed?.title ?: pendingScope.originalClause,
                    kind = DownloadIssueKind.CLARIFICATION_REQUIRED,
                    scopeClarification = pendingScope,
                )
                continue
            }
            val query = AssistantDownloadScope(
                feedUrl = feed.feedUrl,
                publishedFromMillis = from,
                publishedBeforeMillis = before,
                limit = filter.latest.takeIf { exclusions.isEmpty() && filter.attachmentFormat == null },
            )
            val rows = repository.articlesInFeed(query)
                .filter { it.feedUrl == feed.feedUrl && authorized(it) }
            feedRows += filter to rows
        }

        if (exclusions.isNotEmpty()) {
            val rows = articles.values + feedRows.flatMap { it.second }
            val titles =
                knownArticles.values + rows.map { it.title } + saved?.verifiedExclusionSubjects.orEmpty()
            val verifiedFiles = exclusions.verifiedFiles(rows) +
                    saved?.verifiedFileExclusions.orEmpty() +
                    saved?.verifiedExclusionFilenames.orEmpty()
                        .map { DownloadExclusion(DownloadExclusionKind.FILENAME, it) }
            val subjects = exclusions.mapNotNull { it.episodeTitle } +
                    exclusions.filter { it.kind == DownloadExclusionKind.EPISODE }.map { it.value }
            val verifiedTitles = subjects.filterTo(mutableSetOf()) { value ->
                titles.any { it.equals(value, ignoreCase = true) }
            }
            for ((index, issue) in issues.withIndex()) {
                val pendingScope = issue.scopeClarification ?: continue
                issues[index] = issue.copy(
                    scopeClarification = pendingScope.copy(
                        verifiedExclusionSubjects = verifiedTitles,
                        verifiedFileExclusions = verifiedFiles,
                    )
                )
            }
            val unresolved = exclusions.unverified(verifiedTitles, verifiedFiles)
            if (unresolved != null && (rows.isNotEmpty() || selection.directLinks.isNotEmpty())) {
                return ValidatedDownloadPlan(
                    targets = emptyList(),
                    choices = emptyList(),
                    issues = issues + DownloadIssue(
                        title = unresolved.value,
                        kind = DownloadIssueKind.CLARIFICATION_REQUIRED
                    ),
                    originTurnId = clarification?.first,
                )
            }
        }
        val pickedUrls = picked.groupBy({ it.articleId }, { it.url })
        checkSelection(pickedUrls.keys.all { id ->
            id in ids && articles[id]?.let { row -> exclusions.none { it.excludes(row) } } == true
        })
        val missingAttachments = mutableMapOf<String, DownloadIssue>()
        fun missingAttachment(article: AssistantDownloadArticle) {
            missingAttachments[article.articleId] =
                DownloadIssue(article.title, DownloadIssueKind.MISSING_ATTACHMENT)
        }

        fun collect(article: AssistantDownloadArticle, attachmentFormat: String?, urls: List<String> = emptyList()) {
            if (exclusions.any { it.excludes(article) }) return
            val options = article.options(
                format = attachmentFormat,
                allAttachments = allAttachments,
                exclusions = exclusions,
                pickedUrls = urls,
            )
            when {
                options.size > 1 -> choices += choice(article.title, options)
                options.size == 1 -> targets += options.single().targets
                article.attachments.isEmpty() || exclusions.none { it.kind != DownloadExclusionKind.EPISODE } ->
                    missingAttachment(article)
            }
        }
        direct.mapNotNull(articles::get).forEach { row ->
            collect(article = row, attachmentFormat = format, urls = pickedUrls[row.articleId].orEmpty())
        }
        for (group in groups.distinct()) {
            val options = group.mapNotNull(articles::get)
                .filterNot { row -> exclusions.any { it.excludes(row) } }
                .flatMap { row ->
                    val versions = row.options(
                        format = format,
                        allAttachments = allAttachments,
                        exclusions = exclusions,
                        pickedUrls = pickedUrls[row.articleId].orEmpty(),
                    )
                    if (row.attachments.isEmpty()) missingAttachment(row)
                    val namesake = group.count { titleKey(it) == titleKey(row.articleId) } > 1
                    versions.map { option ->
                        val label = if (namesake && option.title == row.title) {
                            option.targets.joinToString { downloadLinkLabel(it.url) }
                        } else {
                            option.title
                        }
                        option.copy(title = if (versions.size == 1 && !namesake) row.title else "${row.title} · $label")
                    }
                }
            if (options.isNotEmpty()) choices += choice(group.joinToString(" / ") {
                knownArticles.getValue(it)
            }.take(360), options)
        }
        for ((filter, rows) in feedRows) {
            val eligible = rows.filterNot { row -> exclusions.any { it.excludes(row) } }
                .filter { row ->
                    row.attachments.isEmpty() && filter.attachmentFormat == null ||
                            row.attachments.any { file ->
                                (filter.attachmentFormat == null || file.matches(filter.attachmentFormat)) &&
                                        exclusions.none { it.excludes(row, file) }
                            }
                }
            if (eligible.isEmpty()) {
                issues += DownloadIssue(
                    title = feeds.getValue(checkNotNull(filter.feedId)).title,
                    kind = DownloadIssueKind.NO_MATCH
                )
            }
            (filter.latest?.let(eligible::take) ?: eligible).forEach {
                collect(it, filter.attachmentFormat)
            }
        }
        val urls = selection.directLinks.map { link ->
            val address = Url(link.url)
            checkSelection(address.protocol == URLProtocol.HTTP || address.protocol == URLProtocol.HTTPS)
            checkSelection(
                link.url.startsWith("${address.protocol.name}://", ignoreCase = true) &&
                        address.host.isNotBlank() &&
                        link.url.none(Char::isWhitespace)
            )
            val source = link.before + link.url + link.after
            checkSelection(
                source == request.question ||
                        request.history.any { it.question == source } ||
                        source in citedMessages
            )
            link.url
        }.distinct()
        for ((url, failure) in checkLinks(urls)) {
            if (failure == null) {
                targets += AssistantDownloadTarget.DirectLink(url)
            } else {
                issues += DownloadIssue(downloadLinkLabel(url), failure)
            }
        }
        if (targets.isEmpty() && choices.isEmpty() && issues.isEmpty() && missingAttachments.isEmpty()) {
            issues += DownloadIssue(request.question, DownloadIssueKind.NO_MATCH)
        }
        return ValidatedDownloadPlan(
            targets = targets.distinctBy { it.key },
            choices = choices.distinctBy { it.options },
            issues = issues.distinct() + missingAttachments.values,
            resolvedChoiceIds = setOfNotNull(saved?.id),
            originTurnId = clarification?.first,
        )
    }

    private suspend fun resolveChoices(
        selection: DownloadSelection,
        request: AssistantTurnRequest
    ): ValidatedDownloadPlan {
        val choices =
            request.downloadEvidence.pendingSelections.flatMap { batch -> batch.choices.map { batch.originTurnId to it } }
        checkSelection(
            selection.choices.map { it.choiceId }.distinct().size == selection.choices.size
        )
        val chosen = selection.choices.map { pick ->
            val (origin, choice) = choices.singleOrNull { it.second.id == pick.choiceId }
                ?: invalidSelection()
            checkSelection(pick.allOptions == pick.optionIds.isEmpty())
            checkSelection(pick.optionIds.all { id -> choice.options.any { it.id == id } })
            val options = if (pick.allOptions) {
                choice.options
            } else {
                choice.options.filter { it.id in pick.optionIds }
            }
            Triple(origin, choice, options.flatMap { it.targets })
        }
        val origin = chosen.map { it.first }.distinct().singleOrNull() ?: invalidSelection()
        val ids = chosen
            .flatMap { it.third }
            .filterIsInstance<AssistantDownloadTarget.Attachment>()
            .map { it.articleId }
            .distinct()
        val articles = repository.articlesByIds(ids)
            .filter(::authorized)
            .associateBy { it.articleId }
        val unresolved = selection.exclusions.unverified(
            titles = articles.values.map { it.title },
            files = selection.exclusions.verifiedFiles(articles.values),
        )
        if (unresolved != null) {
            return ValidatedDownloadPlan(
                targets = emptyList(),
                choices = emptyList(),
                issues = listOf(
                    DownloadIssue(
                        title = unresolved.value,
                        kind = DownloadIssueKind.CLARIFICATION_REQUIRED
                    )
                ),
                originTurnId = origin,
            )
        }
        val targets = mutableListOf<AssistantDownloadTarget>()
        val issues = mutableListOf<DownloadIssue>()
        val resolved = mutableSetOf<String>()
        for ((_, choice, selected) in chosen) {
            val live = selected
                .filterIsInstance<AssistantDownloadTarget.Attachment>()
                .filter { target ->
                    articles[target.articleId]?.attachments?.any { it.url == target.url } == true
                }
            if (live.size != selected.size || live.isEmpty()) {
                issues += DownloadIssue(choice.title, DownloadIssueKind.STALE_CHOICE)
                continue
            }
            val allowed = live.filter { target ->
                val row = articles.getValue(target.articleId)
                val file = row.attachments.first { it.url == target.url }
                selection.exclusions.none { it.excludes(row) || it.excludes(row, file) }
            }
            if (allowed.isEmpty()) {
                issues += DownloadIssue(choice.title, DownloadIssueKind.CLARIFICATION_REQUIRED)
            } else {
                targets += allowed
                resolved += choice.id
            }
        }
        return ValidatedDownloadPlan(
            targets = targets.distinctBy { it.key },
            choices = emptyList(),
            issues = issues,
            resolvedChoiceIds = resolved,
            originTurnId = origin,
        )
    }

    fun authorized(article: AssistantDownloadArticle): Boolean = when (scope) {
        ArticleSearchScope.All -> true
        is ArticleSearchScope.Restricted -> article.articleId in scope.articleIds || article.feedUrl in scope.feedUrls
    }

    private suspend fun checkLinks(urls: List<String>): List<Pair<String, DownloadIssueKind?>> =
        coroutineScope {
            val probes = Semaphore(8)
            urls.map { url ->
                async {
                    val failure = probes.withPermit {
                        try {
                            if (repository.checkDirectLink(url)) null else DownloadIssueKind.FILE_LINK_REQUIRED
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            DownloadIssueKind.LINK_CHECK_FAILED
                        }
                    }
                    url to failure
                }
            }.awaitAll()
        }

    private fun choice(title: String, options: List<DownloadOption>) = DownloadChoice(
        id = Uuid.random().toString(),
        title = title,
        options = options.mapIndexed { index, option -> option.copy(id = (index + 1).toString()) },
    )
}

private fun AssistantDownloadAttachment.label() = listOfNotNull(
    downloadLinkLabel(url).takeIf(String::isNotBlank),
    type?.takeIf(String::isNotBlank),
    length.takeIf { it > 0 }?.let { bytes ->
        val (unit, label) = when {
            bytes >= 1_000_000_000 -> 1_000_000_000 to "GB"
            bytes >= 1_000_000 -> 1_000_000 to "MB"
            bytes >= 1_000 -> 1_000 to "kB"
            else -> 1 to "B"
        }
        val fraction = (bytes % unit) * 10 / unit
        if (fraction == 0L) "${bytes / unit} $label" else "${bytes / unit}.$fraction $label"
    },
).distinct().joinToString(" · ")

private fun AssistantDownloadAttachment.matches(requested: String): Boolean {
    val mime = type.orEmpty().substringBefore(';').trim().lowercase()
    val format = requested.lowercase()
    val extension = downloadLinkLabel(url, maxLength = Int.MAX_VALUE).substringAfterLast('.', "").lowercase()
    return when (format) {
        "audio" -> mediaKind == AttachmentMediaKind.Audio
        "video" -> mediaKind == AttachmentMediaKind.Video
        "playlist" -> mediaKind == AttachmentMediaKind.Playlist
        else -> if (format.startsWith('.')) {
            extension == format.removePrefix(".")
        } else {
            mime == format || mime.substringAfterLast('/') == format || extension == format
        }
    }
}

private fun List<DownloadExclusion>.verifiedFiles(rows: Collection<AssistantDownloadArticle>) =
    filterTo(mutableSetOf()) { filter ->
        filter.kind == DownloadExclusionKind.FILENAME && rows.any { row ->
            row.attachments.any { filter.excludes(row, it) }
        }
    }

private fun List<DownloadExclusion>.unverified(
    titles: Collection<String>,
    files: Collection<DownloadExclusion>
) =
    firstOrNull { filter ->
        filter.episodeTitle?.let { title ->
            titles.none { it.equals(title, ignoreCase = true) }
        } == true || filter.kind == DownloadExclusionKind.EPISODE && titles.none {
            it.equals(filter.value, ignoreCase = true)
        } || filter.kind == DownloadExclusionKind.FILENAME && filter !in files
    }

/** A normalized file extension, media category, or MIME type in the model output. */
private fun String.isFormat() = isNotBlank() && length <= 80 && none(Char::isWhitespace)

private fun DownloadExclusion.excludes(article: AssistantDownloadArticle) =
    kind == DownloadExclusionKind.EPISODE && value.equals(article.title, ignoreCase = true)

private fun DownloadExclusion.excludes(
    article: AssistantDownloadArticle,
    file: AssistantDownloadAttachment
): Boolean {
    if (episodeTitle != null && !episodeTitle.equals(article.title, ignoreCase = true)) return false
    return when (kind) {
        DownloadExclusionKind.EPISODE -> false
        DownloadExclusionKind.FORMAT -> file.matches(value)
        DownloadExclusionKind.FILENAME -> value.equals(
            downloadLinkLabel(file.url),
            ignoreCase = true
        )
    }
}

private fun AssistantDownloadArticle.options(
    format: String?,
    allAttachments: Boolean,
    exclusions: List<DownloadExclusion>,
    pickedUrls: List<String>,
): List<DownloadOption> {
    val files = attachments.distinctBy { it.url }.filter { file ->
        (format == null || file.matches(format)) && exclusions.none { it.excludes(this, file) }
    }

    fun option(label: String, files: List<AssistantDownloadAttachment>) = DownloadOption(
        id = "",
        title = label,
        targets = files.map {
            AssistantDownloadTarget.Attachment(
                articleId = articleId,
                url = it.url
            )
        },
    )
    if (pickedUrls.isNotEmpty()) {
        val selected = files.filter { it.url in pickedUrls }
        checkSelection(selected.size == pickedUrls.size)
        return listOf(option(selected.joinToString { it.label() }, selected))
    }
    if (allAttachments && files.isNotEmpty()) return listOf(option(title, files))
    return files.map { option(it.label(), listOf(it)) }
}

private fun checkSelection(valid: Boolean) {
    if (!valid) invalidSelection()
}

private fun invalidSelection(): Nothing = throw AssistantException(AssistantError.INVALID_OUTPUT)

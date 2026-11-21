package com.skyd.downloader

import androidx.room3.withWriteTransaction
import co.touchlab.kermit.Logger
import com.skyd.downloader.db.DownloadBatchReceiptDao
import com.skyd.downloader.db.DownloadBatchReceiptEntity
import com.skyd.downloader.db.DownloadDao
import com.skyd.downloader.db.DownloadDatabase
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.download.DownloadConstraints
import com.skyd.downloader.download.suffixedBatchFileName
import com.skyd.downloader.util.FileUtil
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.Uuid

data class DownloadBatchItem(
    val key: String,
    val url: String,
    val path: String,
    val fileName: String? = null,
    val metadata: String? = null,
    val constraints: DownloadConstraints = DownloadConstraints(),
    val requireFileResponse: Boolean = false,
)

@Serializable
data class DownloadBatchReceipt(
    val batchId: String,
    val submissionId: String,
    val queued: Map<String, String>,
    val existing: Map<String, String>,
    val failed: Set<String>,
    val matchedCount: Int,
    val remainingCount: Int,
    val limitExceeded: Boolean,
)

internal class DownloadBatchQueue(
    private val database: DownloadDatabase,
    private val schedule: suspend (DownloadEntity) -> Unit,
) {
    private val dao = database.downloadDao()
    private val receipts = database.downloadBatchReceiptDao()
    private val log = Logger.withTag("DownloadBatchQueue")

    suspend fun enqueue(
        batchId: String,
        submissionId: String,
        items: List<DownloadBatchItem>,
        failedKeys: Set<String>,
    ): DownloadBatchReceipt {
        currentCoroutineContext().ensureActive()
        return withContext(NonCancellable) {
            val submission = database.withWriteTransaction {
                dao.submitBatch(
                    receipts = receipts,
                    batchId = batchId,
                    submissionId = submissionId,
                    items = items,
                    failedKeys = failedKeys,
                    now = Clock.System.now().toEpochMilliseconds(),
                )
            }
            for (task in submission.queuedEntities) {
                try {
                    schedule(task)
                } catch (error: Exception) {
                    log.e(throwable = error) { "Could not schedule queued download ${task.id}" }
                }
            }
            submission.receipt
        }
    }

    suspend fun receipt(batchId: String, submissionId: String): DownloadBatchReceipt? =
        receipts.find(batchId, submissionId)?.decodeBatchReceipt()
}

private val DownloadTarget.destination get() = path to fileName
private val DownloadTarget.identity get() = Triple(url, path, fileName)

internal const val DOWNLOAD_BATCH_LIMIT = 128

@Serializable
private data class QueuedBatchTarget(val target: DownloadTarget, val id: String)

@Serializable
private data class StoredDownloadBatch(
    val receipt: DownloadBatchReceipt,
    val targets: List<QueuedBatchTarget>,
)

internal data class DownloadBatchSubmission(
    val receipt: DownloadBatchReceipt,
    val queuedEntities: List<DownloadEntity>,
)

internal fun DownloadBatchReceiptEntity.decodeBatchReceipt(): DownloadBatchReceipt =
    Json.decodeFromString<StoredDownloadBatch>(payload).receipt

// The caller holds the download database write transaction.
internal suspend fun DownloadDao.submitBatch(
    receipts: DownloadBatchReceiptDao,
    batchId: String,
    submissionId: String,
    items: List<DownloadBatchItem>,
    failedKeys: Set<String>,
    now: Long,
): DownloadBatchSubmission {
    receipts.find(batchId, submissionId)?.let {
        return DownloadBatchSubmission(
            receipt = it.decodeBatchReceipt(),
            queuedEntities = emptyList()
        )
    }

    val previous = receipts.findQueued(batchId)
    val remaining = DOWNLOAD_BATCH_LIMIT - previous.sumOf { it.queuedCount }
    val storedBatches = previous.map { Json.decodeFromString<StoredDownloadBatch>(it.payload) }
    val previousKeys = storedBatches.flatMap { it.receipt.queued.entries }.associate { it.toPair() }
    val previousTargets = storedBatches.flatMap { it.targets }.associateBy { it.target.identity }
    val seenTargets = mutableSetOf<Triple<String, String, String>>()
    val seenKeys = failedKeys.toMutableSet()
    val reservedNames = mutableSetOf<Pair<String, String>>()
    val newTasks = linkedMapOf<String, DownloadEntity>()
    val queuedTargets = mutableListOf<QueuedBatchTarget>()
    val existing = linkedMapOf<String, String>()
    val failed = failedKeys.toMutableSet()
    var newCount = 0

    for (item in items) {
        if (!seenKeys.add(item.key)) continue
        val target = try {
            DownloadTarget.resolve(url = item.url, path = item.path, fileName = item.fileName)
        } catch (_: IllegalArgumentException) {
            failed.add(item.key)
            continue
        }
        if (!seenTargets.add(target.identity)) continue

        val previouslyQueued = previousKeys[item.key] ?: previousTargets[target.identity]?.id
        val destinationOwner = findByDestination(target.path, target.fileName)
        val existingId =
            target.findSource(this)?.id ?: destinationOwner?.takeIf { it.url == target.url }?.id
            ?: previouslyQueued?.let { find(it)?.id }
        if (existingId != null) {
            existing[item.key] = existingId
            continue
        }
        if (previouslyQueued != null) continue

        val resolvedName = if (destinationOwner != null || target.destination in reservedNames ||
            FileUtil.finalFileExists(target.path, target.fileName)
        ) {
            if (target.requestedFileName.isNotEmpty()) {
                failed.add(item.key)
                continue
            }
            availableBatchFileName(target, reservedNames)
        } else {
            target.fileName
        }
        reservedNames.add(target.path to resolvedName)
        newCount++
        if (newCount > remaining) continue
        val task = target.copy(fileName = resolvedName).queuedEntity(
            now = now,
            attemptId = Uuid.random().toString(),
            metadata = item.metadata,
            constraints = item.constraints,
        ).copy(batchId = batchId, requireFileResponse = item.requireFileResponse)
        newTasks[item.key] = task
        queuedTargets += QueuedBatchTarget(target = target, id = task.id)
    }

    val limitExceeded = newCount > remaining
    val accepted = if (limitExceeded) emptyMap() else newTasks
    val entities = accepted.values.toList()
    val receipt = DownloadBatchReceipt(
        batchId = batchId,
        submissionId = submissionId,
        queued = accepted.mapValues { it.value.id },
        existing = existing,
        failed = failed,
        matchedCount = newCount + existing.size + failed.size,
        remainingCount = remaining - accepted.size,
        limitExceeded = limitExceeded,
    )
    if (entities.isNotEmpty()) insertAll(entities)
    receipts.insert(
        DownloadBatchReceiptEntity(
            batchId = batchId,
            submissionId = submissionId,
            queuedCount = entities.size,
            payload = Json.encodeToString(
                StoredDownloadBatch(
                    receipt = receipt,
                    targets = if (limitExceeded) emptyList() else queuedTargets,
                )
            ),
        )
    )
    return DownloadBatchSubmission(receipt = receipt, queuedEntities = entities)
}

private suspend fun DownloadDao.availableBatchFileName(
    target: DownloadTarget,
    reservedNames: Set<Pair<String, String>>,
): String {
    while (true) {
        val candidate = suffixedBatchFileName(target.fileName)
        if (target.path to candidate !in reservedNames &&
            findByDestination(target.path, candidate) == null &&
            !FileUtil.finalFileExists(target.path, candidate)
        ) {
            return candidate
        }
    }
}

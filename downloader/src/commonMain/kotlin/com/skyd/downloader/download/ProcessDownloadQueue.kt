package com.skyd.downloader.download

import com.skyd.downloader.ACTIVE_DOWNLOAD_STATUS_NAMES
import com.skyd.downloader.Status
import com.skyd.downloader.db.DownloadDao
import com.skyd.downloader.db.DownloadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

internal class ProcessDownloadQueue(
    private val downloadDao: DownloadDao,
    private val executor: DownloadExecutor,
) {
    private data class WorkItem(val id: String, val attemptId: String)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val channel = Channel<WorkItem>(Channel.BUFFERED)
    private val mutex = Mutex()
    private val scheduled = mutableSetOf<WorkItem>()
    private val activeJobs = mutableMapOf<String, Job>()

    init {
        repeat(MAX_CONCURRENT_DOWNLOADS) {
            scope.launch {
                for (item in channel) execute(item)
            }
        }
    }

    suspend fun schedule(entity: DownloadEntity) {
        val item = WorkItem(entity.id, entity.attemptId)
        if (mutex.withLock { scheduled.add(item) }) channel.send(item)
    }

    suspend fun cancel(id: String) {
        val job = mutex.withLock {
            scheduled.removeAll { it.id == id }
            activeJobs.remove(id)
        }
        job?.cancelAndJoin()
    }

    suspend fun reconcile() {
        val interrupted = downloadDao.findAllInStatuses(
            ACTIVE_DOWNLOAD_STATUS_NAMES
        )
        interrupted.forEach { entity ->
            downloadDao.update(
                entity.copy(
                    status = Status.Paused.name,
                    attemptId = "",
                    speedInBytePerMs = 0f,
                )
            )
        }
    }

    private suspend fun execute(item: WorkItem) {
        var result: DownloadExecutionResult? = null
        val job = scope.launch(start = CoroutineStart.LAZY) {
            result = executor.execute(item.id, item.attemptId)
        }
        val accepted = mutex.withLock {
            if (item !in scheduled) false else {
                activeJobs[item.id] = job
                true
            }
        }
        if (!accepted) {
            job.cancel()
            return
        }
        job.start()
        job.join()
        mutex.withLock {
            if (activeJobs[item.id] === job) activeJobs.remove(item.id)
        }

        when (val executionResult = result) {
            is DownloadExecutionResult.Retry -> scope.launch {
                delay(executionResult.delayMillis.milliseconds)
                if (mutex.withLock { item in scheduled }) channel.send(item)
            }

            else -> mutex.withLock { scheduled.remove(item) }
        }
    }

    companion object {
        private const val MAX_CONCURRENT_DOWNLOADS = 3
    }
}

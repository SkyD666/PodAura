package com.skyd.podaura.model.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class BatchProgress(
    val totalCount: Int,
    val successCount: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
) {
    val remainingCount: Int
        get() = (totalCount - successCount - skippedCount - failedCount).coerceAtLeast(0)
}

internal fun <T> processBatch(
    items: Collection<T>,
    action: suspend (T) -> Boolean,
): Flow<BatchProgress> = flow {
    var progress = BatchProgress(items.size)
    emit(progress)
    for (item in items) {
        currentCoroutineContext().ensureActive()
        progress = try {
            if (action(item)) progress.copy(successCount = progress.successCount + 1)
            else progress.copy(skippedCount = progress.skippedCount + 1)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            progress.copy(failedCount = progress.failedCount + 1)
        }
        emit(progress)
    }
}

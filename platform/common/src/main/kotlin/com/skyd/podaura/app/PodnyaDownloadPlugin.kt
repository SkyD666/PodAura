package com.skyd.podaura.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.skyd.podaura.api.model.repository.IAssistantDownloadRepository
import com.skyd.podaura.di.assistantDownloadHostModule
import com.skyd.podaura.podnya.capabilities.downloads.OpenDownloads
import com.skyd.podaura.podnya.capabilities.downloads.downloadModule
import com.skyd.podaura.ui.screen.download.DownloadBottomSheet
import com.skyd.podaura.ui.screen.download.DownloadListFocus
import kotlinx.coroutines.CancellationException
import org.koin.core.scope.Scope

internal val downloadPlugin = createDownloadPlugin { request, dismiss ->
    DownloadBottomSheet(
        loadFocus = {
            get<IAssistantDownloadRepository>().downloadListFocus(
                batchId = request.batchId,
                submissionIds = request.submissionIds,
            )
        },
        onDismissRequest = dismiss,
    )
}

internal fun createDownloadPlugin(
    sheet: @Composable Scope.(request: DownloadSheetRequest, onDismiss: () -> Unit) -> Unit,
) = PodnyaPlugin(
    id = "downloads",
    modules = arrayOf(downloadModule, assistantDownloadHostModule),
) { scope, _ ->
    var request by rememberSaveable(stateSaver = DownloadSheetRequest.Saver) {
        mutableStateOf(null)
    }
    request?.let { activeRequest ->
        scope.sheet(activeRequest) { request = null }
    }
    return@PodnyaPlugin { conversationId, action ->
        if (action is OpenDownloads) {
            request = DownloadSheetRequest(
                batchId = action.batchId(conversationId),
                submissionIds = action.submissionIds,
            )
        }
    }
}

internal data class DownloadSheetRequest(
    val batchId: String,
    val submissionIds: List<String>,
) {
    companion object {
        val Saver = listSaver<DownloadSheetRequest?, String>(
            save = { request -> request?.let { listOf(it.batchId) + it.submissionIds }.orEmpty() },
            restore = { DownloadSheetRequest(batchId = it.first(), submissionIds = it.drop(1)) },
        )
    }
}

internal suspend fun IAssistantDownloadRepository.downloadListFocus(
    batchId: String,
    submissionIds: List<String>,
): DownloadListFocus {
    val queued = mutableSetOf<String>()
    val existing = mutableSetOf<String>()
    for (id in submissionIds.distinct()) {
        val receipt = try {
            receipt(batchId = batchId, submissionId = id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } ?: continue
        queued += receipt.queued.values
        existing += receipt.existing.values
    }
    return DownloadListFocus(queued = queued, existing = existing)
}

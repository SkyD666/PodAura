package com.skyd.downloader.download

import com.skyd.downloader.db.DownloadDao
import com.skyd.downloader.db.DownloadEntity
import com.skyd.fundation.di.inject
import org.koin.core.component.KoinComponent

internal actual class DownloadManager actual constructor() : KoinComponent {
    private val downloadDao by inject<DownloadDao>()
    private val executor by inject<DownloadExecutor>()
    private val queue by lazy { ProcessDownloadQueue(downloadDao, executor) }

    actual suspend fun schedule(entity: DownloadEntity) = queue.schedule(entity)

    actual suspend fun cancel(id: String) = queue.cancel(id)

    actual suspend fun reconcile() = queue.reconcile()
}

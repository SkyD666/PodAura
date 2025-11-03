package com.skyd.podaura.model.repository.download

import com.skyd.podaura.model.download.DownloadInfoBean
import com.skyd.podaura.model.repository.BaseRepository
import kotlinx.coroutines.flow.Flow

class DownloadRepository : BaseRepository() {
    fun requestDownloadTasksList(): Flow<List<DownloadInfoBean>> {
        return DownloadManager.instance.downloadInfoListFlow
    }
}
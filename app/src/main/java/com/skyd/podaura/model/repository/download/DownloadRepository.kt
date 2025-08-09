package com.skyd.podaura.model.repository.download

import com.skyd.podaura.appContext
import com.skyd.podaura.model.bean.download.DownloadInfoBean
import com.skyd.podaura.model.repository.BaseRepository
import kotlinx.coroutines.flow.Flow

class DownloadRepository : BaseRepository() {
    fun requestDownloadTasksList(): Flow<List<DownloadInfoBean>> {
        return DownloadManager.getInstance(appContext).downloadInfoListFlow
    }
}
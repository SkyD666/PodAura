package com.skyd.downloader.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.skyd.downloader.Status
import com.skyd.downloader.UserAction

@Entity(tableName = DownloadEntity.TABLE_NAME)
data class DownloadEntity(
    @PrimaryKey
    var id: Int = 0,
    var url: String = "",
    var path: String = "",
    var fileName: String = "",
    var timeQueued: Long = 0,
    var status: String = Status.Init.toString(),
    var totalBytes: Long = 0,
    var downloadedBytes: Long = 0,
    var speedInBytePerMs: Float = 0f,
    var eTag: String = "",
    var workerUuid: String = "",
    var createTime: Long = 0,
    var userAction: String = UserAction.Init.toString(),
    var failureReason: String = ""
) {
    companion object {
        const val TABLE_NAME = "Download"
    }
}

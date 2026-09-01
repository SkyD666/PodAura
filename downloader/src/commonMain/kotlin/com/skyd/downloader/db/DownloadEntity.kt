package com.skyd.downloader.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.skyd.downloader.Status

@Entity(
    tableName = DownloadEntity.TABLE_NAME,
    indices = [
        Index(
            value = [
                DownloadEntity.URL_COLUMN,
                DownloadEntity.PATH_COLUMN,
                DownloadEntity.REQUESTED_FILE_NAME_COLUMN,
            ]
        ),
        Index(value = [DownloadEntity.PATH_COLUMN, DownloadEntity.FILE_NAME_COLUMN]),
        Index(value = [DownloadEntity.STATUS_COLUMN]),
    ],
)
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    var url: String = "",
    var path: String = "",
    var fileName: String = "",
    var requestedFileName: String = "",
    var fileNameResolved: Boolean = true,
    var timeQueued: Long = 0,
    var status: String = Status.Init.toString(),
    var totalBytes: Long = 0,
    var downloadedBytes: Long = 0,
    var speedInBytePerMs: Float = 0f,
    var eTag: String = "",
    var lastModified: String = "",
    var finalUrl: String = "",
    var attemptId: String = "",
    var autoRetryCount: Int = 0,
    var nextAttemptAt: Long = 0,
    var createTime: Long = 0,
    var updatedTime: Long = 0,
    var failureReason: String = "",
    var failureCode: String = "",
    var metadata: String? = null,
    var completionHandled: Boolean = false,
    var requireUnmetered: Boolean = false,
    var requiresCharging: Boolean = false,
    var requiresBatteryNotLow: Boolean = false,
) {
    companion object {
        const val TABLE_NAME = "Download"
        const val ID_COLUMN = "id"
        const val URL_COLUMN = "url"
        const val PATH_COLUMN = "path"
        const val FILE_NAME_COLUMN = "fileName"
        const val REQUESTED_FILE_NAME_COLUMN = "requestedFileName"
        const val FILE_NAME_RESOLVED_COLUMN = "fileNameResolved"
        const val TIME_QUEUED_COLUMN = "timeQueued"
        const val STATUS_COLUMN = "status"
        const val TOTAL_BYTES_COLUMN = "totalBytes"
        const val DOWNLOADED_BYTES_COLUMN = "downloadedBytes"
        const val SPEED_IN_BYTE_PER_MS_COLUMN = "speedInBytePerMs"
        const val E_TAG_COLUMN = "eTag"
        const val LAST_MODIFIED_COLUMN = "lastModified"
        const val FINAL_URL_COLUMN = "finalUrl"
        const val ATTEMPT_ID_COLUMN = "attemptId"
        const val AUTO_RETRY_COUNT_COLUMN = "autoRetryCount"
        const val NEXT_ATTEMPT_AT_COLUMN = "nextAttemptAt"
        const val CREATE_TIME_COLUMN = "createTime"
        const val UPDATED_TIME_COLUMN = "updatedTime"
        const val FAILURE_REASON_COLUMN = "failureReason"
        const val FAILURE_CODE_COLUMN = "failureCode"
        const val METADATA_COLUMN = "metadata"
        const val COMPLETION_HANDLED_COLUMN = "completionHandled"
        const val REQUIRE_UNMETERED_COLUMN = "requireUnmetered"
        const val REQUIRES_CHARGING_COLUMN = "requiresCharging"
        const val REQUIRES_BATTERY_NOT_LOW_COLUMN = "requiresBatteryNotLow"
    }
}

package com.skyd.podaura.model.bean.download.autorule

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.skyd.podaura.model.bean.BaseBean
import com.skyd.podaura.model.bean.feed.FeedBean
import kotlinx.serialization.Serializable

const val AUTO_DOWNLOAD_RULE_TABLE_NAME = "AutoDownloadRule"

@Serializable
@Entity(
    tableName = AUTO_DOWNLOAD_RULE_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = FeedBean::class,
            parentColumns = [FeedBean.URL_COLUMN],
            childColumns = [AutoDownloadRuleBean.FEED_URL_COLUMN],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(AutoDownloadRuleBean.FEED_URL_COLUMN)]
)
data class AutoDownloadRuleBean(
    @PrimaryKey
    @ColumnInfo(name = FEED_URL_COLUMN)
    val feedUrl: String,
    @ColumnInfo(name = REQUIRE_WIFI_COLUMN)
    val requireWifi: Boolean,
    @ColumnInfo(name = REQUIRE_BATTERY_NOT_LOW_COLUMN)
    val requireBatteryNotLow: Boolean,
    @ColumnInfo(name = REQUIRE_CHARGING_COLUMN)
    val requireCharging: Boolean,
    @ColumnInfo(name = ENABLED_COLUMN)
    val enabled: Boolean,
    @ColumnInfo(name = MAX_DOWNLOAD_COUNT_COLUMN)
    val maxDownloadCount: Int,
    @ColumnInfo(name = FILTER_PATTERN_COLUMN)
    var filterPattern: String?,
) : BaseBean {
    companion object {
        const val FEED_URL_COLUMN = "feedUrl"
        const val ENABLED_COLUMN = "enabled"
        const val REQUIRE_WIFI_COLUMN = "requireWifi"
        const val REQUIRE_BATTERY_NOT_LOW_COLUMN = "requireBatteryNotLow"
        const val REQUIRE_CHARGING_COLUMN = "requireCharging"
        const val MAX_DOWNLOAD_COUNT_COLUMN = "maxDownloadCount"
        const val FILTER_PATTERN_COLUMN = "filterPattern"
    }
}
package com.skyd.podaura.model.db.dao.download

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.skyd.podaura.model.bean.download.autorule.AUTO_DOWNLOAD_RULE_TABLE_NAME
import com.skyd.podaura.model.bean.download.autorule.AutoDownloadRuleBean
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoDownloadRuleDao {
    @Transaction
    @Upsert
    suspend fun upsert(rule: AutoDownloadRuleBean)

    @Transaction
    @Query(
        "DELETE FROM $AUTO_DOWNLOAD_RULE_TABLE_NAME " +
                "WHERE ${AutoDownloadRuleBean.FEED_URL_COLUMN} = :feedUrl"
    )
    suspend fun deleteByFeedUrl(feedUrl: String): Int

    @Transaction
    @Query("SELECT * FROM $AUTO_DOWNLOAD_RULE_TABLE_NAME")
    fun getAllRules(): PagingSource<Int, AutoDownloadRuleBean>

    @Transaction
    @Query(
        "SELECT * FROM $AUTO_DOWNLOAD_RULE_TABLE_NAME " +
                "WHERE ${AutoDownloadRuleBean.FEED_URL_COLUMN} = :feedUrl"
    )
    fun containsByFeedUrl(feedUrl: String): Flow<AutoDownloadRuleBean?>

    @Transaction
    @Query(
        "SELECT * FROM $AUTO_DOWNLOAD_RULE_TABLE_NAME " +
                "WHERE ${AutoDownloadRuleBean.FEED_URL_COLUMN} = :feedUrl"
    )
    fun getRuleByFeedUrl(feedUrl: String): Flow<AutoDownloadRuleBean?>

    @Transaction
    @Query(
        "UPDATE $AUTO_DOWNLOAD_RULE_TABLE_NAME " +
                "SET ${AutoDownloadRuleBean.ENABLED_COLUMN} = :enabled " +
                "WHERE ${AutoDownloadRuleBean.FEED_URL_COLUMN} = :feedUrl"
    )
    suspend fun updateEnabled(feedUrl: String, enabled: Boolean): Int

    @Transaction
    @Query(
        "UPDATE $AUTO_DOWNLOAD_RULE_TABLE_NAME " +
                "SET ${AutoDownloadRuleBean.REQUIRE_WIFI_COLUMN} = :requireWifi " +
                "WHERE ${AutoDownloadRuleBean.FEED_URL_COLUMN} = :feedUrl"
    )
    suspend fun updateRequireWifi(feedUrl: String, requireWifi: Boolean): Int

    @Transaction
    @Query(
        "UPDATE $AUTO_DOWNLOAD_RULE_TABLE_NAME " +
                "SET ${AutoDownloadRuleBean.REQUIRE_BATTERY_NOT_LOW_COLUMN} = :requireBatteryNotLow " +
                "WHERE ${AutoDownloadRuleBean.FEED_URL_COLUMN} = :feedUrl"
    )
    suspend fun updateRequireBatteryNotLow(feedUrl: String, requireBatteryNotLow: Boolean): Int

    @Transaction
    @Query(
        "UPDATE $AUTO_DOWNLOAD_RULE_TABLE_NAME " +
                "SET ${AutoDownloadRuleBean.REQUIRE_CHARGING_COLUMN} = :requireCharging " +
                "WHERE ${AutoDownloadRuleBean.FEED_URL_COLUMN} = :feedUrl"
    )
    suspend fun updateRequireCharging(feedUrl: String, requireCharging: Boolean): Int

    @Transaction
    @Query(
        "UPDATE $AUTO_DOWNLOAD_RULE_TABLE_NAME " +
                "SET ${AutoDownloadRuleBean.MAX_DOWNLOAD_COUNT_COLUMN} = :maxDownloadCount " +
                "WHERE ${AutoDownloadRuleBean.FEED_URL_COLUMN} = :feedUrl"
    )
    suspend fun updateMaxDownloadCount(feedUrl: String, maxDownloadCount: Int): Int

    @Transaction
    @Query(
        "UPDATE $AUTO_DOWNLOAD_RULE_TABLE_NAME " +
                "SET ${AutoDownloadRuleBean.FILTER_PATTERN_COLUMN} = :filterPattern " +
                "WHERE ${AutoDownloadRuleBean.FEED_URL_COLUMN} = :feedUrl"
    )
    suspend fun updateFilterPattern(feedUrl: String, filterPattern: String?): Int
}
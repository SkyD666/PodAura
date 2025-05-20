package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.download.autorule.AUTO_DOWNLOAD_RULE_TABLE_NAME
import com.skyd.podaura.model.bean.download.autorule.AutoDownloadRuleBean
import com.skyd.podaura.model.bean.feed.FEED_TABLE_NAME
import com.skyd.podaura.model.bean.feed.FeedBean

class Migration24To25 : Migration(24, 25) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE `${AUTO_DOWNLOAD_RULE_TABLE_NAME}` (" +
                    "${AutoDownloadRuleBean.FEED_URL_COLUMN} TEXT PRIMARY KEY NOT NULL, " +
                    "${AutoDownloadRuleBean.REQUIRE_WIFI_COLUMN} INTEGER NOT NULL, " +
                    "${AutoDownloadRuleBean.REQUIRE_BATTERY_NOT_LOW_COLUMN} INTEGER NOT NULL, " +
                    "${AutoDownloadRuleBean.REQUIRE_CHARGING_COLUMN} INTEGER NOT NULL, " +
                    "${AutoDownloadRuleBean.ENABLED_COLUMN} INTEGER NOT NULL, " +
                    "${AutoDownloadRuleBean.MAX_DOWNLOAD_COUNT_COLUMN} INTEGER NOT NULL, " +
                    "${AutoDownloadRuleBean.FILTER_PATTERN_COLUMN} TEXT, " +
                    "FOREIGN KEY (${AutoDownloadRuleBean.FEED_URL_COLUMN}) " +
                    "REFERENCES $FEED_TABLE_NAME(${FeedBean.URL_COLUMN}) " +
                    "ON DELETE CASCADE" +
                    ")"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS " +
                    "`index_${AUTO_DOWNLOAD_RULE_TABLE_NAME}_${AutoDownloadRuleBean.FEED_URL_COLUMN}` " +
                    "ON `${AUTO_DOWNLOAD_RULE_TABLE_NAME}` (`${AutoDownloadRuleBean.FEED_URL_COLUMN}`)"
        )
    }
}
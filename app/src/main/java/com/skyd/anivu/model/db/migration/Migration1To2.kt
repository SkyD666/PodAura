package com.skyd.anivu.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skyd.anivu.model.bean.download.bt.DOWNLOAD_LINK_UUID_MAP_TABLE_NAME
import com.skyd.anivu.model.bean.download.bt.DownloadLinkUuidMapBean

class Migration1To2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                CREATE TABLE $DOWNLOAD_LINK_UUID_MAP_TABLE_NAME (
                    ${DownloadLinkUuidMapBean.LINK_COLUMN} TEXT NOT NULL,
                    ${DownloadLinkUuidMapBean.UUID_COLUMN} TEXT NOT NULL,
                    PRIMARY KEY (${DownloadLinkUuidMapBean.LINK_COLUMN}, ${DownloadLinkUuidMapBean.UUID_COLUMN})
                )
                """
        )
    }
}
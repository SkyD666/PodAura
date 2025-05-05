package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.download.bt.BtDownloadInfoBean.Companion.DESCRIPTION_COLUMN
import com.skyd.podaura.model.bean.download.bt.BtDownloadInfoBean.Companion.DOWNLOAD_DATE_COLUMN
import com.skyd.podaura.model.bean.download.bt.BtDownloadInfoBean.Companion.DOWNLOAD_REQUEST_ID_COLUMN
import com.skyd.podaura.model.bean.download.bt.BtDownloadInfoBean.Companion.DOWNLOAD_STATE_COLUMN
import com.skyd.podaura.model.bean.download.bt.BtDownloadInfoBean.Companion.LINK_COLUMN
import com.skyd.podaura.model.bean.download.bt.BtDownloadInfoBean.Companion.NAME_COLUMN
import com.skyd.podaura.model.bean.download.bt.BtDownloadInfoBean.Companion.PROGRESS_COLUMN
import com.skyd.podaura.model.bean.download.bt.BtDownloadInfoBean.Companion.SIZE_COLUMN
import com.skyd.podaura.model.bean.download.bt.DOWNLOAD_INFO_TABLE_NAME

class Migration8To9 : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE ${DOWNLOAD_INFO_TABLE_NAME}_Backup (" +
                    "$LINK_COLUMN TEXT PRIMARY KEY NOT NULL, " +
                    "$NAME_COLUMN TEXT NOT NULL, " +
                    "$DOWNLOAD_DATE_COLUMN INTEGER NOT NULL, " +
                    "$SIZE_COLUMN INTEGER NOT NULL, " +
                    "$PROGRESS_COLUMN REAL NOT NULL, " +
                    "$DESCRIPTION_COLUMN TEXT, " +
                    "$DOWNLOAD_STATE_COLUMN TEXT NOT NULL, " +
                    "$DOWNLOAD_REQUEST_ID_COLUMN TEXT NOT NULL" +
                    ")"
        )
        connection.execSQL(
            "INSERT INTO ${DOWNLOAD_INFO_TABLE_NAME}_Backup SELECT " +
                    "$LINK_COLUMN, $NAME_COLUMN, $DOWNLOAD_DATE_COLUMN, $SIZE_COLUMN, $PROGRESS_COLUMN, " +
                    "$DESCRIPTION_COLUMN, $DOWNLOAD_STATE_COLUMN, $DOWNLOAD_REQUEST_ID_COLUMN" +
                    " FROM $DOWNLOAD_INFO_TABLE_NAME"
        )
        connection.execSQL("DROP TABLE $DOWNLOAD_INFO_TABLE_NAME")
        connection.execSQL("ALTER TABLE ${DOWNLOAD_INFO_TABLE_NAME}_Backup RENAME to $DOWNLOAD_INFO_TABLE_NAME")
        connection.execSQL("CREATE UNIQUE INDEX index_DownloadInfo_link ON $DOWNLOAD_INFO_TABLE_NAME ($LINK_COLUMN)")
    }
}
package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.bean.download.bt.BtDownloadInfoBean
import com.skyd.podaura.model.bean.download.bt.DOWNLOAD_INFO_TABLE_NAME
import com.skyd.podaura.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.podaura.model.preference.dataStore

class Migration18To19 : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val defaultPath = dataStore.getOrDefault(MediaLibLocationPreference)
        db.execSQL("ALTER TABLE `$DOWNLOAD_INFO_TABLE_NAME` ADD ${BtDownloadInfoBean.PATH_COLUMN} TEXT NOT NULL DEFAULT \"$defaultPath\"")
    }
}
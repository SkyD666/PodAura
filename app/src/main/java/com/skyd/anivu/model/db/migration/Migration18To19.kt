package com.skyd.anivu.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skyd.anivu.appContext
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.model.bean.download.bt.BtDownloadInfoBean
import com.skyd.anivu.model.bean.download.bt.DOWNLOAD_INFO_TABLE_NAME
import com.skyd.anivu.model.preference.data.medialib.MediaLibLocationPreference

class Migration18To19 : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val defaultPath = appContext.dataStore.getOrDefault(MediaLibLocationPreference)
        db.execSQL("ALTER TABLE `$DOWNLOAD_INFO_TABLE_NAME` ADD ${BtDownloadInfoBean.PATH_COLUMN} TEXT NOT NULL DEFAULT \"$defaultPath\"")
    }
}
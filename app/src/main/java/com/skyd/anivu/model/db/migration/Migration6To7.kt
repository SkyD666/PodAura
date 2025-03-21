package com.skyd.anivu.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skyd.anivu.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.anivu.model.bean.article.ArticleBean

class Migration6To7 : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE $ARTICLE_TABLE_NAME ADD ${ArticleBean.GUID_COLUMN} TEXT")
    }
}
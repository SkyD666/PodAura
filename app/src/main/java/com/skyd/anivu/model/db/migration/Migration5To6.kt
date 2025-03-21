package com.skyd.anivu.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skyd.anivu.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.anivu.model.bean.article.ArticleBean
import com.skyd.anivu.model.bean.feed.FEED_TABLE_NAME
import com.skyd.anivu.model.bean.feed.FeedBean

class Migration5To6 : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE $FEED_TABLE_NAME ADD ${FeedBean.CUSTOM_DESCRIPTION_COLUMN} TEXT")
        db.execSQL("ALTER TABLE $FEED_TABLE_NAME ADD ${FeedBean.CUSTOM_ICON_COLUMN} TEXT")

        db.execSQL("ALTER TABLE $ARTICLE_TABLE_NAME ADD ${ArticleBean.IS_READ_COLUMN} INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE $ARTICLE_TABLE_NAME ADD ${ArticleBean.IS_FAVORITE_COLUMN} INTEGER NOT NULL DEFAULT 0")
    }
}
package com.skyd.anivu.model.db.migration

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getStringOrNull
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skyd.anivu.model.bean.article.ARTICLE_CATEGORY_TABLE_NAME
import com.skyd.anivu.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.anivu.model.bean.article.ArticleBean
import com.skyd.anivu.model.bean.article.ArticleCategoryBean
import com.skyd.anivu.model.bean.feed.FEED_TABLE_NAME
import com.skyd.anivu.model.bean.feed.FeedBean
import kotlinx.serialization.json.Json

class Migration20To21 : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create backup table
        db.execSQL("PRAGMA foreign_keys = OFF")
        db.execSQL(
            "CREATE TABLE ${ARTICLE_TABLE_NAME}_Backup (" +
                    "${ArticleBean.ARTICLE_ID_COLUMN} TEXT PRIMARY KEY NOT NULL, " +
                    "${ArticleBean.FEED_URL_COLUMN} TEXT NOT NULL, " +
                    "${ArticleBean.TITLE_COLUMN} TEXT, " +
                    "${ArticleBean.DATE_COLUMN} INTEGER, " +
                    "${ArticleBean.AUTHOR_COLUMN} TEXT, " +
                    "${ArticleBean.DESCRIPTION_COLUMN} TEXT, " +
                    "${ArticleBean.CONTENT_COLUMN} TEXT, " +
                    "${ArticleBean.IMAGE_COLUMN} TEXT, " +
                    "${ArticleBean.LINK_COLUMN} TEXT, " +
                    "${ArticleBean.GUID_COLUMN} TEXT, " +
                    "${ArticleBean.UPDATE_AT_COLUMN} INTEGER, " +
                    "${ArticleBean.IS_READ_COLUMN} INTEGER NOT NULL, " +
                    "${ArticleBean.IS_FAVORITE_COLUMN} INTEGER NOT NULL, " +
                    "FOREIGN KEY (${ArticleBean.FEED_URL_COLUMN}) " +
                    "REFERENCES $FEED_TABLE_NAME(${FeedBean.URL_COLUMN}) " +
                    "ON DELETE CASCADE" +
                    ")"
        )
        db.execSQL(
            "INSERT INTO ${ARTICLE_TABLE_NAME}_Backup SELECT " +
                    "${ArticleBean.ARTICLE_ID_COLUMN}, ${ArticleBean.FEED_URL_COLUMN}, " +
                    "${ArticleBean.TITLE_COLUMN}, ${ArticleBean.DATE_COLUMN}, " +
                    "${ArticleBean.AUTHOR_COLUMN}, ${ArticleBean.DESCRIPTION_COLUMN}, " +
                    "${ArticleBean.CONTENT_COLUMN}, ${ArticleBean.IMAGE_COLUMN}, " +
                    "${ArticleBean.LINK_COLUMN}, ${ArticleBean.GUID_COLUMN}, " +
                    "${ArticleBean.UPDATE_AT_COLUMN}, ${ArticleBean.IS_READ_COLUMN}, " +
                    "${ArticleBean.IS_FAVORITE_COLUMN} " +
                    "FROM $ARTICLE_TABLE_NAME"
        )

        // Create category table
        db.execSQL(
            "CREATE TABLE `$ARTICLE_CATEGORY_TABLE_NAME` (" +
                    "${ArticleCategoryBean.ARTICLE_ID_COLUMN} TEXT NOT NULL, " +
                    "${ArticleCategoryBean.CATEGORY_COLUMN} TEXT NOT NULL, " +
                    "PRIMARY KEY (${ArticleCategoryBean.ARTICLE_ID_COLUMN}, ${ArticleCategoryBean.CATEGORY_COLUMN}) " +
                    "FOREIGN KEY (${ArticleCategoryBean.ARTICLE_ID_COLUMN}) " +
                    "REFERENCES $ARTICLE_TABLE_NAME(${ArticleBean.ARTICLE_ID_COLUMN}) " +
                    "ON DELETE CASCADE" +
                    ")"
        )

        val cursor = db.query(
            "SELECT ${ArticleBean.ARTICLE_ID_COLUMN}, catrgories FROM $ARTICLE_TABLE_NAME"
        )
        while (cursor.moveToNext()) {
            val categories = cursor.getStringOrNull(1)?.let {
                Json.decodeFromString<List<String>>(it)
            }?.distinct()
            if (categories.isNullOrEmpty()) continue
            val articleId = cursor.getString(0)
            categories.forEach { category ->
                db.insert(
                    ARTICLE_CATEGORY_TABLE_NAME,
                    SQLiteDatabase.CONFLICT_REPLACE,
                    ContentValues().apply {
                        put(ArticleCategoryBean.ARTICLE_ID_COLUMN, articleId)
                        put(ArticleCategoryBean.CATEGORY_COLUMN, category)
                    }
                )
            }
        }

        // Drop old table
        db.execSQL("DROP TABLE $ARTICLE_TABLE_NAME")
        db.execSQL("ALTER TABLE ${ARTICLE_TABLE_NAME}_Backup RENAME to $ARTICLE_TABLE_NAME")
        db.execSQL("CREATE INDEX index_Article_articleId ON $ARTICLE_TABLE_NAME (${ArticleBean.ARTICLE_ID_COLUMN})")
        db.execSQL("CREATE INDEX index_Article_feedUrl ON $ARTICLE_TABLE_NAME (${ArticleBean.FEED_URL_COLUMN})")
        db.execSQL("PRAGMA foreign_keys = ON")
    }
}
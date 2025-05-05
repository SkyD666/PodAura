package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.article.ARTICLE_CATEGORY_TABLE_NAME
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleCategoryBean
import com.skyd.podaura.model.bean.feed.FEED_TABLE_NAME
import com.skyd.podaura.model.bean.feed.FeedBean
import kotlinx.serialization.json.Json

class Migration20To21 : Migration(20, 21) {
    override fun migrate(connection: SQLiteConnection) {
        // Create backup table
        connection.execSQL("PRAGMA foreign_keys = OFF")
        connection.execSQL(
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
        connection.execSQL(
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
        connection.execSQL(
            "CREATE TABLE `$ARTICLE_CATEGORY_TABLE_NAME` (" +
                    "${ArticleCategoryBean.ARTICLE_ID_COLUMN} TEXT NOT NULL, " +
                    "${ArticleCategoryBean.CATEGORY_COLUMN} TEXT NOT NULL, " +
                    "PRIMARY KEY (${ArticleCategoryBean.ARTICLE_ID_COLUMN}, ${ArticleCategoryBean.CATEGORY_COLUMN}) " +
                    "FOREIGN KEY (${ArticleCategoryBean.ARTICLE_ID_COLUMN}) " +
                    "REFERENCES $ARTICLE_TABLE_NAME(${ArticleBean.ARTICLE_ID_COLUMN}) " +
                    "ON DELETE CASCADE" +
                    ")"
        )

        connection.prepare(
            "SELECT ${ArticleBean.ARTICLE_ID_COLUMN}, catrgories FROM $ARTICLE_TABLE_NAME"
        ).use { statement ->
            while (statement.step()) {
                val categories = statement.getText(1).let {
                    Json.decodeFromString<List<String>>(it)
                }.distinct()
                if (categories.isEmpty()) continue
                val articleId = statement.getText(0)
                categories.forEach { category ->
                    connection.prepare(
                        "INSERT OR REPLACE INTO $ARTICLE_CATEGORY_TABLE_NAME " +
                                "(${ArticleCategoryBean.ARTICLE_ID_COLUMN}, " +
                                "${ArticleCategoryBean.CATEGORY_COLUMN}) VALUES (?, ?)"
                    ).use { insertStmt ->
                        insertStmt.bindText(1, articleId)
                        insertStmt.bindText(2, category)
                        insertStmt.step()
                    }
                }
            }
        }

        // Drop old table
        connection.execSQL("DROP TABLE $ARTICLE_TABLE_NAME")
        connection.execSQL("ALTER TABLE ${ARTICLE_TABLE_NAME}_Backup RENAME to $ARTICLE_TABLE_NAME")
        connection.execSQL("CREATE INDEX index_Article_articleId ON $ARTICLE_TABLE_NAME (${ArticleBean.ARTICLE_ID_COLUMN})")
        connection.execSQL("CREATE INDEX index_Article_feedUrl ON $ARTICLE_TABLE_NAME (${ArticleBean.FEED_URL_COLUMN})")
        connection.execSQL("PRAGMA foreign_keys = ON")
    }
}
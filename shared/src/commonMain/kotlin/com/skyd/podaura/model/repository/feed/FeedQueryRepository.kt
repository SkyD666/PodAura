package com.skyd.podaura.model.repository.feed

import androidx.room3.RoomRawQuery
import com.skyd.podaura.api.model.bean.FeedQuery
import com.skyd.podaura.api.model.bean.FeedRecord
import com.skyd.podaura.api.model.bean.KeywordMatch
import com.skyd.podaura.api.model.repository.IFeedRepository
import com.skyd.podaura.model.bean.feed.FEED_TABLE_NAME
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.db.dao.FeedDao

/** Implements the public read-only query using bound SQL values. */
class FeedQueryRepository(private val feedDao: FeedDao) : IFeedRepository {
    override suspend fun query(query: FeedQuery): List<FeedRecord> {
        require(query.limit > 0)
        val arguments = mutableListOf<Any>()
        val filters = mutableListOf("1")
        query.withinFeedUrls?.let { urls ->
            filters += if (urls.isEmpty()) "0" else {
                arguments.addAll(urls)
                "`${FeedBean.URL_COLUMN}` IN (${urls.joinToString { "?" }})"
            }
        }
        query.muted?.let {
            filters += "`${FeedBean.MUTE_COLUMN}` = ?"
            arguments += if (it) 1L else 0L
        }
        if (query.keywords.isNotEmpty()) {
            val columns = listOf(
                FeedBean.TITLE_COLUMN, FeedBean.NICKNAME_COLUMN,
                FeedBean.DESCRIPTION_COLUMN, FeedBean.CUSTOM_DESCRIPTION_COLUMN
            )
            val join = if (query.match == KeywordMatch.ALL) " AND " else " OR "
            filters += query.keywords.joinToString(join, "(", ")") { keyword ->
                val literal =
                    "%${keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_")}%"
                columns.joinToString(" OR ", "(", ")") { column ->
                    arguments += literal
                    "`$column` LIKE ? ESCAPE '!'"
                }
            }
        }
        arguments += query.limit.toLong()
        require(arguments.size <= 999)
        val sql = RoomRawQuery(
            "SELECT * FROM `$FEED_TABLE_NAME` WHERE ${filters.joinToString(" AND ")} " +
                    "ORDER BY COALESCE(`${FeedBean.NICKNAME_COLUMN}`, `${FeedBean.TITLE_COLUMN}`, ''), " +
                    "`${FeedBean.URL_COLUMN}` LIMIT ?"
        ) { statement ->
            arguments.forEachIndexed { index, value ->
                when (value) {
                    is String -> statement.bindText(index + 1, value)
                    is Long -> statement.bindLong(index + 1, value)
                }
            }
        }
        return feedDao.queryFeeds(sql).map {
            FeedRecord(it.url, it.title, it.nickname, it.description, it.customDescription, it.mute)
        }
    }
}

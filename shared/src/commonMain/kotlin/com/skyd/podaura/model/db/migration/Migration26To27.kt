package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.feed.FEED_TABLE_NAME
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.group.GROUP_TABLE_NAME
import com.skyd.podaura.model.bean.group.GroupBean
import com.skyd.podaura.model.db.dao.GroupDao.Companion.ORDER_DELTA

class Migration26To27 : Migration(26, 27) {
    private fun SQLiteConnection.reorderFeedsInGroup(groupId: String?) {
        val statement = if (groupId == null) {
            prepare(
                "SELECT ${FeedBean.URL_COLUMN} FROM `$FEED_TABLE_NAME` " +
                        "WHERE ${FeedBean.GROUP_ID_COLUMN} IS NULL " +
                        "ORDER BY ${FeedBean.TITLE_COLUMN}"
            )
        } else {
            prepare(
                "SELECT ${FeedBean.URL_COLUMN} FROM `$FEED_TABLE_NAME` " +
                        "WHERE ${FeedBean.GROUP_ID_COLUMN} = ? " +
                        "ORDER BY ${FeedBean.TITLE_COLUMN}"
            ).also { it.bindText(1, groupId) }
        }

        statement.use { statement ->
            var index = 0
            while (statement.step()) {
                prepare(
                    "UPDATE `$FEED_TABLE_NAME` " +
                            "SET ${FeedBean.ORDER_POSITION_COLUMN} = ? " +
                            "WHERE ${FeedBean.URL_COLUMN} = ?"
                ).use { updateStmt ->
                    updateStmt.bindDouble(1, (index * ORDER_DELTA) + ORDER_DELTA)
                    updateStmt.bindText(2, statement.getText(0))
                    updateStmt.step()
                }
                index++
            }
        }
    }

    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `$FEED_TABLE_NAME` " +
                    "ADD ${FeedBean.ORDER_POSITION_COLUMN} REAL NOT NULL DEFAULT 0"
        )
        connection.execSQL(
            "ALTER TABLE `$FEED_TABLE_NAME` " +
                    "ADD ${FeedBean.FILTER_MASK_COLUMN} INTEGER NOT NULL DEFAULT 0"
        )
        connection.reorderFeedsInGroup(groupId = null)
        connection.prepare(
            "SELECT ${GroupBean.GROUP_ID_COLUMN} FROM `$GROUP_TABLE_NAME`"
        ).use { statement ->
            while (statement.step()) {
                connection.reorderFeedsInGroup(groupId = statement.getText(0))
            }
        }
    }
}
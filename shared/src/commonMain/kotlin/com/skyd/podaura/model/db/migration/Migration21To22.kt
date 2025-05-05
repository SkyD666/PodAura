package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.ext.calculateHashMapInitialCapacity
import com.skyd.podaura.model.bean.feed.FEED_TABLE_NAME
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.group.GROUP_TABLE_NAME
import com.skyd.podaura.model.bean.group.GroupBean
import com.skyd.podaura.model.db.dao.GroupDao.Companion.ORDER_DELTA

class Migration21To22 : Migration(21, 22) {
    private fun sortGroups(groupList: List<Map<String, String?>>): List<Map<String, String?>> {
        val groupsMap = groupList.associateBy { it["groupId"] }
        val hasPreviousGroups = LinkedHashSet<Map<String, String?>>(
            calculateHashMapInitialCapacity(groupList.size)
        )
        groupList.forEach { group ->
            val nextGroupId = group["nextGroupId"]
            if (nextGroupId != null) {
                hasPreviousGroups.add(groupsMap[nextGroupId]!!)
            }
        }
        val noPreviousGroups = (groupList - hasPreviousGroups).sortedBy { it["name"] }
        val sortedList = LinkedHashSet<Map<String, String?>>(
            calculateHashMapInitialCapacity(groupList.size)
        )
        noPreviousGroups.forEach { group ->
            var currentGroup: Map<String, String?>? = group
            var currentGroupId: String? = group["groupId"]
            while (currentGroupId != null) {
                sortedList.add(currentGroup!!)
                if (currentGroupId == currentGroup["nextGroupId"]) break
                currentGroupId = currentGroup["nextGroupId"]
                currentGroup = if (currentGroupId != null) groupsMap[currentGroupId] else null
            }
        }
        val result = sortedList.toList()
        return result
    }

    override fun migrate(connection: SQLiteConnection) {
        // Create backup table
        connection.execSQL("PRAGMA foreign_keys = OFF")
        connection.execSQL(
            "CREATE TABLE ${GROUP_TABLE_NAME}_Backup (" +
                    "${GroupBean.GROUP_ID_COLUMN} TEXT PRIMARY KEY NOT NULL, " +
                    "${GroupBean.NAME_COLUMN} TEXT NOT NULL, " +
                    "${GroupBean.IS_EXPANDED_COLUMN} INTEGER NOT NULL, " +
                    "${GroupBean.ORDER_POSITION_COLUMN} REAL NOT NULL" +
                    ")"
        )

        connection.prepare(
            "SELECT ${GroupBean.GROUP_ID_COLUMN}, ${GroupBean.NAME_COLUMN}, " +
                    "${GroupBean.IS_EXPANDED_COLUMN}, previousGroupId, nextGroupId FROM `$GROUP_TABLE_NAME`"
        ).use { statement ->
            val oldGroups = mutableListOf<Map<String, String?>>()
            while (statement.step()) {
                oldGroups.add(
                    buildMap {
                        put(GroupBean.GROUP_ID_COLUMN, statement.getText(0))
                        put(GroupBean.NAME_COLUMN, statement.getText(1))
                        put(GroupBean.IS_EXPANDED_COLUMN, statement.getInt(2).toString())
                        put(
                            "previousGroupId",
                            if (statement.isNull(3)) null else statement.getText(3)
                        )
                        put(
                            "nextGroupId",
                            if (statement.isNull(4)) null else statement.getText(4)
                        )
                    }
                )
            }
            sortGroups(oldGroups).forEachIndexed { index, map ->
                connection.prepare(
                    "INSERT OR REPLACE INTO `${GROUP_TABLE_NAME}_Backup` " +
                            "(${GroupBean.GROUP_ID_COLUMN}, ${GroupBean.NAME_COLUMN}, " +
                            "${GroupBean.IS_EXPANDED_COLUMN}, ${GroupBean.ORDER_POSITION_COLUMN}) " +
                            "VALUES (?, ?, ?, ?)"
                ).use { insertStmt ->
                    insertStmt.bindText(1, map[GroupBean.GROUP_ID_COLUMN]!!)
                    insertStmt.bindText(2, map[GroupBean.NAME_COLUMN]!!)
                    insertStmt.bindInt(3, map[GroupBean.IS_EXPANDED_COLUMN]!!.toInt())
                    insertStmt.bindDouble(4, (index * ORDER_DELTA) + ORDER_DELTA)
                    insertStmt.step()
                }
            }
        }

        // Drop old table
        connection.execSQL("DROP TABLE `$GROUP_TABLE_NAME`")
        connection.execSQL("ALTER TABLE ${GROUP_TABLE_NAME}_Backup RENAME to `$GROUP_TABLE_NAME`")

        // Change Feed table
        connection.execSQL(
            "CREATE TABLE ${FEED_TABLE_NAME}_Backup (" +
                    "${FeedBean.URL_COLUMN} TEXT PRIMARY KEY NOT NULL, " +
                    "${FeedBean.TITLE_COLUMN} TEXT, " +
                    "${FeedBean.DESCRIPTION_COLUMN} TEXT, " +
                    "${FeedBean.LINK_COLUMN} TEXT, " +
                    "${FeedBean.ICON_COLUMN} TEXT, " +
                    "${FeedBean.GROUP_ID_COLUMN} TEXT, " +
                    "${FeedBean.NICKNAME_COLUMN} TEXT, " +
                    "${FeedBean.CUSTOM_DESCRIPTION_COLUMN} TEXT, " +
                    "${FeedBean.CUSTOM_ICON_COLUMN} TEXT, " +
                    "${FeedBean.SORT_XML_ARTICLES_ON_UPDATE_COLUMN} INTEGER NOT NULL DEFAULT 0, " +
                    "${FeedBean.REQUEST_HEADERS_COLUMN} TEXT, " +
                    "${FeedBean.MUTE_COLUMN} INTEGER NOT NULL DEFAULT 0" +
                    ")"
        )
        connection.execSQL(
            "INSERT INTO ${FEED_TABLE_NAME}_Backup SELECT " +
                    "${FeedBean.URL_COLUMN}, ${FeedBean.TITLE_COLUMN}, " +
                    "${FeedBean.DESCRIPTION_COLUMN}, ${FeedBean.LINK_COLUMN}, " +
                    "${FeedBean.ICON_COLUMN}, ${FeedBean.GROUP_ID_COLUMN}, " +
                    "${FeedBean.NICKNAME_COLUMN}, ${FeedBean.CUSTOM_DESCRIPTION_COLUMN}, " +
                    "${FeedBean.CUSTOM_ICON_COLUMN}, ${FeedBean.SORT_XML_ARTICLES_ON_UPDATE_COLUMN}, " +
                    "${FeedBean.REQUEST_HEADERS_COLUMN}, ${FeedBean.MUTE_COLUMN} " +
                    "FROM $FEED_TABLE_NAME"
        )
        connection.execSQL("DROP TABLE `$FEED_TABLE_NAME`")
        connection.execSQL("ALTER TABLE ${FEED_TABLE_NAME}_Backup RENAME to `$FEED_TABLE_NAME`")

        connection.execSQL("PRAGMA foreign_keys = ON")
    }
}
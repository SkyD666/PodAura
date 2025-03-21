package com.skyd.anivu.model.db.migration

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getStringOrNull
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skyd.anivu.ext.calculateHashMapInitialCapacity
import com.skyd.anivu.model.bean.feed.FEED_TABLE_NAME
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.bean.group.GROUP_TABLE_NAME
import com.skyd.anivu.model.bean.group.GroupBean
import com.skyd.anivu.model.db.dao.GroupDao.Companion.ORDER_DELTA

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

    override fun migrate(db: SupportSQLiteDatabase) {
        // Create backup table
        db.execSQL("PRAGMA foreign_keys = OFF")
        db.execSQL(
            "CREATE TABLE ${GROUP_TABLE_NAME}_Backup (" +
                    "${GroupBean.GROUP_ID_COLUMN} TEXT PRIMARY KEY NOT NULL, " +
                    "${GroupBean.NAME_COLUMN} TEXT NOT NULL, " +
                    "${GroupBean.IS_EXPANDED_COLUMN} INTEGER NOT NULL, " +
                    "${GroupBean.ORDER_POSITION_COLUMN} REAL NOT NULL" +
                    ")"
        )

        val cursor = db.query(
            "SELECT ${GroupBean.GROUP_ID_COLUMN}, ${GroupBean.NAME_COLUMN}, " +
                    "${GroupBean.IS_EXPANDED_COLUMN}, previousGroupId, nextGroupId FROM `$GROUP_TABLE_NAME`"
        )
        val oldGroups = mutableListOf<Map<String, String?>>()
        while (cursor.moveToNext()) {
            oldGroups.add(
                buildMap {
                    put(GroupBean.GROUP_ID_COLUMN, cursor.getString(0))
                    put(GroupBean.NAME_COLUMN, cursor.getString(1))
                    put(GroupBean.IS_EXPANDED_COLUMN, cursor.getInt(2).toString())
                    put("previousGroupId", cursor.getStringOrNull(3))
                    put("nextGroupId", cursor.getStringOrNull(4))
                }
            )
        }
        val newGroupContentValues = sortGroups(oldGroups).mapIndexed { index, map ->
            ContentValues().apply {
                put(GroupBean.GROUP_ID_COLUMN, map[GroupBean.GROUP_ID_COLUMN])
                put(GroupBean.NAME_COLUMN, map[GroupBean.NAME_COLUMN])
                put(GroupBean.IS_EXPANDED_COLUMN, map[GroupBean.IS_EXPANDED_COLUMN]!!.toInt())
                put(GroupBean.ORDER_POSITION_COLUMN, (index * ORDER_DELTA) + ORDER_DELTA)
            }
        }
        newGroupContentValues.forEach { group ->
            db.insert("`${GROUP_TABLE_NAME}_Backup`", SQLiteDatabase.CONFLICT_REPLACE, group)
        }

        // Drop old table
        db.execSQL("DROP TABLE `$GROUP_TABLE_NAME`")
        db.execSQL("ALTER TABLE ${GROUP_TABLE_NAME}_Backup RENAME to `$GROUP_TABLE_NAME`")

        // Change Feed table
        db.execSQL(
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
        db.execSQL(
            "INSERT INTO ${FEED_TABLE_NAME}_Backup SELECT " +
                    "${FeedBean.URL_COLUMN}, ${FeedBean.TITLE_COLUMN}, " +
                    "${FeedBean.DESCRIPTION_COLUMN}, ${FeedBean.LINK_COLUMN}, " +
                    "${FeedBean.ICON_COLUMN}, ${FeedBean.GROUP_ID_COLUMN}, " +
                    "${FeedBean.NICKNAME_COLUMN}, ${FeedBean.CUSTOM_DESCRIPTION_COLUMN}, " +
                    "${FeedBean.CUSTOM_ICON_COLUMN}, ${FeedBean.SORT_XML_ARTICLES_ON_UPDATE_COLUMN}, " +
                    "${FeedBean.REQUEST_HEADERS_COLUMN}, ${FeedBean.MUTE_COLUMN} " +
                    "FROM $FEED_TABLE_NAME"
        )
        db.execSQL("DROP TABLE `$FEED_TABLE_NAME`")
        db.execSQL("ALTER TABLE ${FEED_TABLE_NAME}_Backup RENAME to `$FEED_TABLE_NAME`")

        db.execSQL("PRAGMA foreign_keys = ON")
    }
}
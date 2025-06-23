package com.skyd.podaura.model.repository.importexport.opml

import com.skyd.compone.component.blockString
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.db.dao.GroupDao
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.import_opml_conflict_strategy_replace
import podaura.shared.generated.resources.import_opml_conflict_strategy_skip
import kotlin.uuid.Uuid


sealed class ImportOpmlConflictStrategy {
    companion object {
        val strategies = listOf(SkipStrategy, ReplaceStrategy)
    }

    abstract suspend fun handle(
        groupDao: GroupDao,
        feedDao: FeedDao,
        opmlGroupWithFeed: OpmlGroupWithFeed,
    ): Int

    abstract val displayName: String

    fun checkOpmlGroupWithFeedFormat(opmlGroupWithFeed: OpmlGroupWithFeed) {
        opmlGroupWithFeed.feeds.forEach {
            check(it.url.isNotBlank()) { "Feed's URL is blank: Feed title: ${it.title}" }
        }
    }

    /**
     * Add or merge a group
     *
     * @return groupId. return null if the group is default group.
     */
    suspend fun addOrMergeGroup(groupDao: GroupDao, group: GroupVo): String? {
        if (group.groupId == GroupVo.DEFAULT_GROUP_ID) {
            return null
        }
        var groupId = Uuid.random().toString()
        if (groupDao.containsByName(group.name) == 0) {
            groupDao.setGroup(
                GroupVo(
                    groupId = groupId,
                    name = group.name,
                    isExpanded = true,
                ).toPo(orderPosition = groupDao.getMaxOrder() + GroupDao.ORDER_DELTA)
            )
        } else {
            groupId = groupDao.queryGroupIdByName(group.name)
        }
        return groupId
    }

    // Skip on conflict
    data object SkipStrategy : ImportOpmlConflictStrategy() {
        override suspend fun handle(
            groupDao: GroupDao,
            feedDao: FeedDao,
            opmlGroupWithFeed: OpmlGroupWithFeed,
        ): Int {
            checkOpmlGroupWithFeedFormat(opmlGroupWithFeed)

            var importedCount = 0
            val groupId = addOrMergeGroup(groupDao, opmlGroupWithFeed.group)

            opmlGroupWithFeed.feeds.forEach { feed ->
                // Skip
                if (feedDao.containsByUrl(feed.url) == 0) {
                    feedDao.setFeed(feed.copy(groupId = groupId))
                    importedCount++
                }
            }
            return importedCount
        }

        override val displayName: String
            get() = blockString(Res.string.import_opml_conflict_strategy_skip)
    }

    // Replace on conflict
    data object ReplaceStrategy : ImportOpmlConflictStrategy() {
        override suspend fun handle(
            groupDao: GroupDao,
            feedDao: FeedDao,
            opmlGroupWithFeed: OpmlGroupWithFeed,
        ): Int {
            checkOpmlGroupWithFeedFormat(opmlGroupWithFeed)

            val groupId = addOrMergeGroup(groupDao, opmlGroupWithFeed.group)

            opmlGroupWithFeed.feeds.forEach { feed ->
                // Replace
                // @Insert(onConflict = OnConflictStrategy.REPLACE)
                feedDao.setFeed(feed.copy(groupId = groupId))
            }
            return opmlGroupWithFeed.feeds.size
        }

        override val displayName: String
            get() = blockString(Res.string.import_opml_conflict_strategy_replace)
    }
}
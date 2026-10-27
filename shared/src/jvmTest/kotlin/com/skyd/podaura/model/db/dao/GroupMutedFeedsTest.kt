package com.skyd.podaura.model.db.dao

import androidx.paging.PagingSource
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.group.GroupBean
import com.skyd.podaura.model.bean.group.groupfeed.GroupOrFeedBean
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.instance
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GroupMutedFeedsTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var directory: File
    private lateinit var database: AppDatabase

    @BeforeTest
    fun setUp() {
        directory = Files.createTempDirectory("group-muted-feeds-test").toFile()
        database = AppDatabase.instance(
            Room.databaseBuilder<AppDatabase>(File(directory, "test.db").absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(dispatcher)
        )
    }

    @AfterTest
    fun tearDown() {
        database.close()
        directory.deleteRecursively()
    }

    @Test
    fun countsIncludeHiddenFeedsInDefaultMixedAllMutedAndEmptyGroups() = runTest(dispatcher) {
        for ((index, id) in listOf("mixed", "muted", "empty").withIndex()) {
            database.groupDao().setGroup(GroupBean(id, id, true, index * 10.0))
        }
        for (groupId in listOf(null, "mixed", "muted")) {
            database.feedDao().setFeed(FeedBean("$groupId-muted", groupId = groupId, mute = true))
            if (groupId != "muted") {
                database.feedDao().setFeed(FeedBean("$groupId-visible", groupId = groupId))
            }
        }
        for (expanded in listOf(true, false)) {
            database.groupDao().collapseAllGroup(!expanded)
            for (hideMuted in listOf(true, false)) {
                val rows = loadRows(expanded, hideMuted)
                assertEquals(
                    mapOf(null to (2 to 1), "mixed" to (2 to 1), "muted" to (1 to 1), "empty" to (0 to 0)),
                    rows.filter { it.id.feedUrl == null }.associate {
                        it.id.groupId to (it.feedCount to it.mutedFeedCount)
                    },
                )
                val visibleFeeds = rows.mapNotNull { it.feed?.feed }
                assertEquals(if (!expanded) 0 else if (hideMuted) 2 else 5, visibleFeeds.size)
                if (hideMuted) assertTrue(visibleFeeds.none { it.mute })
            }
        }
    }

    @Test
    fun mutingAFeedInvalidatesThePageAndUpdatesGroupCounts() = runTest(dispatcher) {
        database.groupDao().setGroup(GroupBean("group", "Group", true, 10.0))
        val feed = FeedBean("feed", groupId = "group")
        database.feedDao().setFeed(feed)
        dispatcher.scheduler.advanceUntilIdle()
        val source = database.groupDao().getGroupsAndFeeds(true, true, true)
        try {
            source.load(PagingSource.LoadParams.Refresh(null, 3, true))
            database.feedDao().updateFeed(feed.copy(mute = true))
            dispatcher.scheduler.advanceUntilIdle()
            assertTrue(source.invalid)
            val rows = loadRows()
            assertEquals(1, rows.size)
            assertEquals(1, rows.single().feedCount)
            assertEquals(1, rows.single().mutedFeedCount)
        } finally {
            source.invalidate()
        }
    }

    private suspend fun loadRows(
        defaultGroupIsExpanded: Boolean = true,
        hideMutedFeed: Boolean = true,
    ): List<GroupOrFeedBean> {
        dispatcher.scheduler.advanceUntilIdle()
        val source = database.groupDao().getGroupsAndFeeds(defaultGroupIsExpanded, true, hideMutedFeed)
        val rows = mutableListOf<GroupOrFeedBean>()
        var params: PagingSource.LoadParams<Int> = PagingSource.LoadParams.Refresh(null, 3, true)
        try {
            while (true) {
                val page = assertIs<PagingSource.LoadResult.Page<Int, GroupOrFeedBean>>(source.load(params))
                rows.addAll(page.data)
                val nextKey = page.nextKey ?: break
                params = PagingSource.LoadParams.Append(nextKey, 3, true)
            }
        } finally {
            source.invalidate()
        }
        return rows
    }
}

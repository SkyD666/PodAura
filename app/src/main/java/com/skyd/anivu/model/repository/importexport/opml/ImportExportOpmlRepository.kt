package com.skyd.anivu.model.repository.importexport.opml

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import be.ceau.opml.OpmlParser
import be.ceau.opml.OpmlWriter
import be.ceau.opml.entity.Body
import be.ceau.opml.entity.Head
import be.ceau.opml.entity.Opml
import be.ceau.opml.entity.Outline
import com.skyd.anivu.appContext
import com.skyd.anivu.model.repository.BaseRepository
import com.skyd.anivu.ext.getAppName
import com.skyd.anivu.ext.toAbsoluteDateTimeString
import com.skyd.anivu.ext.validateFileName
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.bean.feed.FeedViewBean
import com.skyd.anivu.model.bean.group.GroupVo
import com.skyd.anivu.model.bean.group.GroupWithFeedBean
import com.skyd.anivu.model.db.dao.FeedDao
import com.skyd.anivu.model.db.dao.GroupDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory
import java.io.InputStream
import java.util.Date
import kotlin.random.Random
import kotlin.time.measureTime

@Factory(
    binds = [
        ImportExportOpmlRepository::class,
        IImportOpmlRepository::class,
        IExportOpmlRepository::class,
    ]
)
class ImportExportOpmlRepository(
    private val feedDao: FeedDao,
    private val groupDao: GroupDao,
) : BaseRepository(), IImportOpmlRepository, IExportOpmlRepository {
    private fun groupWithFeedsWithoutDefaultGroup(): Flow<List<GroupWithFeedBean>> =
        groupDao.getGroupWithFeeds().flowOn(Dispatchers.IO)

    private suspend fun defaultGroupFeeds(): Flow<List<FeedViewBean>> =
        groupDao.getGroupIds().map { groupIds ->
            feedDao.getFeedsNotInGroup(groupIds)
        }.flowOn(Dispatchers.IO)

    override fun importOpmlMeasureTime(
        opmlUri: Uri,
        strategy: ImportOpmlConflictStrategy,
    ): Flow<IImportOpmlRepository.ImportOpmlResult> = flow {
        var importedFeedCount = 0
        val time = measureTime {
            appContext.contentResolver.openInputStream(opmlUri)!!.use {
                parseOpml(it)
            }.forEach { opmlGroupWithFeed ->
                importedFeedCount += strategy.handle(
                    groupDao = groupDao,
                    feedDao = feedDao,
                    opmlGroupWithFeed = opmlGroupWithFeed,
                )
            }
        }.inWholeMilliseconds

        emit(
            IImportOpmlRepository.ImportOpmlResult(
                time = time,
                importedFeedCount = importedFeedCount,
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun parseOpml(inputStream: InputStream): List<OpmlGroupWithFeed> {
        fun MutableList<OpmlGroupWithFeed>.addGroup(group: GroupVo) = add(
            OpmlGroupWithFeed(group = group, feeds = mutableListOf())
        )

        fun MutableList<OpmlGroupWithFeed>.addFeed(feed: FeedBean) = last().feeds.add(feed)
        fun MutableList<OpmlGroupWithFeed>.addFeedToDefault(feed: FeedBean) =
            first().feeds.add(feed)

        fun Outline.toFeed(groupId: String) = FeedBean(
            url = attributes["xmlUrl"]!!,
            title = attributes["title"] ?: text.toString(),
            description = attributes["description"],
            link = attributes["link"],
            icon = attributes["icon"],
            groupId = groupId,
            nickname = attributes["nickname"],
            customDescription = attributes["customDescription"],
            customIcon = attributes["customIcon"]?.let {
                if (it.startsWith("http://") || it.startsWith("https://")) it
                else null
            }
        )

        val opml = OpmlParser().parse(inputStream)
        val groupWithFeedList = mutableListOf<OpmlGroupWithFeed>().apply {
            addGroup(GroupVo.DefaultGroup)
        }

        opml.body.outlines.forEach {
            // Only feeds
            if (it.subElements.isEmpty()) {
                // It's a empty group
                if (it.attributes["xmlUrl"] == null) {
                    if (!it.attributes["isDefault"].toBoolean()) {
                        groupWithFeedList.addGroup(
                            GroupVo(
                                groupId = "",
                                name = it.attributes["title"] ?: it.text.toString(),
                                isExpanded = true,
                            )
                        )
                    }
                } else {
                    groupWithFeedList.addFeedToDefault(it.toFeed(groupId = GroupVo.DefaultGroup.groupId))
                }
            } else {
                if (!it.attributes["isDefault"].toBoolean()) {
                    groupWithFeedList.addGroup(
                        GroupVo(
                            groupId = "",
                            name = it.attributes["title"] ?: it.text.toString(),
                            isExpanded = true,
                        )
                    )
                }
                it.subElements.forEach { outline ->
                    groupWithFeedList.addFeed(outline.toFeed(groupId = ""))
                }
            }
        }

        return groupWithFeedList
    }

    override fun exportOpmlMeasureTime(outputDir: Uri): Flow<Long> = flow {
        emit(measureTime { exportOpml(outputDir) }.inWholeMilliseconds)
    }.flowOn(Dispatchers.IO)

    private fun createFeedOutlineList(feeds: List<FeedViewBean>): List<Outline> {
        return feeds.map { feedView ->
            val feed = feedView.feed
            Outline(
                mutableMapOf(
                    "text" to feed.title,
                    "title" to feed.title,
                    "xmlUrl" to feed.url,
                    "htmlUrl" to feed.url,
                ).apply {
                    feed.description?.let { put("description", it) }
                    feed.link?.let { put("link", it) }
                    feed.icon?.let { put("icon", it) }
                    feed.nickname?.let { put("nickname", it) }
                    feed.customDescription?.let { put("customDescription", it) }
                    feed.customIcon?.let {
                        if (it.startsWith("http://") || it.startsWith("https://")) {
                            put("customIcon", it)
                        }
                    }
                },
                listOf()
            )
        }
    }

    private suspend fun exportOpml(outputDir: Uri) {
        val text = OpmlWriter().write(
            Opml(
                "2.0",
                Head(
                    appContext.getAppName(),
                    Date().toString(), null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                ),
                Body(
                    mutableListOf(
                        // Default group feeds (No group)
                        *createFeedOutlineList(defaultGroupFeeds().first()).toTypedArray(),
                        // Other groups
                        *groupWithFeedsWithoutDefaultGroup().first().map { groupWithFeeds ->
                            Outline(
                                mutableMapOf(
                                    "text" to groupWithFeeds.group.name,
                                    "title" to groupWithFeeds.group.name,
                                ),
                                createFeedOutlineList(groupWithFeeds.feeds)
                            )
                        }.toTypedArray()
                    )
                )
            )
        )!!

        saveOpml(text, outputDir)
    }

    private fun saveOpml(text: String, outputDir: Uri) {
        val appName = appContext.getAppName()
        val fileName = (appName + "_" + System.currentTimeMillis().toAbsoluteDateTimeString() +
                "_" + Random.nextInt(0, Int.MAX_VALUE).toString() + ".opml").validateFileName()

        val documentFile = DocumentFile.fromTreeUri(appContext, outputDir)!!
        val opmlUri: Uri = documentFile.createFile("text/x-opml", fileName)!!.uri
        val opmlOutputStream = appContext.contentResolver.openOutputStream(opmlUri)!!
        opmlOutputStream.writer().use { writer ->
            writer.write(text)
        }
    }
}
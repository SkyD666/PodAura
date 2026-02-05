package com.skyd.podaura.model.repository.importexport.opml

import com.skyd.podaura.model.repository.importexport.opmlparser.OPML
import com.skyd.podaura.ext.takeIfNotBlank
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.model.bean.group.GroupWithFeedBean
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.db.dao.GroupDao
import com.skyd.podaura.model.repository.BaseRepository
import com.skyd.podaura.model.repository.importexport.opmlparser.decodeFromSource
import com.skyd.podaura.model.repository.importexport.opmlparser.dsl.OutlineDsl
import com.skyd.podaura.model.repository.importexport.opmlparser.dsl.opml
import com.skyd.podaura.model.repository.importexport.opmlparser.encodeToSink
import com.skyd.podaura.model.repository.importexport.opmlparser.entity.Outline
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.app_name
import kotlin.time.Clock
import kotlin.time.measureTime

class ImportExportOpmlRepository(
    private val feedDao: FeedDao,
    private val groupDao: GroupDao,
) : BaseRepository(), IImportOpmlRepository, IExportOpmlRepository {
    private val opml = OPML()

    private fun groupWithFeedsWithoutDefaultGroup(): Flow<List<GroupWithFeedBean>> =
        groupDao.getGroupWithFeeds().flowOn(Dispatchers.IO)

    private suspend fun defaultGroupFeeds(): Flow<List<FeedViewBean>> =
        groupDao.getGroupIds().map { groupIds ->
            feedDao.getFeedsNotInGroup(groupIds)
        }.flowOn(Dispatchers.IO)

    override fun importOpmlMeasureTime(
        opmlFile: PlatformFile,
        strategy: ImportOpmlConflictStrategy,
    ): Flow<IImportOpmlRepository.ImportOpmlResult> = flow {
        var importedFeedCount = 0
        val time = measureTime {
            opmlFile.source().buffered().use { parseOpml(it) }
                .forEach { opmlGroupWithFeed ->
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

    private fun parseOpml(source: Source): List<OpmlGroupWithFeed> {
        fun MutableList<OpmlGroupWithFeed>.addGroup(group: GroupVo) = add(
            OpmlGroupWithFeed(group = group, feeds = mutableListOf())
        )

        fun MutableList<OpmlGroupWithFeed>.addFeed(feed: FeedBean) = last().feeds.add(feed)
        fun MutableList<OpmlGroupWithFeed>.addFeedToDefault(feed: FeedBean) =
            first().feeds.add(feed)

        fun Outline.toFeed(groupId: String) = FeedBean(
            url = xmlUrl!!,
            title = title ?: text.toString(),
            description = description,
            link = link,
            icon = attributes["icon"]?.takeIf { it.isNotBlank() },
            groupId = groupId,
            nickname = attributes["nickname"]?.takeIf { it.isNotBlank() },
            customDescription = attributes["customDescription"]?.takeIf { it.isNotBlank() },
            customIcon = attributes["customIcon"]?.takeIf {
                it.startsWith("http://") || it.startsWith("https://")
            }
        )

        val opmlObject = opml.decodeFromSource(source.buffered())
        val groupWithFeedList = mutableListOf<OpmlGroupWithFeed>().apply {
            addGroup(GroupVo.DefaultGroup)
        }

        opmlObject.body.outlines.forEach {
            // Only feeds
            if (it.outlines.isNullOrEmpty()) {
                // It's a empty group
                if (it.xmlUrl == null) {
                    groupWithFeedList.addGroup(
                        GroupVo(
                            groupId = "",
                            name = it.title ?: it.text.toString(),
                            isExpanded = true,
                        )
                    )
                } else {
                    groupWithFeedList.addFeedToDefault(it.toFeed(groupId = GroupVo.DefaultGroup.groupId))
                }
            } else {
                groupWithFeedList.addGroup(
                    GroupVo(
                        groupId = "",
                        name = it.title ?: it.text.toString(),
                        isExpanded = true,
                    )
                )
                it.outlines.forEach { outline ->
                    groupWithFeedList.addFeed(outline.toFeed(groupId = ""))
                }
            }
        }

        return groupWithFeedList
    }

    override fun exportOpmlMeasureTime(outputDir: PlatformFile): Flow<Long> = flow {
        emit(measureTime {
            outputDir.sink().buffered().use { sink -> exportOpml(sink) }
        }.inWholeMilliseconds)
    }.flowOn(Dispatchers.IO)

    private fun OutlineDsl.addFeedOutlines(feeds: List<FeedViewBean>) {
        feeds.sortedBy { it.feed.orderPosition }.forEach { feedView ->
            val feed = feedView.feed
            outline {
                title = feed.title
                text = feed.title
                description = feed.description
                htmlUrl = feed.url
                xmlUrl = feed.url
                link = feed.link
                attribute("icon", feed.icon?.takeIfNotBlank())
                attribute("nickname", feed.nickname?.takeIfNotBlank())
                attribute("customDescription", feed.customDescription?.takeIfNotBlank())
                attribute("customIcon", feed.customIcon?.takeIf {
                    it.startsWith("http://") || it.startsWith("https://")
                })
            }
        }
    }

    private suspend fun exportOpml(sink: Sink) {
        val opmlObject = opml {
            version = "2.0"
            head {
                title = getString(Res.string.app_name)
                dateCreated = Clock.System.now().toString()
            }
            body {
                defaultGroupFeeds().first().forEach { feedView ->
                    val feed = feedView.feed
                    outline {
                        title = feed.title
                        text = feed.title
                        description = feed.description
                        htmlUrl = feed.url
                        xmlUrl = feed.url
                        link = feed.link
                        attribute("icon", feed.icon?.takeIfNotBlank())
                        attribute("nickname", feed.nickname?.takeIfNotBlank())
                        attribute("customDescription", feed.customDescription?.takeIfNotBlank())
                        attribute("customIcon", feed.customIcon?.takeIf {
                            it.startsWith("http://") || it.startsWith("https://")
                        })
                    }
                }
                // Default group feeds (No group)
                addFeedOutlines(defaultGroupFeeds().first())
                // Other groups
                groupWithFeedsWithoutDefaultGroup().first().forEach { groupWithFeeds ->
                    outline {
                        title = groupWithFeeds.group.name
                        text = groupWithFeeds.group.name
                        addFeedOutlines(groupWithFeeds.feeds)
                    }
                }
            }
        }

        opml.encodeToSink(sink, opmlObject)
    }
}

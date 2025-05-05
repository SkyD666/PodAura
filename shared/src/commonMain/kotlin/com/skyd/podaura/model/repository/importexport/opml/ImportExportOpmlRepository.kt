package com.skyd.podaura.model.repository.importexport.opml

import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.model.bean.group.GroupWithFeedBean
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.db.dao.GroupDao
import com.skyd.podaura.model.repository.BaseRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import net.devrieze.xmlutil.serialization.kxio.decodeFromSource
import net.devrieze.xmlutil.serialization.kxio.encodeToSink
import nl.adaptivity.xmlutil.serialization.XML
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Factory
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.app_name
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
    private val xml = XML {
        autoPolymorphic = true
        indentString = "  "
        defaultPolicy {
            pedantic = false
            ignoreUnknownChildren()
        }
    }

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
            icon = icon,
            groupId = groupId,
            nickname = nickname,
            customDescription = customDescription,
            customIcon = customIcon?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        )

        val opml = xml.decodeFromSource(Opml.serializer(), source.buffered())
        val groupWithFeedList = mutableListOf<OpmlGroupWithFeed>().apply {
            addGroup(GroupVo.DefaultGroup)
        }

        opml.body.outlines.forEach {
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

    private fun createFeedOutlineList(feeds: List<FeedViewBean>): List<Outline> {
        return feeds.map { feedView ->
            val feed = feedView.feed
            Outline(
                title = feed.title,
                text = feed.title,
                description = feed.description,
                htmlUrl = feed.url,
                xmlUrl = feed.url,
                link = feed.link,
                icon = feed.icon,
                nickname = feed.nickname,
                customDescription = feed.customDescription,
                customIcon = feed.customIcon?.takeIf { it.startsWith("http://") || it.startsWith("https://") },
            )
        }
    }

    private suspend fun exportOpml(sink: Sink) {
        val opml = Opml(
            version = "2.0",
            head = Head(
                title = getString(Res.string.app_name),
                dateCreated = Clock.System.now().toString(),
            ),
            body = Body(
                outlines = listOf(
                    // Default group feeds (No group)
                    *createFeedOutlineList(defaultGroupFeeds().first()).toTypedArray(),
                    // Other groups
                    *groupWithFeedsWithoutDefaultGroup().first().map { groupWithFeeds ->
                        Outline(
                            title = groupWithFeeds.group.name,
                            text = groupWithFeeds.group.name,
                            outlines = createFeedOutlineList(groupWithFeeds.feeds),
                        )
                    }.toTypedArray()
                )
            )
        )

        xml.encodeToSink(sink, Opml.serializer(), opml)
    }
}
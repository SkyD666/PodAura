package com.skyd.podaura.model.repository.media

import androidx.collection.LruCache
import androidx.compose.ui.util.fastFirstOrNull
import com.skyd.fundation.ext.atomicMove
import com.skyd.fundation.ext.createDirectories
import com.skyd.fundation.ext.currentTimeMillis
import com.skyd.fundation.ext.deleteRecursively
import com.skyd.fundation.ext.exists
import com.skyd.fundation.ext.isDirectory
import com.skyd.fundation.ext.isFile
import com.skyd.fundation.ext.list
import com.skyd.fundation.ext.sink
import com.skyd.fundation.ext.source
import com.skyd.fundation.ext.walk
import com.skyd.podaura.ext.flowOf
import com.skyd.podaura.ext.splitByBlank
import com.skyd.podaura.ext.validateFileName
import com.skyd.podaura.model.bean.MediaBean
import com.skyd.podaura.model.bean.MediaGroupBean
import com.skyd.podaura.model.bean.MediaGroupBean.Companion.isDefaultGroup
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.preference.appearance.media.MediaFileFilterPreference
import com.skyd.podaura.model.preference.behavior.media.BaseMediaListSortByPreference
import com.skyd.podaura.model.preference.behavior.media.MediaListSortAscPreference
import com.skyd.podaura.model.preference.behavior.media.MediaListSortByPreference
import com.skyd.podaura.model.preference.behavior.media.MediaSubListSortAscPreference
import com.skyd.podaura.model.preference.behavior.media.MediaSubListSortByPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.repository.BaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.files.Path
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.io.decodeFromSource
import kotlinx.serialization.json.io.encodeToSink
import kotlin.time.Clock

class MediaRepository(
    private val json: Json,
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
) : BaseRepository(), IMediaRepository {

    companion object {
        const val FOLDER_INFO_JSON_NAME = "info.json"
        const val OLD_MEDIA_LIB_JSON_NAME = "group.json"
        const val MEDIA_LIB_JSON_NAME = "MediaLib.json"

        private val mediaLibJsons = LruCache<String, MediaLibJson>(maxSize = 10)

        private val refreshPath = MutableSharedFlow<String>(extraBufferCapacity = Int.MAX_VALUE)
    }

    private fun parseMediaLibJson(mediaLibRootJsonFile: Path): MediaLibJson? {
        if (!mediaLibRootJsonFile.exists()) {
            Path(mediaLibRootJsonFile.parent!!, OLD_MEDIA_LIB_JSON_NAME).apply {
                if (exists()) atomicMove(mediaLibRootJsonFile)
            }
        }
        if (!mediaLibRootJsonFile.exists()) return null
        return json.decodeFromSource<MediaLibJson>(mediaLibRootJsonFile.source()).apply {
            files.removeIf {
                !Path(mediaLibRootJsonFile.parent!!, it.fileName).exists() ||
                        it.fileName.equals(FOLDER_INFO_JSON_NAME, true) ||
                        it.fileName.equals(MEDIA_LIB_JSON_NAME, true)
            }
        }
    }

    private suspend fun getOrReadMediaLibJson(path: String): MediaLibJson {
        val existFiles: (String) -> List<Path> = { p ->
            Path(p).list().toMutableList().filter { it.exists() }
        }
        val existMediaLibJson = mediaLibJsons[path]
        if (existMediaLibJson != null) {
            existMediaLibJson.files.appendFiles(existFiles(path))
            return existMediaLibJson
        }

        val newMediaLibJson = parseMediaLibJson(Path(path, MEDIA_LIB_JSON_NAME))
            ?: MediaLibJson(files = mutableListOf())
        newMediaLibJson.files.appendFiles(existFiles(path))

        mediaLibJsons.put(path, newMediaLibJson)
        tryFixWrongArticleId(path, newMediaLibJson)

        return newMediaLibJson
    }

    // Format groups
    private fun formatMediaLibJson(old: MediaLibJson): MediaLibJson {
        val allGroups = (old.files.map { it.groupName } + old.allGroups)
            .distinct().filterNotNull().toMutableList()
        return MediaLibJson(
            allGroups = allGroups,
            files = old.files,
        )
    }

    private suspend fun writeMediaLibJson(path: String, data: MediaLibJson) {
        Path(path, MEDIA_LIB_JSON_NAME).sink().use {
            json.encodeToSink(formatMediaLibJson(data), it)
        }
        refreshPath.emit(path)
    }

    private val appendFilesMutex = Mutex()
    private suspend fun MutableList<FileJson>.appendFiles(
        files: List<Path>,
        fileJsonBuild: (Path) -> FileJson = {
            FileJson(
                fileName = it.name,
                groupName = null,
                isFile = it.isFile,
                displayName = null,
                articleId = null,
                articleLink = null,
                articleGuid = null,
                feedUrl = null,
            )
        },
    ) = appendFilesMutex.withLock {
        if (files.isEmpty()) return@withLock
        removeIf { !Path(files[0].parent!!, it.fileName).exists() }
        files.forEach { file ->
            if (file.name.equals(FOLDER_INFO_JSON_NAME, true) ||
                file.name.equals(MEDIA_LIB_JSON_NAME, true)
            ) {
                return@forEach
            }
            if (firstOrNull { it.fileName == file.name } == null) {
                add(fileJsonBuild(file))
            }
        }
    }

    // Try fix wrong articleId
    private suspend fun tryFixWrongArticleId(
        path: String,
        mediaLibJson: MediaLibJson,
    ) {
        var shouldFix = false
        mediaLibJson.files.forEach { json ->
            if (json.articleId?.let { articleDao.exists(it) > 0 } != true) {
                val feedUrl = json.feedUrl
                if (feedUrl != null) {
                    val newArticleId =
                        articleDao.queryArticleByGuid(json.articleGuid, feedUrl)?.articleId
                            ?: articleDao.queryArticleByLink(json.articleLink, feedUrl)?.articleId

                    json.articleId = newArticleId
                    shouldFix = true
                }
            }
        }
        if (shouldFix) {
            writeMediaLibJson(path, mediaLibJson)
        }
    }

    private fun FileJson.toMediaBean(
        path: String,
        articleWithEnclosure: ArticleWithEnclosureBean?,
        feedBean: FeedBean?,
    ): MediaBean? {
        val file = Path(path, fileName)
        if (!file.exists()) return null
        val fileCount = if (file.isDirectory) {
            runCatching { file.list().size }.getOrNull()?.run {
                this - listOf(
                    Path(file, MEDIA_LIB_JSON_NAME).exists(),
                    Path(file, FOLDER_INFO_JSON_NAME).exists(),
                ).count { it }
            } ?: 0
        } else 0

        return MediaBean(
            displayName = displayName,
            filePath = file.toString(),
            fileCount = fileCount,
            articleWithEnclosure = articleWithEnclosure,
            feedBean = feedBean,
        )
    }

    override fun requestGroups(path: String): Flow<List<MediaGroupBean>> = merge(
        kotlinx.coroutines.flow.flowOf(path), refreshFiles, refreshPath
    ).filter { it == path }.map {
        val allGroups = getOrReadMediaLibJson(path).allGroups
        listOf(MediaGroupBean.DefaultMediaGroup) +
                allGroups.map { MediaGroupBean(name = it) }.sortedBy { it.name }
    }.flowOn(Dispatchers.IO)

    private val refreshFiles =
        MutableSharedFlow<String>(replay = 1, extraBufferCapacity = Int.MAX_VALUE)

    override suspend fun refreshFiles(path: String) {
        return refreshFiles.emit(path)
    }

    override fun requestFiles(
        path: String,
        group: MediaGroupBean?,
        isSubList: Boolean,
    ): Flow<List<MediaBean>> = combine(
        merge(kotlinx.coroutines.flow.flowOf(path), refreshFiles, refreshPath).filter { it == path }
            .map {
                val mediaLibJson = getOrReadMediaLibJson(path)
                val fileJsons = mediaLibJson.files
                val videoList = (if (group == null) fileJsons else {
                    val groupName = if (group.isDefaultGroup()) null else group.name
                    fileJsons.filter { it.groupName == groupName }
                }).let { jsons ->
                    val articleMap = articleDao.getArticleWithFeedListByIds(
                        jsons.mapNotNull { it.articleId }
                    ).associateBy { it.articleWithEnclosure.article.articleId }

                    val feedMap = feedDao.getFeedsIn(
                        jsons.mapNotNull { it.feedUrl }
                    ).associateBy { it.feed.url }

                    jsons.mapNotNull { fileJson ->
                        fileJson.toMediaBean(
                            path = path,
                            articleWithEnclosure = articleMap[fileJson.articleId]?.articleWithEnclosure,
                            feedBean = feedMap[fileJson.feedUrl]?.feed
                                ?: articleMap[fileJson.articleId]?.feed,
                        )
                    }
                }
                videoList.toMutableList().apply {
                    fastFirstOrNull { it.name.equals(FOLDER_INFO_JSON_NAME, true) }
                        ?.let { remove(it) }
                    fastFirstOrNull { it.name.equals(MEDIA_LIB_JSON_NAME, true) }
                        ?.let { remove(it) }
                }
            },
        dataStore.flowOf(MediaFileFilterPreference),
    ) { videoList, displayFilter ->
        videoList.filter {
            runCatching {
                it.path.isDirectory || it.path.name.matches(Regex(displayFilter))
            }.getOrNull() == true
        }
    }.combine(
        dataStore.flowOf(
            if (isSubList) MediaSubListSortByPreference else MediaListSortByPreference
        )
    ) { list, sortBy ->
        when (sortBy) {
            BaseMediaListSortByPreference.DATE -> list.sortedBy { it.date }
            BaseMediaListSortByPreference.NAME -> list.sortedBy {
                it.displayName ?: it.name
            }

            BaseMediaListSortByPreference.FILE_COUNT -> list.sortedBy { it.fileCount }
            else -> list.sortedBy { it.displayName ?: it.name }
        }
    }.combine(
        dataStore.flowOf(
            if (isSubList) MediaSubListSortAscPreference else MediaListSortAscPreference
        )
    ) { list, sortAsc ->
        if (sortAsc) list else list.reversed()
    }.flowOn(Dispatchers.IO)

    override fun search(
        path: String,
        query: String,
        recursive: Boolean,
    ): Flow<List<MediaBean>> = kotlinx.coroutines.flow.flowOf(
        query.trim() to recursive
    ).flatMapLatest { (query, recursive) ->
        merge(
            kotlinx.coroutines.flow.flowOf(path), refreshFiles, refreshPath
        ).debounce(70).filter { it == path }.map {
            val queries = query.splitByBlank()

            val fileJsonsWithDirPath = mutableListOf<Pair<FileJson, String>>()
            Path(path).walk(onEnter = { dir ->
                dir.toString() == path || recursive
            }).filter { it.isDirectory }.asFlow().collect { dirPath ->
                val mediaLibJson = getOrReadMediaLibJson(dirPath.toString())
                fileJsonsWithDirPath += mediaLibJson.files.map { it to dirPath.toString() }
            }
            val fileJsons = fileJsonsWithDirPath.map { it.first }

            val articleMap = articleDao.getArticleWithFeedListByIds(
                fileJsons.mapNotNull { it.articleId }
            ).associateBy { it.articleWithEnclosure.article.articleId }
            val feedMap = feedDao.getFeedsIn(
                fileJsons.mapNotNull { it.feedUrl }
            ).associateBy { it.feed.url }

            fileJsonsWithDirPath.filter { (fileJson, _) ->
                queries.isEmpty() || queries.any {
                    it in fileJson.fileName ||
                            it in fileJson.displayName.orEmpty() ||
                            it in fileJson.feedUrl.orEmpty() ||
                            it in fileJson.articleLink.orEmpty()
                }
            }.mapNotNull { (fileJson, dirPath) ->
                fileJson.toMediaBean(
                    path = dirPath,
                    articleWithEnclosure = articleMap[fileJson.articleId]?.articleWithEnclosure,
                    feedBean = feedMap[fileJson.feedUrl]?.feed
                        ?: articleMap[fileJson.articleId]?.feed,
                )
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun deleteFile(file: Path): Flow<Boolean> = flow {
        val path = file.parent!!.toString()
        val mediaLibJson = getOrReadMediaLibJson(path).apply {
            files.removeIf { it.fileName == file.name }
        }
        writeMediaLibJson(path = path, mediaLibJson)
        emit(file.deleteRecursively() != null)
    }.flowOn(Dispatchers.IO)

    override fun renameFile(file: Path, newName: String): Flow<Path?> = flow {
        val path = file.parent!!.toString()
        val mediaLibJson = getOrReadMediaLibJson(path)
        val validateFileName = newName.validateFileName()
        val newFile = Path(file.parent!!, validateFileName)
        if (file.atomicMove(newFile)) {
            mediaLibJson.files.firstOrNull { it.fileName == file.name }?.fileName =
                validateFileName
            writeMediaLibJson(path = path, mediaLibJson)
            emit(newFile)
        } else {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    override fun setDisplayName(
        mediaBean: MediaBean,
        displayName: String?,
    ): Flow<MediaBean> = flow {
        val path = mediaBean.path.parent!!.toString()
        val mediaLibJson = getOrReadMediaLibJson(path = path)
        mediaLibJson.files.firstOrNull {
            it.fileName == mediaBean.path.name
        }?.displayName = if (displayName.isNullOrBlank()) null else displayName
        writeMediaLibJson(path = path, mediaLibJson)

        emit(mediaBean.copy(displayName = displayName))
    }.flowOn(Dispatchers.IO)

    override fun addNewFile(
        file: Path,
        groupName: String?,
        articleId: String?,
        displayName: String?,
    ): Flow<Boolean> = flow {
        if (!file.exists()) {
            emit(false)
            return@flow
        }
        val group = groupName?.let { MediaGroupBean(name = it) } ?: MediaGroupBean.DefaultMediaGroup

        val realGroupName = if (group.isDefaultGroup()) null else group.name
        val realArticleId = articleId.takeIf { !it.isNullOrBlank() }
        val article = realArticleId?.let { articleDao.getArticleWithFeed(it).first() }
        val articleLink = article?.articleWithEnclosure?.article?.link
        val articleGuid = article?.articleWithEnclosure?.article?.guid
        val feedUrl = article?.feed?.url
        val realDisplayName = displayName.takeIf { !it.isNullOrBlank() }
            ?: article?.articleWithEnclosure?.article?.title

        val path = file.parent!!.toString()
        var mediaLibJson = getOrReadMediaLibJson(path = path)
        if (realGroupName != null && !mediaLibJson.allGroups.contains(realGroupName)) {
            createGroup(path, group).first()
            mediaLibJson = getOrReadMediaLibJson(path = path)
        }
        val index = mediaLibJson.files.indexOfFirst { it.fileName == file.name }
        if (index >= 0) {
            mediaLibJson.files[index].groupName = realGroupName
            mediaLibJson.files[index].articleId = realArticleId
            mediaLibJson.files[index].displayName = realDisplayName
            mediaLibJson.files[index].articleLink = articleLink
            mediaLibJson.files[index].articleGuid = articleGuid
            mediaLibJson.files[index].feedUrl = feedUrl
        } else {
            mediaLibJson.files.add(
                FileJson(
                    fileName = file.name,
                    groupName = realGroupName,
                    isFile = file.isFile,
                    displayName = realDisplayName,
                    articleId = realArticleId,
                    articleLink = articleLink,
                    articleGuid = articleGuid,
                    feedUrl = feedUrl,
                )
            )
        }
        writeMediaLibJson(path = path, mediaLibJson)

        emit(true)
    }.flowOn(Dispatchers.IO)

    override fun getFolder(
        parentFile: Path,
        groupName: String?,
        feedUrl: String?,
        displayName: String?,
    ): Flow<Path> = flow {
        val path = parentFile.toString()
        val mediaLibJson = getOrReadMediaLibJson(path = path)
        val existed = mediaLibJson.files.firstOrNull {
            it.feedUrl == feedUrl && !it.isFile
        }
        if (existed != null) {
            emit(Path(parentFile, existed.fileName))
            return@flow
        }
        val newFolder = Path(
            parentFile,
            "${displayName?.validateFileName(100)} - ${Clock.currentTimeMillis()}"
        )
        createFolder(
            file = newFolder,
            groupName = groupName,
            feedUrl = feedUrl,
            displayName = displayName,
        ).first()
        emit(newFolder)
    }.flowOn(Dispatchers.IO)

    private fun createFolder(
        file: Path,
        groupName: String?,
        feedUrl: String?,
        displayName: String?,
    ): Flow<Boolean> = flow {
        if (file.exists() || !file.createDirectories()) {
            emit(false)
            return@flow
        }
        val group = groupName?.let { MediaGroupBean(name = it) } ?: MediaGroupBean.DefaultMediaGroup

        val realGroupName = if (group.isDefaultGroup()) null else group.name
        val realFeedUrl = feedUrl.takeIf { !it.isNullOrBlank() }
        val feed = realFeedUrl?.let { feedDao.getFeedView(it) }
        val realDisplayName = displayName.takeIf { !it.isNullOrBlank() } ?: feed?.feed?.title

        val path = file.parent!!.toString()
        var mediaLibJson = getOrReadMediaLibJson(path = path)
        if (realGroupName != null && !mediaLibJson.allGroups.contains(realGroupName)) {
            createGroup(path, group).first()
            mediaLibJson = getOrReadMediaLibJson(path = path)
        }
        val index = mediaLibJson.files.indexOfFirst { it.fileName == file.name && !it.isFile }
        if (index >= 0) {
            mediaLibJson.files[index].groupName = realGroupName
            mediaLibJson.files[index].feedUrl = realFeedUrl
            mediaLibJson.files[index].displayName = realDisplayName
        } else {
            mediaLibJson.files.add(
                FileJson(
                    fileName = file.name,
                    groupName = realGroupName,
                    isFile = file.isFile,
                    displayName = realDisplayName,
                    articleId = null,
                    feedUrl = realFeedUrl,
                    articleLink = null,
                    articleGuid = null,
                )
            )
        }
        writeMediaLibJson(path = path, mediaLibJson)

        emit(true)
    }.flowOn(Dispatchers.IO)

    fun createGroup(path: String, group: MediaGroupBean): Flow<Unit> = flow {
        if (group.isDefaultGroup()) {
            emit(Unit)
            return@flow
        }
        val mediaLibJson = getOrReadMediaLibJson(path = path)
        if (mediaLibJson.allGroups.contains(group.name)) {
            emit(Unit)
            return@flow
        }
        mediaLibJson.allGroups.add(group.name)
        writeMediaLibJson(path = path, mediaLibJson)

        emit(Unit)
    }.flowOn(Dispatchers.IO)

    fun deleteGroup(path: String, group: MediaGroupBean): Flow<Unit> = flow {
        if (group.isDefaultGroup()) {
            emit(Unit)
            return@flow
        }
        val mediaLibJson = getOrReadMediaLibJson(path = path)
        mediaLibJson.files.forEach {
            if (it.groupName == group.name) {
                it.groupName = null
            }
        }
        mediaLibJson.allGroups.remove(group.name)
        writeMediaLibJson(path = path, mediaLibJson)

        emit(Unit)
    }.flowOn(Dispatchers.IO)

    fun renameGroup(
        path: String,
        group: MediaGroupBean,
        newName: String,
    ): Flow<MediaGroupBean> = flow {
        if (group.isDefaultGroup()) {
            emit(MediaGroupBean.DefaultMediaGroup)
            return@flow
        }
        val mediaLibJson = getOrReadMediaLibJson(path = path)
        mediaLibJson.files.forEach {
            if (it.groupName == group.name) {
                it.groupName = newName
            }
        }
        val index = mediaLibJson.allGroups.indexOf(group.name)
        if (index >= 0) {
            mediaLibJson.allGroups[index] = newName
        }
        writeMediaLibJson(path = path, mediaLibJson)

        emit(MediaGroupBean(name = newName))
    }.flowOn(Dispatchers.IO)

    fun changeMediaGroup(
        path: String,
        mediaBean: MediaBean,
        group: MediaGroupBean,
    ): Flow<Unit> = flow {
        var mediaLibJson = getOrReadMediaLibJson(path = path)
        if (!group.isDefaultGroup() && !mediaLibJson.allGroups.contains(group.name)) {
            createGroup(path, group).first()
            mediaLibJson = getOrReadMediaLibJson(path = path)
        }
        val index = mediaLibJson.files.indexOfFirst { it.fileName == mediaBean.path.name }
        if (index >= 0) {
            mediaLibJson.files[index].groupName = if (group.isDefaultGroup()) null else group.name
        } else {
            if (!group.isDefaultGroup()) {
                mediaLibJson.files.add(
                    FileJson(
                        fileName = mediaBean.path.name,
                        groupName = group.name,
                        isFile = mediaBean.path.isFile,
                        displayName = mediaBean.displayName,
                        articleId = mediaBean.articleId,
                        articleLink = mediaBean.articleWithEnclosure?.article?.link,
                        articleGuid = mediaBean.articleWithEnclosure?.article?.guid,
                        feedUrl = mediaBean.feedBean?.url,
                    )
                )
            }
        }
        writeMediaLibJson(path = path, mediaLibJson)

        emit(Unit)
    }.flowOn(Dispatchers.IO)

    fun moveFilesToGroup(
        path: String,
        from: MediaGroupBean,
        to: MediaGroupBean
    ): Flow<Unit> = flow {
        var mediaLibJson = getOrReadMediaLibJson(path = path)
        if (!to.isDefaultGroup() && !mediaLibJson.allGroups.contains(to.name)) {
            createGroup(path, to).first()
            mediaLibJson = getOrReadMediaLibJson(path = path)
        }
        if (from.isDefaultGroup()) {
            if (to.isDefaultGroup()) {
                emit(Unit)
                return@flow
            } else {
                mediaLibJson.files.appendFiles(
                    files = Path(path).list().toList(),
                    fileJsonBuild = {
                        FileJson(
                            fileName = it.name,
                            groupName = to.name,
                            isFile = it.isFile,
                            displayName = null,
                            articleId = null,
                            articleLink = null,
                            articleGuid = null,
                            feedUrl = null,
                        )
                    }
                )
                mediaLibJson.files.forEach {
                    if (it.groupName == null) it.groupName = to.name
                }
            }
        } else {
            mediaLibJson.files.forEach {
                if (it.groupName == from.name) {
                    it.groupName = if (to.isDefaultGroup()) null else to.name
                }
            }
        }
        writeMediaLibJson(path = path, mediaLibJson)

        emit(Unit)
    }.flowOn(Dispatchers.IO)

    @Serializable
    data class MediaLibJson(
        @SerialName("allGroups")
        @EncodeDefault
        val allGroups: MutableList<String> = mutableListOf(),
        @SerialName("files")
        val files: MutableList<FileJson>,
    )

    @Serializable
    data class FileJson(
        @SerialName("fileName")
        var fileName: String,
        @SerialName("groupName")
        var groupName: String? = null,
        @SerialName("isFile")
        var isFile: Boolean = false,
        @SerialName("displayName")
        var displayName: String? = null,
        @SerialName("articleId")
        var articleId: String? = null,
        @SerialName("articleLink")
        var articleLink: String? = null,
        @SerialName("articleGuid")
        var articleGuid: String? = null,
        @SerialName("feedUrl")
        var feedUrl: String? = null,
    )

    @Serializable
    data class FolderInfo(
        @SerialName("displayName")
        val displayName: String? = null,
    )
}
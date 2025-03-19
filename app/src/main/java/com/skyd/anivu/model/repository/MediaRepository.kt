package com.skyd.anivu.model.repository

import androidx.collection.LruCache
import androidx.compose.ui.util.fastFirstOrNull
import com.skyd.anivu.appContext
import com.skyd.anivu.base.BaseRepository
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.flowOf
import com.skyd.anivu.ext.splitByBlank
import com.skyd.anivu.ext.validateFileName
import com.skyd.anivu.model.bean.MediaBean
import com.skyd.anivu.model.bean.MediaGroupBean
import com.skyd.anivu.model.bean.MediaGroupBean.Companion.isDefaultGroup
import com.skyd.anivu.model.bean.article.ArticleWithEnclosureBean
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.FeedDao
import com.skyd.anivu.model.preference.appearance.media.MediaFileFilterPreference
import com.skyd.anivu.model.preference.behavior.media.BaseMediaListSortByPreference
import com.skyd.anivu.model.preference.behavior.media.MediaListSortAscPreference
import com.skyd.anivu.model.preference.behavior.media.MediaListSortByPreference
import com.skyd.anivu.model.preference.behavior.media.MediaSubListSortAscPreference
import com.skyd.anivu.model.preference.behavior.media.MediaSubListSortByPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import javax.inject.Inject

class MediaRepository @Inject constructor(
    private val json: Json,
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
) : BaseRepository() {

    companion object {
        const val FOLDER_INFO_JSON_NAME = "info.json"
        const val OLD_MEDIA_LIB_JSON_NAME = "group.json"
        const val MEDIA_LIB_JSON_NAME = "MediaLib.json"

        private val mediaLibJsons = LruCache<String, MediaLibJson>(maxSize = 10)

        private val refreshPath = MutableSharedFlow<String>(extraBufferCapacity = Int.MAX_VALUE)
    }

    private fun parseMediaLibJson(mediaLibRootJsonFile: File): MediaLibJson? {
        if (!mediaLibRootJsonFile.exists()) {
            File(mediaLibRootJsonFile.parentFile, OLD_MEDIA_LIB_JSON_NAME).apply {
                if (this.exists()) renameTo(mediaLibRootJsonFile)
            }
        }
        if (!mediaLibRootJsonFile.exists()) return null
        return mediaLibRootJsonFile.inputStream().use { inputStream ->
            json.decodeFromStream<MediaLibJson>(inputStream)
        }.apply {
            files.removeIf {
                !File(mediaLibRootJsonFile.parentFile, it.fileName).exists() ||
                        it.fileName.equals(FOLDER_INFO_JSON_NAME, true) ||
                        it.fileName.equals(MEDIA_LIB_JSON_NAME, true)
            }
        }
    }

    private suspend fun getOrReadMediaLibJson(path: String): MediaLibJson {
        val existFiles: (String) -> List<File> = { p ->
            File(p).listFiles().orEmpty().toMutableList().filter { it.exists() }
        }
        val existMediaLibJson = mediaLibJsons[path]
        if (existMediaLibJson != null) {
            existMediaLibJson.files.appendFiles(existFiles(path))
            return existMediaLibJson
        }

        val newMediaLibJson = parseMediaLibJson(File(path, MEDIA_LIB_JSON_NAME))
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
        File(path, MEDIA_LIB_JSON_NAME).apply {
            if (!exists()) {
                getParentFile()?.mkdirs()
                createNewFile()
            }
        }.outputStream().use { outputStream ->
            json.encodeToStream(formatMediaLibJson(data), outputStream)
        }
        refreshPath.emit(path)
    }

    private fun MutableList<FileJson>.appendFiles(
        files: List<File>,
        fileJsonBuild: (File) -> FileJson = {
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
    ) = apply {
        if (files.isEmpty()) return@apply
        removeIf { !File(files[0].parentFile, it.fileName).exists() }
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
                    val newArticleId = articleDao.queryArticleByGuid(
                        json.articleGuid, feedUrl
                    )?.articleId ?: articleDao.queryArticleByLink(
                        json.articleLink, feedUrl
                    )?.articleId

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
        val file = File(path, fileName)
        if (!file.exists()) return null
        val fileCount = if (file.isDirectory) {
            runCatching { file.list()?.size }.getOrNull()?.run {
                this - listOf(
                    File(file, MEDIA_LIB_JSON_NAME).exists(),
                    File(file, FOLDER_INFO_JSON_NAME).exists(),
                ).count { it }
            } ?: 0
        } else 0

        return MediaBean(
            displayName = displayName,
            file = file,
            fileCount = fileCount,
            articleWithEnclosure = articleWithEnclosure,
            feedBean = feedBean,
        )
    }

    fun requestGroups(path: String): Flow<List<MediaGroupBean>> = merge(
        flowOf(path), refreshFiles, refreshPath
    ).filter { it == path }.map {
        val allGroups = getOrReadMediaLibJson(path).allGroups
        listOf(MediaGroupBean.DefaultMediaGroup) +
                allGroups.map { MediaGroupBean(name = it) }.sortedBy { it.name }
    }.flowOn(Dispatchers.IO)

    private val refreshFiles =
        MutableSharedFlow<String>(replay = 1, extraBufferCapacity = Int.MAX_VALUE)

    suspend fun refreshFiles(path: String) {
        return refreshFiles.emit(path)
    }

    fun requestFiles(
        path: String,
        group: MediaGroupBean?,
        isSubList: Boolean = false,
    ): Flow<List<MediaBean>> = combine(
        merge(flowOf(path), refreshFiles, refreshPath).filter { it == path }.map {
            val mediaLibJson = getOrReadMediaLibJson(path)
            val fileJsons = mediaLibJson.files
            val videoList = (if (group == null) fileJsons else {
                val groupName = if (group.isDefaultGroup()) null else group.name
                fileJsons.filter { it.groupName == groupName }
            }).let { jsons ->
                val articleMap = articleDao.getArticleListByIds(
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
        appContext.dataStore.flowOf(MediaFileFilterPreference),
    ) { videoList, displayFilter ->
        videoList.filter {
            runCatching { it.file.name.matches(Regex(displayFilter)) }.getOrNull() == true
        }
    }.combine(
        appContext.dataStore.flowOf(
            if (isSubList) MediaSubListSortByPreference else MediaListSortByPreference
        )
    ) { list, sortBy ->
        when (sortBy) {
            BaseMediaListSortByPreference.DATE -> list.sortedBy { it.date }
            BaseMediaListSortByPreference.NAME -> list.sortedBy { it.displayName ?: it.name }
            BaseMediaListSortByPreference.FILE_COUNT -> list.sortedBy { it.fileCount }
            else -> list.sortedBy { it.displayName ?: it.name }
        }
    }.combine(
        appContext.dataStore.flowOf(
            if (isSubList) MediaSubListSortAscPreference else MediaListSortAscPreference
        )
    ) { list, sortAsc ->
        if (sortAsc) list else list.reversed()
    }.flowOn(Dispatchers.IO)

    fun search(
        path: String,
        query: String,
        recursive: Boolean = false,
    ): Flow<List<MediaBean>> = flowOf(
        query.trim() to recursive
    ).flatMapLatest { (query, recursive) ->
        merge(
            flowOf(path), refreshFiles, refreshPath
        ).debounce(70).filter { it == path }.map {
            val queries = query.splitByBlank()

            val fileJsons = mutableListOf<FileJson>()
            File(path).walkBottomUp().onEnter { dir ->
                dir.path == path || recursive
            }.filter { it.isDirectory }.asFlow().map {
                val mediaLibJson = getOrReadMediaLibJson(it.path)
                fileJsons += mediaLibJson.files
            }.collect()

            val articleMap = articleDao.getArticleListByIds(
                fileJsons.mapNotNull { it.articleId }
            ).associateBy { it.articleWithEnclosure.article.articleId }
            val feedMap = feedDao.getFeedsIn(
                fileJsons.mapNotNull { it.feedUrl }
            ).associateBy { it.feed.url }

            fileJsons.filter { file ->
                queries.any {
                    it in file.fileName ||
                            it in file.displayName.orEmpty() ||
                            it in file.feedUrl.orEmpty() ||
                            it in file.articleLink.orEmpty()
                }
            }.mapNotNull { fileJson ->
                fileJson.toMediaBean(
                    path = path,
                    articleWithEnclosure = articleMap[fileJson.articleId]?.articleWithEnclosure,
                    feedBean = feedMap[fileJson.feedUrl]?.feed
                        ?: articleMap[fileJson.articleId]?.feed,
                )
            }
        }
    }.flowOn(Dispatchers.IO)

    fun deleteFile(file: File): Flow<Boolean> = flow {
        val path = file.parentFile!!.path
        val mediaLibJson = getOrReadMediaLibJson(path).apply {
            files.removeIf { it.fileName == file.name }
        }
        writeMediaLibJson(path = path, mediaLibJson)
        emit(file.deleteRecursively())
    }.flowOn(Dispatchers.IO)

    fun renameFile(file: File, newName: String): Flow<File?> = flow {
        val path = file.parentFile!!.path
        val mediaLibJson = getOrReadMediaLibJson(path)
        val validateFileName = newName.validateFileName()
        val newFile = File(file.parentFile, validateFileName)
        if (file.renameTo(newFile)) {
            mediaLibJson.files.firstOrNull { it.fileName == file.name }?.fileName =
                validateFileName
            writeMediaLibJson(path = path, mediaLibJson)
            emit(newFile)
        } else {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    fun setFileDisplayName(mediaBean: MediaBean, displayName: String?): Flow<MediaBean> = flow {
        val path = mediaBean.file.parentFile!!.path
        val mediaLibJson = getOrReadMediaLibJson(path = path)
        mediaLibJson.files.firstOrNull {
            it.fileName == mediaBean.file.name
        }?.displayName = if (displayName.isNullOrBlank()) null else displayName
        writeMediaLibJson(path = path, mediaLibJson)

        emit(mediaBean.copy(displayName = displayName))
    }.flowOn(Dispatchers.IO)

    fun addNewFile(
        file: File,
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

        val path = file.parentFile!!.path
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

    fun getFolder(
        parentFile: File,
        groupName: String?,
        feedUrl: String?,
        displayName: String?,
    ): Flow<File> = flow {
        val path = parentFile.path
        val mediaLibJson = getOrReadMediaLibJson(path = path)
        val existed = mediaLibJson.files.firstOrNull {
            it.feedUrl == feedUrl && !it.isFile
        }
        if (existed != null) {
            emit(File(parentFile, existed.fileName))
            return@flow
        }
        val newFolder = File(
            parentFile,
            "${displayName?.validateFileName(100)} - ${System.currentTimeMillis()}"
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
        file: File,
        groupName: String?,
        feedUrl: String?,
        displayName: String?,
    ): Flow<Boolean> = flow {
        if (file.exists() || !file.mkdirs()) {
            emit(false)
            return@flow
        }
        val group = groupName?.let { MediaGroupBean(name = it) } ?: MediaGroupBean.DefaultMediaGroup

        val realGroupName = if (group.isDefaultGroup()) null else group.name
        val realFeedUrl = feedUrl.takeIf { !it.isNullOrBlank() }
        val feed = realFeedUrl?.let { feedDao.getFeed(it) }
        val realDisplayName = displayName.takeIf { !it.isNullOrBlank() } ?: feed?.feed?.title

        val path = file.parentFile!!.path
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
        val index = mediaLibJson.files.indexOfFirst { it.fileName == mediaBean.file.name }
        if (index >= 0) {
            mediaLibJson.files[index].groupName = if (group.isDefaultGroup()) null else group.name
        } else {
            if (!group.isDefaultGroup()) {
                mediaLibJson.files.add(
                    FileJson(
                        fileName = mediaBean.file.name,
                        groupName = group.name,
                        isFile = mediaBean.file.isFile,
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
                    files = File(path).listFiles().orEmpty().toList(),
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
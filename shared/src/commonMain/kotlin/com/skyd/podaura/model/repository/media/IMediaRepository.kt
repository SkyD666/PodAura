package com.skyd.podaura.model.repository.media

import com.skyd.podaura.model.bean.MediaBean
import com.skyd.podaura.model.bean.MediaGroupBean
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow

interface IMediaRepository {
    fun requestGroups(path: String): Flow<List<MediaGroupBean>>

    suspend fun refreshFiles(path: String)

    fun requestFiles(
        path: String,
        group: MediaGroupBean?,
        isSubList: Boolean = false,
    ): Flow<List<MediaBean>>

    fun search(
        path: String,
        query: String,
        recursive: Boolean = false,
    ): Flow<List<MediaBean>>

    fun deleteFile(media: MediaBean): Flow<Boolean>

    fun renameFile(media: MediaBean, newName: String): Flow<PlatformFile?>

    fun setDisplayName(mediaBean: MediaBean, displayName: String?): Flow<MediaBean>

    fun addNewFile(
        file: PlatformFile,
        parent: PlatformFile,
        groupName: String?,
        articleId: String?,
        displayName: String?,
    ): Flow<Boolean>

    fun getFolder(
        parentFile: PlatformFile,
        groupName: String?,
        feedUrl: String?,
        displayName: String?,
    ): Flow<PlatformFile>
}

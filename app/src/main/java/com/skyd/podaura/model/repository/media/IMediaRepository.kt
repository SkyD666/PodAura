package com.skyd.podaura.model.repository.media

import com.skyd.podaura.model.bean.MediaBean
import com.skyd.podaura.model.bean.MediaGroupBean
import kotlinx.coroutines.flow.Flow
import kotlinx.io.files.Path

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

    fun deleteFile(file: Path): Flow<Boolean>

    fun renameFile(file: Path, newName: String): Flow<Path?>

    fun setDisplayName(mediaBean: MediaBean, displayName: String?): Flow<MediaBean>

    fun addNewFile(
        file: Path,
        groupName: String?,
        articleId: String?,
        displayName: String?,
    ): Flow<Boolean>

    fun getFolder(
        parentFile: Path,
        groupName: String?,
        feedUrl: String?,
        displayName: String?,
    ): Flow<Path>
}
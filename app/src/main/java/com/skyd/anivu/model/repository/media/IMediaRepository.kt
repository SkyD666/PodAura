package com.skyd.anivu.model.repository.media

import com.skyd.anivu.model.bean.MediaBean
import com.skyd.anivu.model.bean.MediaGroupBean
import kotlinx.coroutines.flow.Flow
import java.io.File

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

    fun deleteFile(file: File): Flow<Boolean>

    fun renameFile(file: File, newName: String): Flow<File?>

    fun setDisplayName(mediaBean: MediaBean, displayName: String?): Flow<MediaBean>

    fun addNewFile(
        file: File,
        groupName: String?,
        articleId: String?,
        displayName: String?,
    ): Flow<Boolean>

    fun getFolder(
        parentFile: File,
        groupName: String?,
        feedUrl: String?,
        displayName: String?,
    ): Flow<File>
}
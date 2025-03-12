package com.skyd.anivu.model.repository

import com.skyd.anivu.base.BaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class FilePickerRepository @Inject constructor() : BaseRepository() {
    fun requestFiles(
        path: String,
        extensionName: String? = null,
    ): Flow<List<File>> = flow {
        val filter: (File) -> Boolean = {
            it.isDirectory || extensionName.isNullOrBlank() || it.extension == extensionName
        }
        File(path).listFiles()
            .orEmpty()
            .toList()
            .filter(filter)
            .sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            .let { emit(it) }
    }.flowOn(Dispatchers.IO)
}
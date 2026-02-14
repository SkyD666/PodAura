package com.skyd.podaura.model.repository

import com.skyd.fundation.ext.extension
import com.skyd.fundation.ext.isDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class FilePickerRepository : BaseRepository() {
    fun requestFiles(
        path: String,
        extensionName: String? = null,
    ): Flow<List<Path>> = flow {
        val filter: (Path) -> Boolean = {
            it.isDirectory || extensionName.isNullOrBlank() || it.extension == extensionName
        }
        SystemFileSystem.list(kotlinx.io.files.Path(path))
            .filter(filter)
            .sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            .let { emit(it) }
    }.flowOn(Dispatchers.IO)
}

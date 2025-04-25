package com.skyd.anivu.model.repository

import com.skyd.anivu.ext.extension
import com.skyd.anivu.ext.isDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.koin.core.annotation.Factory

@Factory(binds = [])
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
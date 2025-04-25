package com.skyd.anivu.ext

import co.touchlab.kermit.Logger
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

expect val Path.lastModifiedTime: Long?

val Path.isDirectory: Boolean
    get() = SystemFileSystem.metadataOrNull(this)?.isDirectory == true

val Path.isFile: Boolean
    get() = SystemFileSystem.metadataOrNull(this)?.isRegularFile == true

val Path.nameWithoutExtension: String
    get() = name.substringBeforeLast(".")

val Path.extension: String
    get() = name.substringAfterLast('.', "")

fun Path.list(): Collection<Path> = SystemFileSystem.list(this)

fun Path.exists(): Boolean = SystemFileSystem.exists(this)

fun Path.createDirectories(mustCreate: Boolean = false): Boolean = runCatching {
    SystemFileSystem.createDirectories(this, mustCreate)
}.onFailure { Logger.e(it.message.orEmpty()) }.getOrNull() != null

fun Path.atomicMove(destination: Path): Boolean = runCatching {
    SystemFileSystem.atomicMove(this, destination)
}.onFailure { Logger.e(it.message.orEmpty()) }.getOrNull() != null

fun Path.rename(name: String): Boolean = runCatching {
    SystemFileSystem.atomicMove(this, parent?.let { Path(it, name) } ?: Path(name))
}.onFailure { Logger.e(it.message.orEmpty()) }.getOrNull() != null

fun Path.source(): Source = SystemFileSystem.source(this).buffered()

fun Path.sink(append: Boolean = false): Sink = SystemFileSystem.sink(this, append).buffered()

fun Path.deleteRecursively(mustExist: Boolean = true) = runCatching {
    SystemFileSystem.deleteRecursively(this, mustExist)
}.onFailure { Logger.e(it.message.orEmpty()) }.getOrNull() != null

fun FileSystem.deleteRecursively(path: Path, mustExist: Boolean = true) {
    if (mustExist && !path.exists()) throw FileNotFoundException("File does not exist: $path")

    val queue = ArrayDeque<Path>()
    queue.add(path)

    while (queue.isNotEmpty()) {
        val currentPath = queue.first()
        val metadata = metadataOrNull(currentPath)
            ?: throw IOException("Can not get the metadata")
        when {
            metadata.isRegularFile -> {
                delete(currentPath)
                queue.removeFirst()
            }

            metadata.isDirectory -> {
                val list = list(currentPath)
                if (list.isEmpty()) {
                    delete(currentPath)
                    queue.removeFirst()
                } else queue.addAll(0, list)
            }

            else -> throw IOException("Path is neither a file nor a directory: $path")
        }
    }
}

fun Path.walk(
    option: PathWalkOption = PathWalkOption.Default,
    onEnter: (Path) -> Boolean = { true },
): Sequence<Path> = SystemFileSystem.walk(this, option, onEnter)

fun FileSystem.walk(
    path: Path,
    option: PathWalkOption = PathWalkOption.Default,
    onEnter: (Path) -> Boolean = { true },
): Sequence<Path> =
    sequence {
        if (!path.exists()) return@sequence
        val queue = ArrayDeque<Path>()
        queue.add(path)

        if (option.breadthFirst) {
            while (queue.isNotEmpty()) {
                val currentPath = queue.removeFirst()
                val metadata = metadataOrNull(currentPath)
                    ?: throw IOException("Can not get the metadata")
                when {
                    metadata.isRegularFile -> yield(currentPath)
                    metadata.isDirectory -> {
                        if (onEnter(currentPath)) {
                            queue.addAll(list(currentPath))
                        }
                        yield(currentPath)
                    }

                    else -> throw IOException("Path is neither a file nor a directory: $path")
                }
            }
        } else {
            while (queue.isNotEmpty()) {
                val currentPath = queue.removeFirst()
                val metadata = metadataOrNull(currentPath)
                    ?: throw IOException("Can not get the metadata")
                when {
                    metadata.isRegularFile -> yield(currentPath)
                    metadata.isDirectory -> {
                        if (onEnter(currentPath)) {
                            queue.addAll(0, list(currentPath))
                        }
                        yield(currentPath)
                    }

                    else -> throw IOException("Path is neither a file nor a directory: $path")
                }
            }
        }
    }

class PathWalkOption internal constructor(val breadthFirst: Boolean) {
    companion object {
        val Default = PathWalkOption(false)
        val BreadthFirst = PathWalkOption(true)
    }
}
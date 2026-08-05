package com.skyd.podaura.model.repository

import androidx.compose.ui.platform.Clipboard
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.TEMP_PICTURES_DIR
import com.skyd.fundation.ext.PathWalkOption
import com.skyd.fundation.ext.currentTimeMillis
import com.skyd.fundation.ext.deleteRecursively
import com.skyd.fundation.ext.isFile
import com.skyd.fundation.ext.lastModifiedTime
import com.skyd.fundation.ext.size
import com.skyd.fundation.ext.source
import com.skyd.fundation.ext.walk
import com.skyd.podaura.ext.getImage
import com.skyd.podaura.ext.platformContext
import com.skyd.podaura.ext.setImage
import com.skyd.podaura.ext.validateFileName
import com.skyd.podaura.ui.component.imageLoaderBuilder
import com.skyd.podaura.util.image.ImageFormatChecker
import com.skyd.podaura.util.image.format.ImageFormat
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.saveImageToGallery
import io.github.vinceglb.filekit.utils.div
import io.github.vinceglb.filekit.utils.toPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.io.files.Path
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class ImageRepository : BaseRepository() {
    fun downloadImage(url: String, title: String?): Flow<Unit> = flow {
        val (imageFile, format) = loadImage(url)
        FileKit.saveImageToGallery(
            file = PlatformFile(imageFile),
            filename = ("${
                title.orEmpty().ifEmpty { url.substringAfterLast('/') }
            }_${Random.nextInt()}").validateFileName() + format.toString(),
        )
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    fun shareImage(url: String): Flow<Boolean> = flow {
        val (imageFile, format) = loadImage(url)
        emit(shareImage(createTempImage(imageFile, format)))
    }.flowOn(Dispatchers.IO)

    fun copyImage(url: String, clipboard: Clipboard): Flow<Unit> = flow {
        val (imageFile, format) = loadImage(url)
        clipboard.setImage(
            file = createTempImage(imageFile, format),
            mimeType = format.toMimeType(),
        )
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    private suspend fun loadImage(url: String): Pair<Path, ImageFormat> {
        val imageFile = platformContext().imageLoaderBuilder().build().getImage(url = url)!!
        val format = imageFile.source().use { ImageFormatChecker.check(it) }
        return imageFile to format
    }

    private suspend fun createTempImage(imageFile: Path, format: ImageFormat): PlatformFile {
        val tempImg =
            PlatformFile(Const.TEMP_PICTURES_DIR.toPath() / (imageFile.name + format.toString()))
        PlatformFile(imageFile).copyTo(tempImg)
        coroutineScope { deleteOldTempFiles(currentFile = imageFile) }
        return tempImg
    }

    private fun deleteOldTempFiles(currentFile: Path) {
        val nowTime = Clock.currentTimeMillis().milliseconds
        Path(Const.TEMP_PICTURES_DIR).deleteDirs { file ->
            file.name == currentFile.name || file.toString() == Const.TEMP_PICTURES_DIR ||
                    (nowTime - (file.lastModifiedTime ?: 0).milliseconds) < 1.hours
        }
    }

    private fun Path.deleteDirs(
        maxSize: Int = 5_242_880,
        exclude: (file: Path) -> Boolean,
    ) {
        if (walk(PathWalkOption.BreadthFirst).filter { it.isFile }.sumOf { it.size } > maxSize) {
            walk(PathWalkOption.Default).forEach { if (!exclude(it)) it.deleteRecursively() }
        }
    }
}

expect suspend fun shareImage(file: PlatformFile): Boolean

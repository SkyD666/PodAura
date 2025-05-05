package com.skyd.podaura.model.repository

import androidx.compose.ui.platform.Clipboard
import com.skyd.podaura.config.Const
import com.skyd.podaura.config.TEMP_PICTURES_DIR
import com.skyd.podaura.ext.PathWalkOption
import com.skyd.podaura.ext.currentTimeMillis
import com.skyd.podaura.ext.deleteRecursively
import com.skyd.podaura.ext.getImage
import com.skyd.podaura.ext.isFile
import com.skyd.podaura.ext.lastModifiedTime
import com.skyd.podaura.ext.platformContext
import com.skyd.podaura.ext.setImage
import com.skyd.podaura.ext.size
import com.skyd.podaura.ext.source
import com.skyd.podaura.ext.validateFileName
import com.skyd.podaura.ext.walk
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.history.ReadHistoryBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.ReadHistoryDao
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import kotlinx.io.files.Path
import org.koin.core.annotation.Factory
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

@Factory(binds = [])
class ReadRepository(
    private val articleDao: ArticleDao,
    private val readHistoryDao: ReadHistoryDao,
) : BaseRepository() {
    fun requestArticleWithFeed(
        articleId: String,
    ): Flow<ArticleWithFeed?> = articleDao.getArticleWithFeed(articleId = articleId)
        .filterNotNull()
        .onEach {
            readHistoryDao.updateReadHistory(
                ReadHistoryBean(
                    articleId = articleId,
                    lastTime = System.currentTimeMillis(),
                )
            )
        }
        .flowOn(Dispatchers.IO)

    fun downloadImage(url: String, title: String?): Flow<Unit> = flow {
        val (imageFile, format) = downloadImage(url)
        FileKit.saveImageToGallery(
            file = PlatformFile(imageFile),
            filename = ("${
                title.orEmpty().ifEmpty { url.substringAfterLast('/') }
            }_${Random.Default.nextInt()}").validateFileName() + format.toString(),
        )
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    fun shareImage(url: String): Flow<Boolean> = flow {
        val (imageFile, format) = downloadImage(url)
        val tempImg = createTempImage(imageFile, format)

        emit(shareImage(tempImg))
    }.flowOn(Dispatchers.IO)

    fun copyImage(url: String, clipboard: Clipboard): Flow<Unit> = flow {
        val (imageFile, format) = downloadImage(url)
        val tempImg = createTempImage(imageFile, format)
        clipboard.setImage(file = tempImg, mimeType = format.toMimeType())
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    private suspend fun downloadImage(url: String): Pair<Path, ImageFormat> {
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
        val nowTime = Clock.Companion.currentTimeMillis().milliseconds
        kotlinx.io.files.Path(Const.TEMP_PICTURES_DIR).deleteDirs { file ->
            file.name == currentFile.name || file.toString() == Const.TEMP_PICTURES_DIR ||
                    (nowTime - (file.lastModifiedTime ?: 0).milliseconds) < 1.hours
        }
    }

    fun Path.deleteDirs(
        maxSize: Int = 5_242_880,
        exclude: (file: Path) -> Boolean,
    ) {
        if (walk(PathWalkOption.Companion.BreadthFirst).filter { it.isFile }.map { it.size }
                .sum() > maxSize) {
            walk(PathWalkOption.Companion.Default).forEach { if (!exclude(it)) it.deleteRecursively() }
        }
    }
}

expect suspend fun shareImage(file: PlatformFile): Boolean
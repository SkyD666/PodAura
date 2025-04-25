package com.skyd.anivu.model.repository

import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.skyd.anivu.appContext
import com.skyd.anivu.config.Const
import com.skyd.anivu.config.TEMP_PICTURES_DIR
import com.skyd.anivu.ext.copyToClipboard
import com.skyd.anivu.ext.deleteDirs
import com.skyd.anivu.ext.getImage
import com.skyd.anivu.ext.savePictureToMediaStore
import com.skyd.anivu.ext.share
import com.skyd.anivu.ext.toUri
import com.skyd.anivu.ext.validateFileName
import com.skyd.anivu.model.bean.article.ArticleWithFeed
import com.skyd.anivu.model.bean.history.ReadHistoryBean
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.ReadHistoryDao
import com.skyd.anivu.ui.component.imageLoaderBuilder
import com.skyd.anivu.util.image.ImageFormatChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Factory
import java.io.File
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
        val imageFile = appContext.imageLoaderBuilder().build().getImage(appContext, url)!!
        val format = imageFile.inputStream().use { ImageFormatChecker.check(it) }
        imageFile.savePictureToMediaStore(
            context = appContext,
            mimetype = format.toMimeType(),
            fileName = (title.orEmpty().ifEmpty {
                url.substringAfterLast('/')
            } + "_" + Random.nextInt()).validateFileName() + format.toString(),
            autoDelete = false,
        )
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    fun shareImage(url: String): Flow<Unit> = flow {
        val imageFile = getImageByCoil(url)
        val format = imageFile.inputStream().use { ImageFormatChecker.check(it) }
        val tempImg = File(Const.TEMP_PICTURES_DIR, imageFile.name + format.toString())
        imageFile.copyTo(tempImg, overwrite = true)

        coroutineScope { deleteOldTempFiles(currentFile = imageFile) }

        tempImg.toUri(appContext).share(appContext, mimeType = format.toMimeType())
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    fun copyImage(url: String): Flow<Unit> = flow {
        val imageFile = getImageByCoil(url)
        val format = imageFile.inputStream().use { ImageFormatChecker.check(it) }
        val tempImg = File(Const.TEMP_PICTURES_DIR, imageFile.name + format.toString())
        imageFile.copyTo(tempImg, overwrite = true)

        coroutineScope { deleteOldTempFiles(currentFile = imageFile) }

        tempImg.toUri(appContext).copyToClipboard(appContext, format.toMimeType())
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    private suspend fun getImageByCoil(url: String): File {
        val request = ImageRequest.Builder(appContext)
            .data(url)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        val imageLoader = appContext.imageLoaderBuilder().build()
        when (val result = imageLoader.execute(request)) {
            is ErrorResult -> throw result.throwable
            is SuccessResult -> {
                imageLoader.diskCache!!.openSnapshot(url).use { snapshot ->
                    return snapshot!!.data.toFile()
                }
            }
        }
    }

    private fun deleteOldTempFiles(currentFile: File) {
        val nowTime = System.currentTimeMillis().milliseconds
        File(Const.TEMP_PICTURES_DIR).deleteDirs { file ->
            file.name == currentFile.name || file == File(Const.TEMP_PICTURES_DIR) ||
                    nowTime - file.lastModified().milliseconds < 1.hours
        }
    }
}
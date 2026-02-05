package com.skyd.podaura.model.repository.player

import com.skyd.fundation.di.get
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.db.dao.MediaPlayHistoryDao
import com.skyd.podaura.ui.player.resolveUri
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile

actual class PlayerRepository actual constructor(
    mediaPlayHistoryDao: MediaPlayHistoryDao,
    articleDao: ArticleDao,
    enclosureDao: EnclosureDao
) : BasePlayerRepository(mediaPlayHistoryDao, articleDao, enclosureDao) {
    actual override fun requestPlaylistByPlatformFile(file: PlatformFile): List<PlaylistMediaWithArticleBean>? {
        return when (val androidFile = file.androidFile) {
            is AndroidFile.UriWrapper -> {
                androidFile.uri.resolveUri(get())?.let { path ->
                    listOf(
                        PlaylistMediaWithArticleBean.fromUrl(
                            playlistId = "",
                            url = path,
                            orderPosition = 1.0,
                        )
                    )
                }
            }

            is AndroidFile.FileWrapper -> listOf(
                PlaylistMediaWithArticleBean.fromUrl(
                    playlistId = "",
                    url = androidFile.file.path,
                    orderPosition = 1.0,
                )
            )
        }
    }
}

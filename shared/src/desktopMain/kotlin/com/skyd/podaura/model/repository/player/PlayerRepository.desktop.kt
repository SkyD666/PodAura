package com.skyd.podaura.model.repository.player

import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.db.dao.MediaPlayHistoryDao
import io.github.vinceglb.filekit.PlatformFile

actual class PlayerRepository actual constructor(
    mediaPlayHistoryDao: MediaPlayHistoryDao,
    articleDao: ArticleDao,
    enclosureDao: EnclosureDao
) : BasePlayerRepository(mediaPlayHistoryDao, articleDao, enclosureDao) {
    override fun requestPlaylistByPlatformFile(file: PlatformFile): List<PlaylistMediaWithArticleBean>? {
        return listOf(
            PlaylistMediaWithArticleBean.Companion.fromUrl(
                playlistId = "",
                url = file.file.path,
                orderPosition = 1.0,
            )
        )
    }
}
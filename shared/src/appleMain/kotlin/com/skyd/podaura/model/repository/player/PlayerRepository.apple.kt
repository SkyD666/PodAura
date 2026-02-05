package com.skyd.podaura.model.repository.player

import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.db.dao.MediaPlayHistoryDao
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

actual class PlayerRepository actual constructor(
    mediaPlayHistoryDao: MediaPlayHistoryDao,
    articleDao: ArticleDao,
    enclosureDao: EnclosureDao
) : BasePlayerRepository(mediaPlayHistoryDao, articleDao, enclosureDao) {
    actual override fun requestPlaylistByPlatformFile(file: PlatformFile): List<PlaylistMediaWithArticleBean>? {
        return listOf(
            PlaylistMediaWithArticleBean.fromUrl(
                playlistId = "",
                url = file.path,
                orderPosition = 1.0,
            )
        )
    }
}

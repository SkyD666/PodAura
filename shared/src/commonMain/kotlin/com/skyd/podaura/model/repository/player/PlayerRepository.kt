package com.skyd.podaura.model.repository.player

import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.db.dao.MediaPlayHistoryDao
import io.github.vinceglb.filekit.PlatformFile

expect class PlayerRepository(
    mediaPlayHistoryDao: MediaPlayHistoryDao,
    articleDao: ArticleDao,
    enclosureDao: EnclosureDao,
) : BasePlayerRepository {
    override fun requestPlaylistByPlatformFile(file: PlatformFile): List<PlaylistMediaWithArticleBean>?
}

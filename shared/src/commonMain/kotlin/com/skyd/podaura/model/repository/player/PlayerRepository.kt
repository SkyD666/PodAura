package com.skyd.podaura.model.repository.player

import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.db.dao.MediaPlayHistoryDao

expect class PlayerRepository(
    mediaPlayHistoryDao: MediaPlayHistoryDao,
    articleDao: ArticleDao,
    enclosureDao: EnclosureDao,
) : BasePlayerRepository
package com.skyd.podaura.model.repository.download

import com.skyd.podaura.model.bean.article.ArticleBean

interface AutoDownloadStarter {
    suspend fun start(data: Map<String, List<ArticleBean>>)
}
package com.skyd.podaura.model.repository.fullcontent

interface IFullContentRepository {
    suspend fun fetch(url: String): FullContent
}

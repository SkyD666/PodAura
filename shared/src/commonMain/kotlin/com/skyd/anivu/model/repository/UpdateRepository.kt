package com.skyd.anivu.model.repository

import com.skyd.anivu.config.Const
import com.skyd.anivu.model.bean.UpdateBean
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory

@Factory(binds = [])
class UpdateRepository(private val httpClient: HttpClient) : BaseRepository() {
    fun checkUpdate(): Flow<UpdateBean> = flow {
        emit(httpClient.get(Const.GITHUB_LATEST_RELEASE).body<UpdateBean>())
    }.flowOn(Dispatchers.IO)
}
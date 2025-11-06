package com.skyd.podaura.model.repository

import com.skyd.fundation.config.Const
import com.skyd.podaura.model.bean.UpdateBean
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class UpdateRepository(
    private val httpClientConfig: HttpClientConfig<*>.() -> Unit,
) : BaseRepository() {
    fun checkUpdate(): Flow<UpdateBean> = flow {
        emit(HttpClient(httpClientConfig).get(Const.GITHUB_LATEST_RELEASE).body<UpdateBean>())
    }.flowOn(Dispatchers.IO)
}
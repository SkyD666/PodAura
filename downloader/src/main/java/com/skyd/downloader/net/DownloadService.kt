package com.skyd.downloader.net

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.HeaderMap
import retrofit2.http.Streaming
import retrofit2.http.Url

internal interface DownloadService {
    @Streaming
    @GET
    suspend fun getUrl(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<ResponseBody>

    @HEAD
    suspend fun getHeadersOnly(
        @Url url: String,
    ): Response<Void>
}

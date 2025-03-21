package com.skyd.downloader.net

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

internal object RetrofitInstance {

    @Volatile
    private var downloadService: DownloadService? = null

    fun getDownloadService(
        okHttpClient: OkHttpClient =
            OkHttpClient
                .Builder()
                .connectTimeout(10000L, TimeUnit.MILLISECONDS)
                .readTimeout(10000L, TimeUnit.MILLISECONDS)
                .build()
    ): DownloadService {
        if (downloadService == null) {
            synchronized(this) {
                if (downloadService == null) {
                    downloadService = Retrofit
                        .Builder()
                        .baseUrl("http://localhost/")
                        .client(okHttpClient)
                        .build()
                        .create(DownloadService::class.java)
                }
            }
        }
        return downloadService!!
    }
}

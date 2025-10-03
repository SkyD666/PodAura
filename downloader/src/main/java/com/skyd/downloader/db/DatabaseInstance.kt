package com.skyd.downloader.db

import android.content.Context
import androidx.room.Room

internal object DatabaseInstance {
    @Volatile
    private var instance: DownloadDatabase? = null

    fun getInstance(context: Context): DownloadDatabase {
        if (instance == null) {
            synchronized(DownloadDatabase::class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        DownloadDatabase::class.java,
                        "Downloader"
                    ).fallbackToDestructiveMigration(false).build()
                }
            }
        }
        return instance!!
    }
}

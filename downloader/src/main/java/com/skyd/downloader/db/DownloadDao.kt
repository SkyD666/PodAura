package com.skyd.downloader.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity)

    @Update
    suspend fun update(entity: DownloadEntity)

    @Query("SELECT * FROM ${DownloadEntity.TABLE_NAME} WHERE id = :id")
    suspend fun find(id: Int): DownloadEntity?

    @Query("DELETE FROM ${DownloadEntity.TABLE_NAME} WHERE id = :id")
    suspend fun remove(id: Int)

    @Query("DELETE FROM ${DownloadEntity.TABLE_NAME}")
    suspend fun deleteAll()

    @Query("SELECT * FROM ${DownloadEntity.TABLE_NAME} ORDER BY timeQueued ASC")
    fun getAllEntityFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM ${DownloadEntity.TABLE_NAME} WHERE createTime <= :timeMillis ORDER BY timeQueued ASC")
    fun getEntityTillTimeFlow(timeMillis: Long): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM ${DownloadEntity.TABLE_NAME} WHERE id = :id ORDER BY timeQueued ASC")
    fun getEntityByIdFlow(id: Int): Flow<DownloadEntity>

    @Query("SELECT * FROM ${DownloadEntity.TABLE_NAME} ORDER BY timeQueued ASC")
    suspend fun getAllEntity(): List<DownloadEntity>

    @Query("SELECT * FROM ${DownloadEntity.TABLE_NAME} WHERE createTime <= :timeMillis ORDER BY timeQueued ASC")
    suspend fun getEntityTillTime(timeMillis: Long): List<DownloadEntity>
}

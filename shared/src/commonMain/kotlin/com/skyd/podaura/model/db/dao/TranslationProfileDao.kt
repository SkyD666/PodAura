package com.skyd.podaura.model.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.skyd.podaura.model.db.entity.TRANSLATION_PROFILE_TABLE_NAME
import com.skyd.podaura.model.db.entity.TranslationProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationProfileDao {
    @Query("SELECT * FROM $TRANSLATION_PROFILE_TABLE_NAME ORDER BY isDefault DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<TranslationProfileEntity>>

    @Query("SELECT * FROM $TRANSLATION_PROFILE_TABLE_NAME WHERE enabled = 1 ORDER BY isDefault DESC, name COLLATE NOCASE")
    fun observeEnabled(): Flow<List<TranslationProfileEntity>>

    @Query("SELECT * FROM $TRANSLATION_PROFILE_TABLE_NAME WHERE id = :id LIMIT 1")
    suspend fun find(id: String): TranslationProfileEntity?

    @Query("SELECT * FROM $TRANSLATION_PROFILE_TABLE_NAME WHERE enabled = 1 AND isDefault = 1 LIMIT 1")
    suspend fun findDefault(): TranslationProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TranslationProfileEntity)

    @Transaction
    suspend fun save(entity: TranslationProfileEntity) {
        upsert(entity)
        if (entity.isDefault) {
            clearDefault()
            markDefault(entity.id)
        }
    }

    @Query("UPDATE $TRANSLATION_PROFILE_TABLE_NAME SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE $TRANSLATION_PROFILE_TABLE_NAME SET isDefault = 1, enabled = 1 WHERE id = :id")
    suspend fun markDefault(id: String)

    @Query(
        """
        UPDATE $TRANSLATION_PROFILE_TABLE_NAME
        SET isDefault = CASE WHEN id = :id THEN 1 ELSE 0 END,
            enabled = CASE WHEN id = :id THEN 1 ELSE enabled END
        WHERE EXISTS (
            SELECT 1 FROM $TRANSLATION_PROFILE_TABLE_NAME WHERE id = :id
        )
        """
    )
    suspend fun setDefault(id: String): Int

    @Query("DELETE FROM $TRANSLATION_PROFILE_TABLE_NAME WHERE id = :id")
    suspend fun delete(id: String): Int
}

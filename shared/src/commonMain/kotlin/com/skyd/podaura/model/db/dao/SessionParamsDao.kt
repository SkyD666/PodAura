package com.skyd.podaura.model.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skyd.podaura.model.bean.download.bt.SESSION_PARAMS_TABLE_NAME
import com.skyd.podaura.model.bean.download.bt.SessionParamsBean

@Dao
interface SessionParamsDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSessionParams(sessionParamsBean: SessionParamsBean)

    @Transaction
    @Delete
    suspend fun deleteSessionParams(sessionParamsBean: SessionParamsBean): Int

    @Transaction
    @Query(
        """
        DELETE FROM $SESSION_PARAMS_TABLE_NAME
        WHERE ${SessionParamsBean.LINK_COLUMN} = :link
        """
    )
    suspend fun deleteSessionParams(link: String): Int

    @Transaction
    @Query(
        """
        SELECT * FROM $SESSION_PARAMS_TABLE_NAME
        WHERE ${SessionParamsBean.LINK_COLUMN} = :link
        """
    )
    suspend fun getSessionParams(link: String): SessionParamsBean?
}
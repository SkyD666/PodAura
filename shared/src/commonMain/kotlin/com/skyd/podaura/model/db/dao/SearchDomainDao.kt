package com.skyd.podaura.model.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skyd.podaura.model.bean.SEARCH_DOMAIN_TABLE_NAME
import com.skyd.podaura.model.bean.SearchDomainBean
import com.skyd.podaura.model.bean.SearchDomainBean.Companion.COLUMN_NAME_COLUMN
import com.skyd.podaura.model.bean.SearchDomainBean.Companion.SEARCH_COLUMN
import com.skyd.podaura.model.bean.SearchDomainBean.Companion.TABLE_NAME_COLUMN

@Dao
interface SearchDomainDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSearchDomain(searchDomainBean: SearchDomainBean)

    @Transaction
    @Query(
        """SELECT $SEARCH_COLUMN FROM $SEARCH_DOMAIN_TABLE_NAME
           WHERE $TABLE_NAME_COLUMN LIKE :tableName AND $COLUMN_NAME_COLUMN LIKE :columnName"""
    )
    suspend fun getSearchDomainOrNull(tableName: String, columnName: String): Boolean?

    // 被选择的搜索域的个数
    @Transaction
    @Query("SELECT COUNT(*) FROM $SEARCH_DOMAIN_TABLE_NAME WHERE $SEARCH_COLUMN = 1")
    suspend fun selectedSearchDomainCount(): Int

    @Transaction
    suspend fun getSearchDomain(tableName: String, columnName: String): Boolean {
        val result = getSearchDomainOrNull(tableName, columnName)
        return result == true || selectedSearchDomainCount() == 0
    }

    @Transaction
    @Query(value = "SELECT * FROM $SEARCH_DOMAIN_TABLE_NAME")
    suspend fun getAllSearchDomain(): List<SearchDomainBean>
}
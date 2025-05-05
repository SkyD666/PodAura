package com.skyd.podaura.model.bean

import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlinx.serialization.Serializable

const val SEARCH_DOMAIN_TABLE_NAME = "SearchDomain"

@Serializable
@Entity(
    tableName = SEARCH_DOMAIN_TABLE_NAME,
    primaryKeys = [SearchDomainBean.TABLE_NAME_COLUMN, SearchDomainBean.COLUMN_NAME_COLUMN]
)
data class SearchDomainBean(
    @ColumnInfo(name = TABLE_NAME_COLUMN)
    var tableName: String,
    @ColumnInfo(name = COLUMN_NAME_COLUMN)
    var columnName: String,
    @ColumnInfo(name = SEARCH_COLUMN)
    var search: Boolean,
) : BaseBean {
    companion object {
        const val TABLE_NAME_COLUMN = "tableName"
        const val COLUMN_NAME_COLUMN = "columnName"
        const val SEARCH_COLUMN = "search"
    }
}


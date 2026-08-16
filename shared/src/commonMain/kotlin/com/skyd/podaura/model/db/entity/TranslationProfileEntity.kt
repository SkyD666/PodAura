package com.skyd.podaura.model.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

const val TRANSLATION_PROFILE_TABLE_NAME = "TranslationProfile"

@Entity(tableName = TRANSLATION_PROFILE_TABLE_NAME)
data class TranslationProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = ID_COLUMN)
    val id: String,
    @ColumnInfo(name = NAME_COLUMN)
    val name: String,
    @ColumnInfo(name = PROVIDER_TYPE_COLUMN)
    val providerType: String,
    @ColumnInfo(name = ENDPOINT_COLUMN)
    val endpoint: String?,
    @ColumnInfo(name = CREDENTIAL_ID_COLUMN)
    val credentialId: String?,
    @ColumnInfo(name = CUSTOM_HEADERS_JSON_COLUMN)
    val customHeadersJson: String,
    @ColumnInfo(name = REQUEST_TIMEOUT_MILLIS_COLUMN)
    val requestTimeoutMillis: Long,
    @ColumnInfo(name = ENABLED_COLUMN)
    val enabled: Boolean,
    @ColumnInfo(name = IS_DEFAULT_COLUMN)
    val isDefault: Boolean,
    @ColumnInfo(name = TARGET_LANGUAGE_COLUMN)
    val targetLanguage: String,
    @ColumnInfo(name = PROVIDER_CONFIG_JSON_COLUMN)
    val providerConfigJson: String,
    @ColumnInfo(name = CREATED_AT_COLUMN)
    val createdAt: Long,
    @ColumnInfo(name = UPDATED_AT_COLUMN)
    val updatedAt: Long,
) {
    companion object {
        const val ID_COLUMN = "id"
        const val NAME_COLUMN = "name"
        const val PROVIDER_TYPE_COLUMN = "providerType"
        const val ENDPOINT_COLUMN = "endpoint"
        const val CREDENTIAL_ID_COLUMN = "credentialId"
        const val CUSTOM_HEADERS_JSON_COLUMN = "customHeadersJson"
        const val REQUEST_TIMEOUT_MILLIS_COLUMN = "requestTimeoutMillis"
        const val ENABLED_COLUMN = "enabled"
        const val IS_DEFAULT_COLUMN = "isDefault"
        const val TARGET_LANGUAGE_COLUMN = "targetLanguage"
        const val PROVIDER_CONFIG_JSON_COLUMN = "providerConfigJson"
        const val CREATED_AT_COLUMN = "createdAt"
        const val UPDATED_AT_COLUMN = "updatedAt"
    }
}

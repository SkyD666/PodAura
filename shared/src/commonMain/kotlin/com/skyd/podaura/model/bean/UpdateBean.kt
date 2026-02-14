package com.skyd.podaura.model.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateBean(
    @SerialName("tag_name")
    var tagName: String,
    @SerialName("name")
    var name: String,
    @SerialName("html_url")
    var htmlUrl: String,
    @SerialName("published_at")
    var publishedAt: String,
    @SerialName("assets")
    var assets: List<AssetsBean>,
    @SerialName("body")
    var body: String
) : BaseBean {

    @Serializable
    class AssetsBean(
        @SerialName("name")
        var name: String,
        @SerialName("size")
        var size: Long,
        @SerialName("download_count")
        var downloadCount: Long?,
        @SerialName("browser_download_url")
        var browserDownloadUrl: String,
        @SerialName("created_at")
        var createdAt: String?,
        @SerialName("updated_at")
        var updatedAt: String?
    ) : BaseBean

}
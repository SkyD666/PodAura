package com.skyd.podaura.model.repository.feed.rssparser.rss

import com.skyd.podaura.model.repository.feed.rssparser.rss.Rss.Companion.CONTENT_PREFIX
import com.skyd.podaura.model.repository.feed.rssparser.rss.Rss.Companion.ITUNES_NAMESPACE
import com.skyd.podaura.model.repository.feed.rssparser.rss.Rss.Companion.ITUNES_PREFIX
import com.skyd.podaura.model.repository.feed.rssparser.rss.Rss.Companion.MEDIA_NAMESPACE
import com.skyd.podaura.model.repository.feed.rssparser.rss.Rss.Companion.MEDIA_PREFIX
import com.skyd.podaura.model.repository.feed.rssparser.rss.Rss.Companion.RSS_CONTENT_NAMESPACE
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue


@Serializable
@XmlSerialName("item")
data class Item(
    @XmlElement
    @XmlSerialName("title")
    val title: String?,

    @XmlElement
    @XmlSerialName("link")
    val link: String?,

    @XmlElement
    @XmlSerialName("description")
    val description: String?,

    @XmlElement
    @XmlSerialName(value = "encoded", namespace = RSS_CONTENT_NAMESPACE, prefix = CONTENT_PREFIX)
    val contentEncoded: String?,

    @XmlElement
    @XmlSerialName("pubDate")
    val pubDate: String?,

    @XmlElement
    @XmlSerialName("guid")
    val guid: String?,

    @XmlElement
    @XmlSerialName("author")
    val author: String?,

    val categories: List<Category>,

    val enclosures: List<Enclosure>,

    // media
    @XmlElement
    @XmlSerialName(value = "title", namespace = MEDIA_NAMESPACE, prefix = MEDIA_PREFIX)
    val mediaTitle: String?,

    @XmlElement
    @XmlSerialName(value = "rating", namespace = MEDIA_NAMESPACE, prefix = MEDIA_PREFIX)
    val mediaRating: String?,

    val mediaContent: MediaContent?,

    val mediaGroup: MediaGroup?,

    val mediaThumbnail: List<MediaThumbnail>,

    // itunes
    @XmlElement
    @XmlSerialName(value = "duration", namespace = ITUNES_NAMESPACE, prefix = ITUNES_PREFIX)
    val itunesDuration: String?,    // maybe seconds or "xx:xx"

    @XmlElement
    @XmlSerialName(value = "image", namespace = ITUNES_NAMESPACE, prefix = ITUNES_PREFIX)
    val itunesImage: Channel.ItunesImage?,

    @XmlElement
    @XmlSerialName(value = "episode", namespace = ITUNES_NAMESPACE, prefix = ITUNES_PREFIX)
    val itunesEpisode: String?,

    @XmlElement
    @XmlSerialName(value = "author", namespace = ITUNES_NAMESPACE, prefix = ITUNES_PREFIX)
    val itunesAuthor: String?,

    @XmlElement
    @XmlSerialName(value = "explicit", namespace = ITUNES_NAMESPACE, prefix = ITUNES_PREFIX)
    val itunesExplicit: Boolean?,

    val itunesCategories: List<ItunesCategory>?,
) {

    @Serializable
    @XmlSerialName("category")
    data class Category(
        @XmlValue
        val text: String,

        @XmlSerialName("domain")
        val domain: String?,
    )

    @Serializable
    @XmlSerialName("enclosure")
    data class Enclosure(
        @XmlSerialName("url")
        val url: String,

        @XmlSerialName("length")
        val length: Long?,

        @XmlSerialName("type")
        val type: String,
    )

    @Serializable
    @XmlSerialName(value = "thumbnail", namespace = MEDIA_NAMESPACE, prefix = MEDIA_PREFIX)
    data class MediaThumbnail(
        @XmlSerialName("url")
        val url: String,

        @XmlSerialName("height")
        val height: Int?,

        @XmlSerialName("width")
        val width: Int?,
    )

    @Serializable
    @XmlSerialName(value = "content", namespace = MEDIA_NAMESPACE, prefix = MEDIA_PREFIX)
    data class MediaContent(
        @XmlSerialName("url")
        val url: String,

        @XmlSerialName("fileSize")
        val fileSize: Long?,

        @XmlSerialName("duration")
        val duration: Long?,

        @XmlSerialName("type")
        val type: String?,
    )

    @Serializable
    @XmlSerialName(value = "group", namespace = MEDIA_NAMESPACE, prefix = MEDIA_PREFIX)
    data class MediaGroup(
        val contents: List<MediaContent>,
    )

    @Serializable
    @XmlSerialName(value = "category", namespace = ITUNES_NAMESPACE, prefix = ITUNES_PREFIX)
    data class ItunesCategory(
        @XmlSerialName("text")
        val text: String?,
        @XmlElement
        val itunesCategory: ItunesCategory?,
    )
}

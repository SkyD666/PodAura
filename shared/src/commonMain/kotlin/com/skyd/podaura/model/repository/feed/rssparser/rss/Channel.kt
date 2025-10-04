package com.skyd.podaura.model.repository.feed.rssparser.rss

import com.skyd.podaura.model.repository.feed.rssparser.rss.Rss.Companion.ITUNES_NAMESPACE
import com.skyd.podaura.model.repository.feed.rssparser.rss.Rss.Companion.ITUNES_PREFIX
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName


@Serializable
@XmlSerialName("channel")
data class Channel(
    @XmlElement
    @XmlSerialName("title")
    val title: String,

    @XmlElement
    @XmlSerialName("link")
    val link: String,

    @XmlElement
    @XmlSerialName("description")
    val description: String?,

    @XmlElement
    @XmlSerialName("image")
    val image: Image?,

    @XmlSerialName("item")
    val items: List<Item>,

    // itunes
    @XmlElement
    @XmlSerialName(value = "author", namespace = ITUNES_NAMESPACE, prefix = ITUNES_PREFIX)
    val itunesAuthor: String?,

    @XmlElement
    @XmlSerialName(value = "image", namespace = ITUNES_NAMESPACE, prefix = ITUNES_PREFIX)
    val itunesImage: ItunesImage?,
) {
    @Serializable
    @XmlSerialName("image")
    data class Image(
        @XmlElement
        @XmlSerialName("url")
        val url: String,

        @XmlElement
        @XmlSerialName("title")
        val title: String,

        @XmlElement
        @XmlSerialName("link")
        val link: String,
    )

    @Serializable
    @XmlSerialName(value = "image", namespace = ITUNES_NAMESPACE, prefix = ITUNES_PREFIX)
    data class ItunesImage(
        @XmlSerialName("href")
        val href: String?,
    )
}

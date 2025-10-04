package com.skyd.podaura.model.repository.feed.rssparser.atom

import com.skyd.podaura.model.repository.feed.rssparser.rss.Item.MediaGroup
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@XmlSerialName("entry")
data class Entry(
    @XmlElement
    @XmlSerialName("id")
    val id: String,

    @XmlElement
    @XmlSerialName("title")
    val title: String,

    @XmlElement
    @XmlSerialName("updated")
    val updated: String?,

    @XmlElement
    @XmlSerialName("author")
    val author: Feed.Person?,

    @XmlElement
    @XmlSerialName("published")
    val published: String?,

    val content: Content?,

    @XmlElement
    @XmlSerialName("link")
    val links: List<Link>?,

    @XmlSerialName("category")
    val categories: List<Category>,

    val mediaGroup: MediaGroup?,
) {
    @Serializable
    @XmlSerialName("content")
    data class Content(
        @XmlSerialName("type")
        val type: String,

        @XmlSerialName("src")
        val src: String?,

        @XmlValue
        val text: String,
    )

    @Serializable
    data class Link(
        @XmlSerialName("href")
        val href: String,

        @XmlSerialName("rel")
        val rel: String?,

        @XmlSerialName("type")
        val type: String?,

        @XmlSerialName("title")
        val title: String?,

        @XmlSerialName("length")
        val length: Long?,
    )

    @Serializable
    data class Category(
        @XmlSerialName("term")
        val term: String,

        @XmlSerialName("scheme")
        val scheme: String?,

        @XmlSerialName("label")
        val label: String?,
    )
}
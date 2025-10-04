package com.skyd.podaura.model.repository.feed.rssparser.atom

import com.skyd.podaura.model.repository.feed.rssparser.BaseXml
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

// https://validator.w3.org/feed/docs/atom.html
@Serializable
@XmlSerialName("feed", namespace = "http://www.w3.org/2005/Atom")
data class Feed(
    @XmlElement
    @XmlSerialName("title")
    val title: String,

    @XmlElement
    @XmlSerialName("link")
    val link: String,

    @XmlElement
    @XmlSerialName("updated")
    val updated: String?,

    @XmlElement
    @XmlSerialName("author")
    val author: Person?,

    @XmlElement
    @XmlSerialName("icon")
    val icon: String?,

    @XmlElement
    @XmlSerialName("logo")
    val logo: String?,

    @XmlElement
    @XmlSerialName("subtitle")
    val subtitle: String?,

    @XmlSerialName("entry")
    val entries: List<Entry>,
) : BaseXml {
    @Serializable
    data class Person(
        @XmlSerialName("name")
        val name: String?,
    )
}
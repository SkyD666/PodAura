package com.skyd.podaura.model.repository.importexport.opml

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("opml")
internal data class Opml(
    @XmlElement(value = false)
    @XmlSerialName("version")
    val version: String?,
    @XmlElement(value = false)
    @XmlSerialName("head")
    val head: Head?,
    val body: Body
)

@Serializable
@XmlSerialName("head")
internal data class Head(
    @XmlElement
    @XmlSerialName("title")
    val title: String,
    @XmlElement
    @XmlSerialName("dateCreated")
    val dateCreated: String,
)

@Serializable
@XmlSerialName("body")
internal data class Body(
    @XmlSerialName("outline")
    val outlines: List<Outline>,
)

@Serializable
@XmlSerialName("outline")
internal data class Outline(
    @XmlElement(value = false)
    @XmlSerialName("title")
    val title: String?,

    @XmlElement(value = false)
    @XmlSerialName("text")
    val text: String?,

    @XmlElement(value = false)
    @XmlSerialName("description")
    val description: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("htmlUrl")
    val htmlUrl: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("xmlUrl")
    val xmlUrl: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("link")
    val link: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("icon")
    val icon: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("nickname")
    val nickname: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("customDescription")
    val customDescription: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("customIcon")
    val customIcon: String? = null,

    @XmlSerialName("outline")
    val outlines: List<Outline>? = null,
)

sealed interface OpmlSource

data class OpmlFeed(val title: String?, val link: String) : OpmlSource

data class OpmlFeedGroup(val title: String, val feeds: List<OpmlFeed>) : OpmlSource
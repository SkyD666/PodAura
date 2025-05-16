package com.skyd.podaura.model.repository.importexport.opmlparser.entity

import com.skyd.podaura.model.repository.importexport.opmlparser.serializer.OutlineCategorySerializer
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("outline")
data class Outline(
    @XmlElement(value = false)
    @XmlSerialName("title")
    val title: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("text")
    val text: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("type")
    val type: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("isComment")
    val isComment: Boolean? = null,

    @XmlElement(value = false)
    @XmlSerialName("isBreakpoint")
    val isBreakpoint: Boolean? = null,

    @XmlElement(value = false)
    @XmlSerialName("created")
    val created: String? = null,

    @Serializable(with = OutlineCategorySerializer::class)
    @XmlElement(value = false)
    @XmlSerialName("category")
    val category: List<String>? = null,

    @XmlElement(value = false)
    @XmlSerialName("description")
    val description: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("url")
    val url: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("htmlUrl")
    val htmlUrl: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("xmlUrl")
    val xmlUrl: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("language")
    val language: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("version")
    val version: String? = null,

    @XmlElement(value = false)
    @XmlSerialName("link")
    val link: String? = null,

    @XmlOtherAttributes
    val attributes: Map<String, String> = emptyMap(),

    @XmlSerialName("outline")
    val outlines: List<Outline>? = null,
)
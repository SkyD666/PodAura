package com.skyd.podaura.model.repository.importexport.opmlparser.entity

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("opml")
data class Opml(
    @XmlElement(value = false)
    @XmlSerialName("version")
    val version: String,
    @XmlElement(value = false)
    @XmlSerialName("head")
    val head: Head,
    @XmlElement(value = false)
    @XmlSerialName("body")
    val body: Body
)
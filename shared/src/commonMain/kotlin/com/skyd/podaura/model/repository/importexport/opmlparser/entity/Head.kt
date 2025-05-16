package com.skyd.podaura.model.repository.importexport.opmlparser.entity

import com.skyd.podaura.model.repository.importexport.opmlparser.serializer.ExpansionStateSerializer
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("head")
data class Head(
    @XmlElement
    @XmlSerialName("title")
    val title: String? = null,
    @XmlElement
    @XmlSerialName("dateCreated")
    val dateCreated: String? = null,
    @XmlElement
    @XmlSerialName("dateModified")
    val dateModified: String? = null,
    @XmlElement
    @XmlSerialName("ownerName")
    val ownerName: String? = null,
    @XmlElement
    @XmlSerialName("ownerEmail")
    val ownerEmail: String? = null,
    @XmlElement
    @XmlSerialName("ownerId")
    val ownerId: String? = null,
    @XmlElement
    @XmlSerialName("docs")
    val docs: String? = null,
    @Serializable(with = ExpansionStateSerializer::class)
    @XmlElement
    @XmlSerialName("expansionState")
    val expansionState: List<Int>? = null,
    @XmlElement
    @XmlSerialName("vertScrollState")
    val vertScrollState: Int? = null,
    @XmlElement
    @XmlSerialName("windowTop")
    val windowTop: Int? = null,
    @XmlElement
    @XmlSerialName("windowLeft")
    val windowLeft: Int? = null,
    @XmlElement
    @XmlSerialName("windowBottom")
    val windowBottom: Int? = null,
    @XmlElement
    @XmlSerialName("windowRight")
    val windowRight: Int? = null,
)
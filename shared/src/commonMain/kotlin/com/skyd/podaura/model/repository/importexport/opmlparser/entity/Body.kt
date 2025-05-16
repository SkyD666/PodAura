package com.skyd.podaura.model.repository.importexport.opmlparser.entity

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("body")
data class Body(
    @XmlSerialName("outline")
    val outlines: List<Outline>,
)
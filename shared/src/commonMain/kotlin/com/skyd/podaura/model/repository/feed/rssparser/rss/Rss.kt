package com.skyd.podaura.model.repository.feed.rssparser.rss

import com.skyd.podaura.model.repository.feed.rssparser.BaseXml
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("rss")
data class Rss(
    @XmlElement
    @XmlSerialName("channel")
    val channel: Channel
) : BaseXml() {
    companion object {
        const val MEDIA_PREFIX = "media"
        const val MEDIA_NAMESPACE = "http://search.yahoo.com/mrss/"
        const val ITUNES_PREFIX = "itunes"
        const val ITUNES_NAMESPACE = "http://www.itunes.com/dtds/podcast-1.0.dtd"
        const val CONTENT_PREFIX = "content"
        const val RSS_CONTENT_NAMESPACE = "http://purl.org/rss/1.0/modules/content/"
    }
}
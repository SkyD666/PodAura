package com.skyd.podaura.model.repository.importexport.opmlparser.dsl

import com.skyd.podaura.model.repository.importexport.opmlparser.entity.Body
import com.skyd.podaura.model.repository.importexport.opmlparser.entity.Head
import com.skyd.podaura.model.repository.importexport.opmlparser.entity.Opml

inline fun opml(builderAction: OpmlBuilder.() -> Unit): Opml {
    val builder = OpmlBuilder()
    builder.builderAction()
    return builder.build()
}

@OpmlDslMarker
class OpmlBuilder @PublishedApi internal constructor() {
    var version: String = "2.0"

    @PublishedApi
    internal var head: Head = Head()

    @PublishedApi
    internal var body: Body = Body(outlines = emptyList())

    inline fun head(builderAction: HeadBuilder.() -> Unit) {
        head = headBuilder(builderAction)
    }

    inline fun body(builderAction: BodyBuilder.() -> Unit) {
        body = bodyBuilder(builderAction)
    }

    @PublishedApi
    internal fun build(): Opml = Opml(version = version, head = head, body = body)
}
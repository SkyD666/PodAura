package com.skyd.podaura.model.repository.importexport.opmlparser.dsl

import com.skyd.podaura.model.repository.importexport.opmlparser.entity.Body

@PublishedApi
internal inline fun bodyBuilder(builderAction: BodyBuilder.() -> Unit): Body {
    val builder = BodyBuilder()
    builder.builderAction()
    return builder.build()
}

@OpmlDslMarker
class BodyBuilder @PublishedApi internal constructor() : OutlineDsl() {
    @PublishedApi
    internal fun build(): Body = Body(outlines = outlinesMutableList)
}
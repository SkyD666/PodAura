package com.skyd.podaura.model.repository.importexport.opmlparser.dsl

import com.skyd.podaura.model.repository.importexport.opmlparser.entity.Head

@PublishedApi
internal inline fun headBuilder(builderAction: HeadBuilder.() -> Unit): Head {
    val builder = HeadBuilder()
    builder.builderAction()
    return builder.build()
}

@OpmlDslMarker
class HeadBuilder @PublishedApi internal constructor() {
    internal var title: String? = null
    internal var dateCreated: String? = null
    internal var dateModified: String? = null
    internal var ownerName: String? = null
    internal var ownerEmail: String? = null
    internal var ownerId: String? = null
    internal var docs: String? = null
    internal var expansionState: List<Int>? = null
    internal var vertScrollState: Int? = null
    internal var windowTop: Int? = null
    internal var windowLeft: Int? = null
    internal var windowBottom: Int? = null
    internal var windowRight: Int? = null

    @PublishedApi
    internal fun build(): Head = Head(
        title = title,
        dateCreated = dateCreated,
        dateModified = dateModified,
        ownerName = ownerName,
        ownerEmail = ownerEmail,
        ownerId = ownerId,
        docs = docs,
        expansionState = expansionState,
        vertScrollState = vertScrollState,
        windowTop = windowTop,
        windowLeft = windowLeft,
        windowBottom = windowBottom,
        windowRight = windowRight,
    )
}
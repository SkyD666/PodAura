package com.skyd.podaura.model.repository.importexport.opmlparser.dsl

import com.skyd.podaura.model.repository.importexport.opmlparser.entity.Outline

@PublishedApi
internal inline fun outlineBuilder(builderAction: OutlineBuilder.() -> Unit): Outline {
    val builder = OutlineBuilder()
    builder.builderAction()
    return builder.build()
}

abstract class OutlineDsl {
    protected val outlinesMutableList: MutableList<Outline> = mutableListOf()

    var outlines: List<Outline>
        get() = outlinesMutableList.toList()
        set(value) {
            outlinesMutableList.clear()
            outlinesMutableList += value
        }

    inline fun outline(builderAction: OutlineBuilder.() -> Unit) =
        outline(outlineBuilder(builderAction))

    fun outline(outline: Outline) {
        outlinesMutableList += outline
    }
}

@OpmlDslMarker
class OutlineBuilder @PublishedApi internal constructor() : OutlineDsl() {
    internal val attributesMutableMap: MutableMap<String, String> = mutableMapOf()
    internal val categoryMutableList: MutableList<String> = mutableListOf()

    var title: String? = null
    var text: String? = null
    var type: String? = null
    var isComment: Boolean? = null
    var isBreakpoint: Boolean? = null
    var created: String? = null
    var description: String? = null
    var url: String? = null
    var htmlUrl: String? = null
    var xmlUrl: String? = null
    var language: String? = null
    var version: String? = null
    var link: String? = null
    var attributes: Map<String, String>
        get() = attributesMutableMap.toMap()
        set(value) {
            attributesMutableMap.clear()
            attributesMutableMap += value
        }
    var category: List<String>
        get() = categoryMutableList.toList()
        set(value) {
            categoryMutableList.clear()
            categoryMutableList.addAll(value)
        }

    fun attribute(key: String, value: String?) {
        if (value == null) return
        attributesMutableMap[key] = value
    }

    fun category(category: String) {
        categoryMutableList += category
    }

    @PublishedApi
    internal fun build(): Outline = Outline(
        title = title,
        text = text,
        type = type,
        isComment = isComment,
        isBreakpoint = isBreakpoint,
        created = created,
        category = category.takeIf { it.isNotEmpty() },
        description = description,
        url = url,
        htmlUrl = htmlUrl,
        xmlUrl = xmlUrl,
        language = language,
        version = version,
        link = link,
        attributes = attributesMutableMap,
        outlines = outlinesMutableList.takeIf { it.isNotEmpty() },
    )
}
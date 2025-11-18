package com.skyd.htmlrender.base.css

import com.skyd.htmlrender.base.css.model.CSSDeclarationWithImportant
import com.skyd.htmlrender.base.css.model.CSSRuleSet
import com.skyd.htmlrender.base.css.model.StyleOrigin

/**
 * @return null if empty
 */
internal fun parseCssDeclarations(declarations: String): List<CSSDeclarationWithImportant>? =
    buildList {
        val srcList = declarations.split(';')
        srcList.forEach { src ->
            val declaration = src.split(':')
            if (declaration.size == 2) {
                val value = declaration[1].trim()
                val afterRemove = value.removeSuffix("!important")
                add(
                    CSSDeclarationWithImportant(
                        declaration[0].trim(),
                        afterRemove.trim(),
                        value != afterRemove
                    )
                )
            }
        }
    }.ifEmpty { null }

internal fun parseCssRuleBlock(origin: StyleOrigin, block: String): List<CSSRuleSet> {
    val removeCommentRegex = "/\\*.*?\\*/".toRegex()
    val setList = block.replace(removeCommentRegex, "").trim().split('}')

    return buildList(setList.size) {
        setList.forEach { set ->
            val list = set.split('{')
            if (list.size == 2) {
                parseCssDeclarations(list[1].trim())?.also { declarations ->
                    add(CSSRuleSet(origin, list[0].trim(), declarations))
                }
            }
        }
    }
}
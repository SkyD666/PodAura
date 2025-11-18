package com.skyd.htmlrender.core.processor

import com.skyd.htmlrender.base.model.HtmlNode
import com.skyd.htmlrender.base.model.NodeProcessor
import com.skyd.htmlrender.base.model.StringNode
import com.skyd.htmlrender.base.model.StyleNode
import com.skyd.htmlrender.core.styler.IParagraphStyleStyler
import com.skyd.htmlrender.core.styler.ParagraphEndStyler
import com.skyd.htmlrender.core.styler.ParagraphStartStyler
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

object ParagraphNodeProcessor : NodeProcessor {

    override suspend fun processNode(node: HtmlNode) {
        currentCoroutineContext().ensureActive()
        when (node) {
            is StringNode -> {
                return
            }

            is StyleNode -> {
                val children = node.children
                if (children.isNullOrEmpty()) {
                    return
                }

                for (i in children.indices) {
                    val currentNode = children[i]

                    if (currentNode !is StyleNode) continue
                    val currentStylers = currentNode.stylers ?: continue
                    val previousNode = if (i > 0) {
                        children[i - 1] as? StyleNode
                    } else {
                        null
                    }

                    fun isPreviousNodeHadEnd(extraLine: Boolean) =
                        previousNode?.stylers?.any { it is ParagraphEndStyler && it.extraLine == extraLine }

                    val isPreviousNodeHadEndWithExtraLine = isPreviousNodeHadEnd(true) == true
                    val isPreviousNodeHadEndNoExtraLine = isPreviousNodeHadEnd(false) == true

                    if (isPreviousNodeHadEndWithExtraLine
                        || isPreviousNodeHadEndNoExtraLine && currentStylers.any { it is ParagraphStartStyler && !it.extraLine }
                    ) {
                        currentStylers.removeAll { it is ParagraphStartStyler }
                    }
                    if (isPreviousNodeHadEndNoExtraLine) {
                        for (index in currentStylers.indices) {
                            val styler = currentStylers[index]
                            if (styler is ParagraphStartStyler && styler.extraLine) {
                                currentStylers[index] = ParagraphStartStyler(false)
                            }
                        }
                    }

                    // If the current node has ParagraphStyle, remove unnecessary stylers
                    if (currentStylers.any { it is IParagraphStyleStyler }) {
                        if (isPreviousNodeHadEndNoExtraLine) {
                            previousNode?.stylers?.removeAll { it is ParagraphEndStyler }
                        }
                        if (isPreviousNodeHadEndWithExtraLine) {
                            previousNode?.stylers?.also { stylers ->
                                for (index in stylers.indices) {
                                    val styler = stylers[index]
                                    if (styler is ParagraphStartStyler && styler.extraLine) {
                                        stylers[index] = ParagraphStartStyler(false)
                                    }
                                }
                            }
                        }

                        for (index in currentStylers.lastIndex downTo 0) {
                            val styler = currentStylers[index]
                            if (styler is ParagraphStartStyler) {
                                if (styler.extraLine) {
                                    currentStylers[index] = ParagraphStartStyler(false)
                                } else {
                                    currentStylers.remove(styler)
                                }
                            } else if (styler is ParagraphEndStyler) {
                                if (styler.extraLine) {
                                    currentStylers[index] = ParagraphEndStyler(false)
                                } else {
                                    currentStylers.remove(styler)
                                }
                            }
                        }
                    }

                    // Recursively handle children of the current node
                    processNode(currentNode)
                }
            }
        }
    }
}
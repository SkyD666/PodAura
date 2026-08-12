package com.skyd.podaura.ui.component.webview

object WebViewStyle {

    private fun readerPriority(harmonizedSource: Boolean): String =
        if (harmonizedSource) "" else "!important"

    private fun argbToCssColor(argb: Int): String = "#${(0xFFFFFF and argb).toString(16).uppercase().padStart(6, '0')}"

    private fun applyFontFace(
        fontPath: String? = null
    ): String = if (fontPath != null) """
        @font-face {
            font-family: external;
            src: url("file://$fontPath")
        }
    """.trimIndent() else ""

    private fun applyFontFamily(
        fontPath: String? = null
    ): String = if (fontPath != null) """
        --font-family: external;
    """.trimIndent() else ""

    fun get(
        fontSize: Float,
        fontPath: String? = null,
        lineHeight: String,
        letterSpacing: Float,
        horizontalPadding: Float,
        textColor: Int,
        textWeight: Int?,
        textAlign: String,
        boldTextColor: Int,
        subheadBold: Boolean,
        subheadUpperCase: Boolean,
        imgMargin: Float,
        imgBorderRadius: Int,
        linkTextColor: Int,
        codeTextColor: Int,
        codeBgColor: Int,
        tablePadding: Float,
        selectionTextColor: Int,
        selectionBgColor: Int,
        harmonizedSource: Boolean = false,
    ): String = """
${applyFontFace(fontPath)}
:root {
    ${applyFontFamily(fontPath)}
    --font-size: ${fontSize.toInt()}px;
    --line-height: ${lineHeight};
    --letter-spacing: ${letterSpacing}px;
    --text-margin: ${horizontalPadding}px;
    --text-color: ${argbToCssColor(textColor)};
    --text-bold: ${textWeight ?: "normal"};
    --text-align: ${textAlign};
    --bold-text-color: ${argbToCssColor(boldTextColor)};
    --link-text-color: ${argbToCssColor(linkTextColor)};
    --selection-text-color: ${argbToCssColor(selectionTextColor)};
    --selection-bg-color: ${argbToCssColor(selectionBgColor)};
    --subhead-bold: ${if (subheadBold) "600" else "normal"};
    --subhead-upper-case: ${if (subheadUpperCase) "uppercase" else "none"};
    --img-margin: ${imgMargin}px;
    --img-border-radius: ${imgBorderRadius}px;
    --content-padding;
    --bold-text-color;
    --image-caption-margin;
    --blockquote-margin: 20px;
    --blockquote-padding;
    --blockquote-bg-color;
    --blockquote-border-width: 3px;
    --blockquote-border-color: ${argbToCssColor(textColor)}33;
    --table-margin: ${tablePadding}px;
    --table-border-width;
    --table-border-color;
    --table-cell-padding: 0.2em;
    --table-alt-row-bg-color;
    --code-text-color: ${argbToCssColor(codeTextColor)};
    --code-bg-color: ${argbToCssColor(codeBgColor)};
    --code-scrollbar-color: ${argbToCssColor(codeTextColor)}22;
    --code-border-width;
    --code-border-color;
    --code-padding;
    --code-font-family: Menlo, Monospace, 'Courier New';
    --code-font-size: 0.9em;
    --pre-color;
}

article {
    padding: 0;
    margin: 0;
    font-family: var(--font-family) ${readerPriority(harmonizedSource)};
    font-size: var(--font-size) ${readerPriority(harmonizedSource)};
    font-weight: var(--text-bold) ${readerPriority(harmonizedSource)};
    color: var(--text-color) ${readerPriority(harmonizedSource)};
    word-wrap: break-word ${readerPriority(harmonizedSource)};
    overflow-wrap: break-word ${readerPriority(harmonizedSource)};
}

body > main > article {
    margin-left: var(--text-margin) !important;
    margin-right: var(--text-margin) !important;
}

/* Page  */
body {
    margin: 0;
    padding: 0;
}

::selection {
    background: var(--selection-bg-color) !important;
    color: var(--selection-text-color) !important;
}

/* Heading  */
h1,
h2,
h3,
h4,
h5,
h6 {
    font-weight: var(--subhead-bold) ${readerPriority(harmonizedSource)};
    text-transform: var(--subhead-upper-case) !important;
    line-height: calc(min(1.2, var(--line-height))) ${readerPriority(harmonizedSource)};
    letter-spacing: var(--letter-spacing) ${readerPriority(harmonizedSource)};
    color: var(--bold-text-color) ${readerPriority(harmonizedSource)};
    text-align: var(--text-align) ${readerPriority(harmonizedSource)};
}

/* Paragraph */
p {
    max-width: 100% !important;
    word-wrap: break-word ${readerPriority(harmonizedSource)};
    overflow-wrap: break-word ${readerPriority(harmonizedSource)};
    line-height: var(--line-height) ${readerPriority(harmonizedSource)};
    letter-spacing: var(--letter-spacing) ${readerPriority(harmonizedSource)};
    text-align: var(--text-align) ${readerPriority(harmonizedSource)};
}

span {
    line-height: var(--line-height) ${readerPriority(harmonizedSource)};
    letter-spacing: var(--letter-spacing) ${readerPriority(harmonizedSource)};
    text-align: var(--text-align) ${readerPriority(harmonizedSource)};
}

/* Strong  */
strong,
b {
    color: var(--bold-text-color) ${readerPriority(harmonizedSource)};
}

/* Link */
a,
a > strong {
    overflow-wrap: anywhere;
    -webkit-tap-highlight-color: rgba(0, 0, 0, 0);
    color: var(--link-text-color) ${readerPriority(harmonizedSource)};
}
div > a {
    display: block;
    overflow-wrap: anywhere;
    -webkit-tap-highlight-color: rgba(0, 0, 0, 0);
    color: var(--link-text-color);
    line-height: var(--line-height);
    letter-spacing: var(--letter-spacing) !important;
    text-align: var(--text-align) !important;
}

/* Image  */
iframe,
video,
embed,
object,
img {
    margin-top: 0.5em !important;
    margin-left: calc(0px - var(--text-margin) + var(--img-margin)) !important;
    margin-right: calc(0px - var(--text-margin) + var(--img-margin)) !important;
    max-width: calc(100% + 2 * var(--text-margin) - 2 * var(--img-margin)) !important;
    border-radius: var(--img-border-radius) ${readerPriority(harmonizedSource)};
}

img {
     height: auto !important;
}

img::after {
    width: 100px !important;
}

img.loaded {
    opacity: 1; /* 加载完成后设置透明度为1 */
}

img.thin {
    margin-top: 0.5em !important;
    margin-bottom: 0.5em !important;
    margin-left: unset !important;
    margin-right: unset !important;
    max-width: 100% !important;
}

p > img {
    margin-top: 0.5em !important;
    margin-bottom: 0.5em !important;
    margin-left: calc(0px - var(--text-margin) + var(--img-margin)) !important;
    margin-right: calc(0px - var(--text-margin) + var(--img-margin)) !important;
    max-width: calc(100% + 2 * var(--text-margin) - 2 * var(--img-margin)) !important;
    height: auto !important;
    border-radius: var(--img-border-radius) ${readerPriority(harmonizedSource)};
}

img + small {
    display: inline-block;
    line-height: calc(min(1.5, var(--line-height))) !important;
    letter-spacing: var(--letter-spacing) !important;
    margin-top: var(--image-caption-margin) !important;
    text-align: var(--text-align) !important;
}

/* List */
ul,
ol {
    padding-left: 1.5em !important;
    padding-inline-start: 1.5em !important;
    padding-inline-end: 0 !important;
    line-height: var(--line-height) ${readerPriority(harmonizedSource)};
    letter-spacing: var(--letter-spacing) ${readerPriority(harmonizedSource)};
    text-align: var(--text-align) ${readerPriority(harmonizedSource)};
}

ul {
    list-style-type: disc !important;
    list-style-position: outside !important;
}

ul > li {
    display: list-item !important;
    list-style-type: disc !important;
    list-style-position: outside !important;
}

ul ul > li {
    list-style-type: circle !important;
}

ul ul ul > li {
    list-style-type: square !important;
}

ol {
    list-style-type: decimal !important;
    list-style-position: outside !important;
}

ol > li {
    display: list-item !important;
    list-style-type: decimal !important;
    list-style-position: outside !important;
}

li {
    line-height: var(--line-height) ${readerPriority(harmonizedSource)};
    letter-spacing: var(--letter-spacing) ${readerPriority(harmonizedSource)};
    margin-inline-start: 0 !important;
    text-align: var(--text-align) ${readerPriority(harmonizedSource)};
}

/* Quote  */
blockquote {
    margin-left: 0.5em ${readerPriority(harmonizedSource)};
    padding-left: calc(0.9em) ${readerPriority(harmonizedSource)};
    background-color: var(--blockquote-bg-color) ${readerPriority(harmonizedSource)};
    border-left: var(--blockquote-border-width) solid var(--blockquote-border-color) ${readerPriority(harmonizedSource)};
    line-height: var(--line-height) ${readerPriority(harmonizedSource)};
    letter-spacing: var(--letter-spacing) ${readerPriority(harmonizedSource)};
    text-align: var(--text-align) ${readerPriority(harmonizedSource)};
}

blockquote blockquote {
    margin-right: 0 ${readerPriority(harmonizedSource)};
}

blockquote img {
    max-width: 100% !important;
    left: 0 !important;
}

/* Table  */
table {
    display: block;
    max-width: 100% !important;
    width: 100% ${readerPriority(harmonizedSource)};
    overflow-x: auto;
    border-collapse: collapse ${readerPriority(harmonizedSource)};
    margin-left: 0 !important;
    margin-right: 0 !important;
}

table th,
table td {
    border: var(--table-border-width) solid var(--table-border-color) ${readerPriority(harmonizedSource)};
    padding: var(--table-cell-padding) ${readerPriority(harmonizedSource)};
    line-height: var(--line-height) ${readerPriority(harmonizedSource)};
    letter-spacing: var(--letter-spacing) ${readerPriority(harmonizedSource)};
    text-align: var(--text-align) ${readerPriority(harmonizedSource)};
}

table tr {
    display: ${if (harmonizedSource) "table-row" else "block"};
}

table tr table tr td {
    display: ${if (harmonizedSource) "table-cell" else "inline-block"};
}

table tr:nth-child(even) {
    background-color: var(--table-alt-row-bg-color) !important;
}

/* Code */
pre,
code {
    color: var(--code-text-color) !important;
    background-color: var(--code-bg-color) !important;
    border: 1 solid var(--code-text-color) !important;
    border-radius: 8px !important;
    padding: 2px 5px !important;
    margin: 2px !important;
    font-family: var(--code-font-family) !important;
    font-size: var(--code-font-size) !important;
}

pre {
    overflow: auto !important;
}

code {
    display: inline-block !important;
}

li code {
    white-space: pre-wrap !important;
    word-wrap: break-all !important;
    overflow-wrap: break-word !important;
    max-width: 100% !important;
}

pre::-webkit-scrollbar {
    height: 14px;
}

pre::-webkit-scrollbar-track {
    background-color: transparent;
}

pre::-webkit-scrollbar-thumb {
    background-color: var(--code-scrollbar-color);
    border-radius: 7px;
    background-clip: content-box;
    border: 5px solid transparent;
    border-left-width: 10px;
    border-right-width: 10px;
}

/* MISC */
figure {
    line-height: calc(min(1.5, var(--line-height))) ${readerPriority(harmonizedSource)};
    letter-spacing: var(--letter-spacing) ${readerPriority(harmonizedSource)};
    text-align: var(--text-align) ${readerPriority(harmonizedSource)};
    margin: 0 ${readerPriority(harmonizedSource)};
    font-size: inherit ${readerPriority(harmonizedSource)};
}

figure * {
    font-size: 1em ${readerPriority(harmonizedSource)};
}

figure p,
caption,
figcaption {
    font-size: 0.85em ${readerPriority(harmonizedSource)};
}

hr {
    border: 0 !important;
    height: 2px !important;
    background-color: var(--text-color) !important;
    opacity: 0.08 !important;
    border-radius: 2px;
}

/* Bionic Reading */
body {
    --br-boldness: 600;
}

[br-mode=on] br-bold *,
                         [br-mode=on] br-edge  {
    opacity: var(--fixation-edge-opacity,  100%);
}

[br-mode=on] br-bold:nth-of-type(n+1) [fixation-strength="1"] {
    display: inline;
    font-weight: var(--br-boldness);
    line-height: var(--br-line-height,  initial);
    text-decoration: var(--br-line-style) underline 2px;
    color: var(--bold-text-color) !important;
    text-underline-offset: 3px;
}

[br-mode=on] br-bold:nth-of-type(n+1) [fixation-strength="2"] {
    display: inline;
    font-weight: var(--br-boldness);
    line-height: var(--br-line-height, initial);
    text-decoration: var(--br-line-style) underline 2px;
    color: var(--bold-text-color) !important;
    text-underline-offset: 3px;
}

"""
}

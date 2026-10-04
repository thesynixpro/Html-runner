package com.aprax.htmlrun.editor

enum class Language {
    HTML,
    CSS,
    JS,
    MARKDOWN,
    PLAIN;

    companion object {
        fun ofFile(name: String): Language = when (name.substringAfterLast('.', "").lowercase()) {
            "html", "htm", "xhtml", "svg", "vue" -> HTML
            "css" -> CSS
            "js", "mjs", "cjs", "jsx" -> JS
            "md", "markdown" -> MARKDOWN
            else -> PLAIN
        }
    }
}

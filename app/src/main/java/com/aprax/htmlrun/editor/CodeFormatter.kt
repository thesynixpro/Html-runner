package com.aprax.htmlrun.editor

/** Re indents CSS and JavaScript by brace depth. HTML is left untouched. */
object CodeFormatter {

    fun supports(language: Language): Boolean = language == Language.CSS || language == Language.JS

    fun format(source: String, indentSize: Int): String {
        val unit = " ".repeat(indentSize.coerceIn(2, 8))
        val output = StringBuilder(source.length + 64)
        var depth = 0

        for (raw in source.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) {
                output.append('\n')
                continue
            }
            if (isClosing(line)) depth = (depth - 1).coerceAtLeast(0)
            repeat(depth) { output.append(unit) }
            output.append(line).append('\n')
            depth = (depth + delta(line)).coerceAtLeast(0)
        }

        return output.toString().trimEnd('\n') + "\n"
    }

    private fun isClosing(line: String): Boolean = when (line.first()) {
        '}', ')', ']' -> true
        else -> false
    }

    private fun delta(line: String): Int {
        var delta = 0
        var quote: Char? = null
        var index = 0

        while (index < line.length) {
            val char = line[index]
            when {
                quote != null -> if (char == quote && line[index - 1] != '\\') quote = null
                char == '"' || char == '\'' || char == '`' -> quote = char
                char == '/' && index + 1 < line.length && line[index + 1] == '/' -> return delta
                char == '{' || char == '[' || char == '(' -> delta++
                char == '}' || char == ']' || char == ')' -> delta--
            }
            index++
        }

        return delta
    }
}

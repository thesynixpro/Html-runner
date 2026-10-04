package com.openprojects.htmlrunner.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Small regex based highlighter. It never changes the text, it only assigns colors,
 * so the editor cursor and selection stay correct.
 */
object SyntaxHighlighter {

    private val commentColor = Color(0xFF6B7385)
    private val tagColor = Color(0xFF7EE787)
    private val attributeColor = Color(0xFF79C0FF)
    private val stringColor = Color(0xFFFFA657)
    private val keywordColor = Color(0xFFFF7B9C)
    private val numberColor = Color(0xFFD2A8FF)

    private val htmlPattern = Regex(
        "<!--[\\s\\S]*?-->|</?[A-Za-z][\\w:.-]*|\"[^\"\\n]*\"|'[^'\\n]*'|[A-Za-z-]+(?==)"
    )

    private val cssPattern = Regex(
        "/\\*[\\s\\S]*?\\*/|\"[^\"\\n]*\"|'[^'\\n]*'|@[A-Za-z-]+|#[0-9A-Fa-f]{3,8}\\b|" +
            "\\b\\d+(?:\\.\\d+)?\\b|(?<=[{;])[ \\t]*[A-Za-z-]+(?=\\s*:)"
    )

    private val jsPattern = Regex(
        "//[^\\n]*|/\\*[\\s\\S]*?\\*/|`[^`]*`|\"[^\"\\n]*\"|'[^'\\n]*'|" +
            "\\b(?:const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|" +
            "new|class|extends|super|this|typeof|instanceof|in|of|delete|void|try|catch|finally|" +
            "throw|async|await|yield|import|export|from|default|null|undefined|true|false|NaN)\\b|" +
            "\\b\\d+(?:\\.\\d+)?\\b"
    )

    fun highlight(text: String, language: Language): AnnotatedString {
        val pattern = when (language) {
            Language.HTML -> htmlPattern
            Language.CSS -> cssPattern
            Language.JS -> jsPattern
        }
        return buildAnnotatedString {
            var last = 0
            pattern.findAll(text).forEach { match ->
                val start = match.range.first
                if (start > last) {
                    append(text.substring(last, start))
                }
                pushStyle(styleFor(match.value, language))
                append(match.value)
                pop()
                last = match.range.last + 1
            }
            if (last < text.length) {
                append(text.substring(last))
            }
        }
    }

    private fun styleFor(token: String, language: Language): SpanStyle {
        val first = token.firstOrNull() ?: return SpanStyle()
        return when (language) {
            Language.HTML -> when {
                token.startsWith("<!--") -> SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)
                first == '<' -> SpanStyle(color = tagColor)
                first == '"' || first == '\'' -> SpanStyle(color = stringColor)
                else -> SpanStyle(color = attributeColor)
            }

            Language.CSS -> when {
                token.startsWith("/*") -> SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)
                first == '"' || first == '\'' -> SpanStyle(color = stringColor)
                first == '@' -> SpanStyle(color = keywordColor)
                first == '#' || first.isDigit() -> SpanStyle(color = numberColor)
                else -> SpanStyle(color = attributeColor)
            }

            Language.JS -> when {
                token.startsWith("//") || token.startsWith("/*") ->
                    SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)
                first == '"' || first == '\'' || first == '`' -> SpanStyle(color = stringColor)
                first.isDigit() -> SpanStyle(color = numberColor)
                else -> SpanStyle(color = keywordColor, fontWeight = FontWeight.Medium)
            }
        }
    }
}
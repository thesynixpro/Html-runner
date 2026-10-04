package com.aprax.htmlrun.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/** Regex based highlighter. It never changes the text, only the colors. */
object SyntaxHighlighter {

    private val darkPalette = Palette(
        comment = Color(0xFF6B7385),
        tag = Color(0xFF7EE787),
        attribute = Color(0xFF79C0FF),
        string = Color(0xFFFFA657),
        keyword = Color(0xFFFF7B9C),
        number = Color(0xFFD2A8FF),
        plain = Color(0xFFC8D1E0),
    )

    private val lightPalette = Palette(
        comment = Color(0xFF6A737D),
        tag = Color(0xFF116329),
        attribute = Color(0xFF0550AE),
        string = Color(0xFF953800),
        keyword = Color(0xFFCF222E),
        number = Color(0xFF6639BA),
        plain = Color(0xFF1F2328),
    )

    private val htmlPattern = Regex(
        "<!--[\\s\\S]*?-->|<!DOCTYPE[^>]*>|</?[A-Za-z][\\w:.-]*|\"[^\"\\n]*\"|'[^'\\n]*'|[A-Za-z-]+(?==)"
    )

    private val cssPattern = Regex(
        "/\\*[\\s\\S]*?\\*/|\"[^\"\\n]*\"|'[^'\\n]*'|@[A-Za-z-]+|#[0-9A-Fa-f]{3,8}\\b|" +
            "\\b\\d+(?:\\.\\d+)?[a-z%]*\\b|--[A-Za-z-]+"
    )

    private val jsPattern = Regex(
        "//[^\\n]*|/\\*[\\s\\S]*?\\*/|`[^`]*`|\"[^\"\\n]*\"|'[^'\\n]*'|" +
            "\\b(?:const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|" +
            "new|class|extends|super|this|typeof|instanceof|in|of|delete|void|try|catch|finally|" +
            "throw|async|await|yield|import|export|from|default|null|undefined|true|false|NaN)\\b|" +
            "\\b\\d+(?:\\.\\d+)?\\b"
    )

    private val markdownPattern = Regex(
        "^#{1,6} .*$|\\*\\*[^*]+\\*\\*|`[^`]+`|\\[[^\\]]*]\\([^)]*\\)"
    )

    fun highlight(
        text: String,
        language: Language,
        dark: Boolean = true,
        search: String = "",
    ): AnnotatedString {
        val palette = if (dark) darkPalette else lightPalette
        val matchHighlight = if (dark) Color(0x33FFC107) else Color(0x33FF9500)

        return buildAnnotatedString {
            pushStyle(SpanStyle(color = palette.plain))
            val pattern = patternFor(language)
            var last = 0
            pattern.findAll(text).forEach { match ->
                val start = match.range.first
                if (start > last) append(text.substring(last, start))
                pushStyle(styleFor(match.value, language, palette))
                append(match.value)
                pop()
                last = match.range.last + 1
            }
            if (last < text.length) append(text.substring(last))
            pop()

            if (search.isNotEmpty()) {
                var from = 0
                while (from <= text.length - search.length) {
                    val index = text.indexOf(search, from)
                    if (index < 0) break
                    addStyle(
                        SpanStyle(background = matchHighlight),
                        index,
                        index + search.length,
                    )
                    from = index + search.length
                }
            }
        }
    }

    private fun patternFor(language: Language): Regex = when (language) {
        Language.HTML -> htmlPattern
        Language.CSS -> cssPattern
        Language.JS -> jsPattern
        Language.MARKDOWN -> markdownPattern
        Language.PLAIN -> Regex("^$")
    }

    private fun styleFor(token: String, language: Language, palette: Palette): SpanStyle {
        val first = token.firstOrNull() ?: return SpanStyle()
        return when (language) {
            Language.HTML -> when {
                token.startsWith("<!--") -> SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic)
                token.startsWith("<!") -> SpanStyle(color = palette.comment)
                first == '<' -> SpanStyle(color = palette.tag)
                first == '"' || first == '\'' -> SpanStyle(color = palette.string)
                else -> SpanStyle(color = palette.attribute)
            }

            Language.CSS -> when {
                token.startsWith("/*") -> SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic)
                token.startsWith("--") -> SpanStyle(color = palette.attribute)
                first == '"' || first == '\'' -> SpanStyle(color = palette.string)
                first == '@' -> SpanStyle(color = palette.keyword)
                first == '#' || first.isDigit() -> SpanStyle(color = palette.number)
                else -> SpanStyle(color = palette.attribute)
            }

            Language.JS -> when {
                token.startsWith("//") || token.startsWith("/*") ->
                    SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic)
                first == '"' || first == '\'' || first == '`' -> SpanStyle(color = palette.string)
                first.isDigit() -> SpanStyle(color = palette.number)
                else -> SpanStyle(color = palette.keyword, fontWeight = FontWeight.Medium)
            }

            Language.MARKDOWN -> when {
                token.startsWith("#") -> SpanStyle(color = palette.keyword, fontWeight = FontWeight.Bold)
                first == '`' -> SpanStyle(color = palette.string)
                first == '*' -> SpanStyle(color = palette.number, fontWeight = FontWeight.Bold)
                else -> SpanStyle(color = palette.attribute)
            }

            Language.PLAIN -> SpanStyle()
        }
    }

    private data class Palette(
        val comment: Color,
        val tag: Color,
        val attribute: Color,
        val string: Color,
        val keyword: Color,
        val number: Color,
        val plain: Color,
    )
}

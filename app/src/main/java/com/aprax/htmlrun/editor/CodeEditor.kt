package com.aprax.htmlrun.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aprax.htmlrun.data.AppSettings
import com.aprax.htmlrun.data.EditorFont

private const val MAX_HIGHLIGHTED_CHARS = 200_000
private const val CHARACTER_WIDTH_RATIO = 0.62f
private val FallbackLineHeight = 20.dp
private val EditorTopPadding = 10.dp

/**
 * Multi line code editor with an optional line number gutter, syntax highlighting and
 * automatic indentation. Word wrap is optional, the horizontal scroll keeps long lines
 * readable when wrapping is turned off.
 */
@Composable
fun CodeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: Language,
    settings: AppSettings,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onRunShortcut: () -> Unit = {},
) {
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val density = LocalDensity.current

    val lineHeight = (settings.fontSizeSp + 6).sp
    val fontFamily = remember(settings.editorFont) { settings.editorFont.fontFamily() }

    var lineHeights by remember { mutableStateOf(emptyList<Float>()) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewportHeight by remember { mutableStateOf(0) }

    val logicalLines = remember(value.text) { value.text.count { it == '\n' } + 1 }
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val longestLine = remember(value.text) {
        value.text.lineSequence().maxOfOrNull { it.length } ?: 0
    }

    LaunchedEffect(textLayout, value.selection, logicalLines) {
        val layout = textLayout ?: return@LaunchedEffect
        val cursor = value.selection.start.coerceIn(0, layout.layoutInput.text.length)
        val rect = layout.getCursorRect(cursor)
        val viewport = viewportHeight.toFloat()
        if (rect.top < verticalScroll.value) {
            verticalScroll.scrollTo(rect.top.toInt())
        } else if (rect.bottom > verticalScroll.value + viewport) {
            verticalScroll.scrollTo((rect.bottom - viewport).coerceAtLeast(0f).toInt())
        }
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(verticalScroll)
            .onSizeChanged { viewportHeight = it.height }
    ) {
        Row(
            modifier = Modifier
                .then(if (settings.wordWrap) Modifier.fillMaxWidth() else Modifier.horizontalScroll(horizontalScroll))
                .then(
                    if (settings.wordWrap) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.width(contentWidth(longestLine, settings.fontSizeSp, settings.showLineNumbers))
                    }
                )
        ) {
            if (settings.showLineNumbers) {
                Column(
                    modifier = Modifier
                        .width(52.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = EditorTopPadding)
                ) {
                    for (index in 0 until logicalLines) {
                        val height = lineHeights.getOrNull(index)
                            ?.let { with(density) { it.toDp() } }
                            ?: with(density) { lineHeight.toDp() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height),
                            contentAlignment = Alignment.TopEnd,
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = (settings.fontSizeSp - 2).sp,
                                lineHeight = lineHeight,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End,
                                modifier = Modifier.padding(end = 10.dp),
                            )
                        }
                    }
                }
            }

            BasicTextField(
                value = value,
                onValueChange = { onValueChange(applyIndent(it, settings.indentSize)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = EditorTopPadding, end = 14.dp, bottom = EditorTopPadding)
                    .onPreviewKeyEvent { event ->
                        val control = event.isCtrlPressed || event.isMetaPressed
                        when {
                            event.key == Key.Enter && control -> {
                                onRunShortcut()
                                true
                            }

                            control && event.key == Key.Z && !event.isShiftPressed -> {
                                onUndo()
                                true
                            }

                            control && (event.key == Key.Z || event.key == Key.Y) -> {
                                onRedo()
                                true
                            }

                            event.key == Key.Tab -> {
                                onValueChange(indentSelection(value, event.isShiftPressed, settings.indentSize))
                                true
                            }

                            event.key == Key.Backspace -> {
                                val next = removeTrailingIndent(value, settings.indentSize)
                                if (next.text != value.text) {
                                    onValueChange(next)
                                    true
                                } else {
                                    false
                                }
                            }

                            else -> false
                        }
                    },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = settings.fontSizeSp.sp,
                    lineHeight = lineHeight,
                    fontFamily = fontFamily,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions.Default,
                visualTransformation = VisualTransformation { annotated ->
                    val source = annotated.text
                    val styled = if (source.length <= MAX_HIGHLIGHTED_CHARS) {
                        SyntaxHighlighter.highlight(
                            text = source,
                            language = language,
                            dark = dark,
                            search = searchQuery,
                        )
                    } else {
                        SyntaxHighlighter.highlight(source, Language.PLAIN, dark, searchQuery)
                    }
                    TransformedText(styled, OffsetMapping.Identity)
                },
                onTextLayout = { layout ->
                    textLayout = layout
                    lineHeights = layout.logicalLineHeights(value.text)
                },
            )
        }
    }
}

private val MinimumContentWidth = 280.dp

private fun contentWidth(longestLine: Int, fontSizeSp: Int, gutter: Boolean): Dp {
    val textWidth = (longestLine.coerceAtMost(600) * CHARACTER_WIDTH_RATIO * fontSizeSp).dp
    return (textWidth + if (gutter) 90.dp else 40.dp).coerceAtLeast(MinimumContentWidth)
}

private fun EditorFont.fontFamily(): FontFamily = when (this) {
    EditorFont.MONOSPACE -> FontFamily.Monospace
    EditorFont.SANS -> FontFamily.SansSerif
    EditorFont.SERIF -> FontFamily.Serif
}

private fun Color.luminanceIsDark(): Boolean = (red * 0.299f + green * 0.587f + blue * 0.114f) < 0.5f

/** Height in pixels of every logical line, soft wrapped rows included. */
private fun TextLayoutResult.logicalLineHeights(source: String): List<Float> {
    if (lineCount == 0) return emptyList()
    val heights = ArrayList<Float>(lineCount)
    var start = getLineTop(0)
    for (line in 0 until lineCount) {
        val bottom = getLineBottom(line)
        val offset = getLineStart(line)
        if (offset == 0 || source.getOrNull(offset - 1) == '\n') {
            if (heights.isNotEmpty()) {
                heights[heights.lastIndex] = bottom - start
                start = bottom
            }
            heights.add(bottom - start)
        } else {
            heights[heights.lastIndex] = bottom - start
            start = bottom
        }
    }
    return heights
}

/** Keeps the indentation of the previous line when Enter is pressed. */
private fun applyIndent(new: TextFieldValue, indentSize: Int): TextFieldValue {
    val selection = new.selection
    if (new.composition != null) return new
    if (!selection.collapsed) return new
    val cursor = selection.start
    if (cursor <= 0 || new.text.getOrNull(cursor - 1) != '\n') return new

    val lineStart = new.text.lastIndexOf('\n', cursor - 2).let { if (it < 0) 0 else it + 1 }
    val previousLine = new.text.substring(lineStart, cursor - 1)
    val indent = previousLine.takeWhile { it == ' ' || it == '\t' }
    val opens = opensBlock(previousLine)
    if (indent.isEmpty() && !opens) return new

    val addition = indent + if (opens) " ".repeat(indentSize.coerceIn(2, 8)) else ""
    val caret = cursor + addition.length
    return new.copy(
        text = new.text.substring(0, cursor) + addition + new.text.substring(cursor),
        selection = TextRange(caret),
        composition = null,
    )
}

private fun opensBlock(line: String): Boolean {
    val trimmed = line.trimEnd()
    return trimmed.endsWith("{") ||
        trimmed.endsWith(">") ||
        trimmed.endsWith("[") ||
        trimmed.endsWith("(")
}

private fun indentSelection(value: TextFieldValue, shift: Boolean, indentSize: Int): TextFieldValue {
    val text = value.text
    val unit = " ".repeat(indentSize.coerceIn(2, 8))
    val blockStart = text.lastIndexOf('\n', value.selection.min - 1).let { if (it < 0) 0 else it + 1 }
    val blockEnd = value.selection.max
    val block = text.substring(blockStart, blockEnd)
    val pattern = if (shift) Regex("^[ \\t]{1,$indentSize}", RegexOption.MULTILINE) else Regex("^", RegexOption.MULTILINE)
    val updated = block.replace(pattern, if (shift) "" else unit)
    val result = text.substring(0, blockStart) + updated + text.substring(blockEnd)
    val delta = updated.length - block.length
    val selection = if (shift || value.selection.collapsed) {
        TextRange(blockStart, blockStart + updated.length)
    } else {
        TextRange(
            (value.selection.min + delta).coerceAtLeast(blockStart),
            (value.selection.max + delta).coerceAtLeast(blockStart),
        )
    }
    return value.copy(text = result, selection = selection, composition = null)
}

private fun removeTrailingIndent(value: TextFieldValue, indentSize: Int): TextFieldValue {
    val selection = value.selection
    if (!selection.collapsed) return value
    val cursor = selection.start
    if (cursor == 0) return value
    val text = value.text
    val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it < 0) 0 else it + 1 }
    val before = text.substring(lineStart, cursor)
    if (before.isEmpty() || before.any { it != ' ' && it != '\t' }) return value
    val keep = (before.length - indentSize.coerceIn(2, 8)).coerceAtLeast(0)
    return value.copy(
        text = text.substring(0, lineStart) + before.substring(0, keep) + text.substring(cursor),
        selection = TextRange(lineStart + keep),
        composition = null,
    )
}

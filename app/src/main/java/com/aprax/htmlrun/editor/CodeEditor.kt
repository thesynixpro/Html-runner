package com.aprax.htmlrun.editor

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EditorBackground = Color(0xFF0F1115)
private val GutterBackground = Color(0xFF11141B)
private val GutterColor = Color(0xFF4A5468)
private val PlainTextColor = Color(0xFFC8D1E0)
private val CursorColor = Color(0xFF4F9DFF)

private val EditorFontSize = 13.sp
private val EditorLineHeight = 20.sp
private val FallbackLineHeight = 20.dp
private val EditorTopPadding = 10.dp

/**
 * Multi line code editor with a line number gutter and syntax highlighting.
 * Long lines soft wrap, and the gutter follows the measured height of every logical
 * line so the numbers stay aligned with the text.
 */
@Composable
fun CodeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: Language,
    modifier: Modifier = Modifier,
    onRunShortcut: () -> Unit = {},
) {
    val verticalScroll = rememberScrollState()
    val density = LocalDensity.current

    var lineHeights by remember { mutableStateOf(emptyList<Float>()) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewportHeight by remember { mutableStateOf(0) }

    val logicalLines = value.text.count { it == '\n' } + 1

    LaunchedEffect(textLayout, value.selection) {
        val layout = textLayout ?: return@LaunchedEffect
        val cursor = value.selection.start.coerceIn(0, layout.layoutInput.text.length)
        val rect = layout.getCursorRect(cursor)
        val viewport = viewportHeight.toFloat()
        if (rect.top < verticalScroll.value) {
            verticalScroll.scrollTo(rect.top.toInt())
        } else if (rect.bottom > verticalScroll.value + viewport) {
            verticalScroll.scrollTo((rect.bottom - viewport).toInt())
        }
    }

    Box(
        modifier = modifier
            .background(EditorBackground)
            .verticalScroll(verticalScroll)
            .onSizeChanged { viewportHeight = it.height }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .width(46.dp)
                    .background(GutterBackground)
                    .padding(vertical = EditorTopPadding)
            ) {
                for (index in 0 until logicalLines) {
                    val height = lineHeights.getOrNull(index)
                        ?.let { with(density) { it.toDp() } }
                        ?: FallbackLineHeight
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            color = GutterColor,
                            fontSize = EditorFontSize,
                            lineHeight = EditorLineHeight,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            BasicTextField(
                value = value,
                onValueChange = { onValueChange(applyIndent(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = EditorTopPadding, end = 12.dp, bottom = EditorTopPadding)
                    .onPreviewKeyEvent { event ->
                        val control = event.isCtrlPressed || event.isMetaPressed
                        when {
                            event.key == Key.Enter && control -> {
                                onRunShortcut()
                                true
                            }

                            event.key == Key.Tab -> {
                                onValueChange(indentSelection(value, shift = event.isShiftPressed))
                                true
                            }

                            event.key == Key.Backspace -> {
                                val next = removeTrailingIndent(value)
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
                    color = PlainTextColor,
                    fontSize = EditorFontSize,
                    lineHeight = EditorLineHeight,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(CursorColor),
                keyboardOptions = KeyboardOptions.Default,
                visualTransformation = VisualTransformation { annotated ->
                    TransformedText(SyntaxHighlighter.highlight(annotated.text, language), OffsetMapping.Identity)
                },
                onTextLayout = { layout ->
                    textLayout = layout
                    lineHeights = layout.logicalLineHeights(value.text)
                }
            )
        }
    }
}

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
private fun applyIndent(new: TextFieldValue): TextFieldValue {
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

    val addition = indent + if (opens) "  " else ""
    val caret = cursor + addition.length
    return new.copy(
        text = new.text.substring(0, cursor) + addition + new.text.substring(cursor),
        selection = TextRange(caret),
        composition = null
    )
}

private fun opensBlock(line: String): Boolean {
    val trimmed = line.trimEnd()
    return trimmed.endsWith("{") ||
        trimmed.endsWith(">") ||
        trimmed.endsWith("[") ||
        trimmed.endsWith("(")
}

private fun indentSelection(value: TextFieldValue, shift: Boolean): TextFieldValue {
    val text = value.text
    val blockStart = text.lastIndexOf('\n', value.selection.min - 1).let { if (it < 0) 0 else it + 1 }
    val blockEnd = value.selection.max
    val block = text.substring(blockStart, blockEnd)
    val pattern = if (shift) Regex("^[ ]{1,2}", RegexOption.MULTILINE) else Regex("^", RegexOption.MULTILINE)
    val updated = block.replace(pattern, if (shift) "" else "  ")
    val result = text.substring(0, blockStart) + updated + text.substring(blockEnd)
    val delta = updated.length - block.length
    val selection = if (shift || value.selection.collapsed) {
        TextRange(blockStart + (updated.length - block.length.coerceAtMost(0)), blockStart + updated.length)
    } else {
        TextRange(value.selection.min + delta, value.selection.max + delta)
    }
    return value.copy(text = result, selection = selection, composition = null)
}

private fun removeTrailingIndent(value: TextFieldValue): TextFieldValue {
    val selection = value.selection
    if (!selection.collapsed) return value
    val cursor = selection.start
    if (cursor == 0) return value
    val text = value.text
    val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it < 0) 0 else it + 1 }
    val before = text.substring(lineStart, cursor)
    if (before.isEmpty() || before.any { it != ' ' && it != '\t' }) return value
    val keep = (before.length - 2).coerceAtLeast(0)
    return value.copy(
        text = text.substring(0, lineStart) + before.substring(0, keep) + text.substring(cursor),
        selection = TextRange(lineStart + keep),
        composition = null
    )
}
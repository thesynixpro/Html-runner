package com.aprax.htmlrun.editor

import androidx.compose.ui.text.input.TextFieldValue

private const val COALESCE_WINDOW_MS = 600L
private const val LIMIT = 250

/** Undo and redo history for the open file. */
class EditorHistory {

    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var lastChangeAt = 0L

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun reset(value: TextFieldValue) {
        undoStack.clear()
        redoStack.clear()
        lastChangeAt = 0L
        undoStack.addLast(value)
    }

    fun onChange(previous: TextFieldValue, current: TextFieldValue, now: Long) {
        if (previous.text == current.text) return
        val typedAtEnd = current.text.length == previous.text.length + 1 &&
            current.text.startsWith(previous.text)
        val merge = typedAtEnd && now - lastChangeAt < COALESCE_WINDOW_MS && undoStack.size > 1
        if (!merge) {
            undoStack.addLast(previous)
            while (undoStack.size > LIMIT) undoStack.removeFirst()
        }
        redoStack.clear()
        lastChangeAt = now
    }

    fun undo(current: TextFieldValue): TextFieldValue? {
        if (undoStack.size < 2) return null
        val target = undoStack.removeLast()
        redoStack.addLast(current)
        return target
    }

    fun redo(current: TextFieldValue): TextFieldValue? {
        val target = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        return target
    }
}

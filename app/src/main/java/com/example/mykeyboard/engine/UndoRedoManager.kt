package com.example.mykeyboard.engine

import android.view.inputmethod.InputConnection
import java.util.Stack

data class TextSnapshot(
    val text: String,
    val cursor: Int
)

class UndoRedoManager(private val maxHistory: Int = 30) {

    private val undoStack = Stack<TextSnapshot>()
    private val redoStack = Stack<TextSnapshot>()
    private var isPerformingUndoRedo = false

    fun recordState(text: String, cursor: Int) {
        if (isPerformingUndoRedo) return

        if (undoStack.isNotEmpty()) {
            val top = undoStack.peek()
            if (top.text == text && top.cursor == cursor) {
                return
            }
        }

        undoStack.push(TextSnapshot(text, cursor))
        if (undoStack.size > maxHistory) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.size > 1

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo(ic: InputConnection): Boolean {
        if (!canUndo()) return false

        isPerformingUndoRedo = true
        val current = undoStack.pop()
        redoStack.push(current)

        val previous = undoStack.peek()
        applySnapshot(ic, current, previous)
        isPerformingUndoRedo = false
        return true
    }

    fun redo(ic: InputConnection): Boolean {
        if (!canRedo()) return false

        isPerformingUndoRedo = true
        val target = redoStack.pop()
        val current = if (undoStack.isNotEmpty()) undoStack.peek() else TextSnapshot("", 0)
        undoStack.push(target)

        applySnapshot(ic, current, target)
        isPerformingUndoRedo = false
        return true
    }

    private fun applySnapshot(ic: InputConnection, from: TextSnapshot, to: TextSnapshot) {
        // Select all existing text and replace with new snapshot
        ic.beginBatchEdit()
        try {
            // Delete surrounding text and replace
            ic.deleteSurroundingText(1000, 1000)
            ic.commitText(to.text, 1)
            val pos = to.cursor.coerceIn(0, to.text.length)
            ic.setSelection(pos, pos)
        } catch (_: Exception) {}
        ic.endBatchEdit()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}

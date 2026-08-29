package com.example.mykeyboard.engine

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class ClipboardItem(
    val id: Long,
    val text: String,
    val timestamp: Long,
    var isPinned: Boolean
)

class ClipboardHistoryManager(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_CLIPBOARD (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TEXT TEXT UNIQUE NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_PINNED INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CLIPBOARD")
        onCreate(db)
    }

    fun addClip(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || clean.length > 5000) return

        try {
            val db = writableDatabase
            val now = System.currentTimeMillis()

            db.execSQL(
                """
                INSERT INTO $TABLE_CLIPBOARD ($COL_TEXT, $COL_TIMESTAMP, $COL_PINNED)
                VALUES (?, ?, 0)
                ON CONFLICT($COL_TEXT) DO UPDATE SET
                    $COL_TIMESTAMP = ?
                """.trimIndent(),
                arrayOf<Any>(clean, now, now)
            )

            // Limit unpinned items to 40
            db.execSQL(
                """
                DELETE FROM $TABLE_CLIPBOARD
                WHERE $COL_PINNED = 0 AND $COL_ID NOT IN (
                    SELECT $COL_ID FROM $TABLE_CLIPBOARD
                    WHERE $COL_PINNED = 0
                    ORDER BY $COL_TIMESTAMP DESC
                    LIMIT 40
                )
                """.trimIndent()
            )
        } catch (_: Exception) {}
    }

    fun getAllClips(): List<ClipboardItem> {
        val list = mutableListOf<ClipboardItem>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_CLIPBOARD,
                arrayOf(COL_ID, COL_TEXT, COL_TIMESTAMP, COL_PINNED),
                null, null, null, null,
                "$COL_PINNED DESC, $COL_TIMESTAMP DESC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    list.add(
                        ClipboardItem(
                            id = it.getLong(0),
                            text = it.getString(1),
                            timestamp = it.getLong(2),
                            isPinned = it.getInt(3) == 1
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun togglePin(id: Long) {
        try {
            val db = writableDatabase
            db.execSQL(
                "UPDATE $TABLE_CLIPBOARD SET $COL_PINNED = 1 - $COL_PINNED WHERE $COL_ID = ?",
                arrayOf(id)
            )
        } catch (_: Exception) {}
    }

    fun deleteClip(id: Long) {
        try {
            val db = writableDatabase
            db.delete(TABLE_CLIPBOARD, "$COL_ID = ?", arrayOf(id.toString()))
        } catch (_: Exception) {}
    }

    fun clearUnpinned() {
        try {
            val db = writableDatabase
            db.delete(TABLE_CLIPBOARD, "$COL_PINNED = 0", null)
        } catch (_: Exception) {}
    }

    companion object {
        private const val DB_NAME = "my_keyboard_clipboard.db"
        private const val DB_VERSION = 1
        private const val TABLE_CLIPBOARD = "clipboard_history"
        private const val COL_ID = "id"
        private const val COL_TEXT = "text"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_PINNED = "is_pinned"
    }
}

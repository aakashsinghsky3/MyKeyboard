package com.example.mykeyboard.engine

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserDictionaryDb(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_WORDS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_WORD TEXT UNIQUE NOT NULL,
                $COL_FREQ INTEGER DEFAULT 1,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
        )
        preseedCustomWords(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        preseedCustomWords(db)
    }

    private fun preseedCustomWords(db: SQLiteDatabase) {
        try {
            val now = System.currentTimeMillis()
            db.execSQL("INSERT OR IGNORE INTO $TABLE_WORDS ($COL_WORD, $COL_FREQ, $COL_TIMESTAMP) VALUES ('anshika', 80, $now)")
            db.execSQL("INSERT OR IGNORE INTO $TABLE_WORDS ($COL_WORD, $COL_FREQ, $COL_TIMESTAMP) VALUES ('akriti', 80, $now)")
        } catch (_: Exception) {}
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WORDS")
        onCreate(db)
    }

    fun learnWord(word: String) {
        val clean = word.trim().lowercase()
        if (clean.length < 2 || clean.any { it.isDigit() }) return

        try {
            val db = writableDatabase
            db.execSQL(
                """
                INSERT INTO $TABLE_WORDS ($COL_WORD, $COL_FREQ, $COL_TIMESTAMP)
                VALUES (?, 1, ?)
                ON CONFLICT($COL_WORD) DO UPDATE SET
                    $COL_FREQ = $COL_FREQ + 1,
                    $COL_TIMESTAMP = ?
                """.trimIndent(),
                arrayOf<Any>(clean, System.currentTimeMillis(), System.currentTimeMillis())
            )
        } catch (_: Exception) {}
    }

    fun addCustomWord(word: String) {
        val clean = word.trim()
        if (clean.isEmpty()) return
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_WORD, clean.lowercase())
                put(COL_FREQ, 100) // High frequency for explicit adds
                put(COL_TIMESTAMP, System.currentTimeMillis())
            }
            db.insertWithOnConflict(TABLE_WORDS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (_: Exception) {}
    }

    fun getLearnedWords(): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_WORDS,
                arrayOf(COL_WORD, COL_FREQ),
                null, null, null, null,
                "$COL_FREQ DESC",
                "200"
            )
            cursor.use {
                while (it.moveToNext()) {
                    val word = it.getString(0)
                    val freq = it.getInt(1)
                    result[word] = freq
                }
            }
        } catch (_: Exception) {}
        return result
    }

    fun deleteWord(word: String) {
        try {
            val db = writableDatabase
            db.delete(TABLE_WORDS, "$COL_WORD = ?", arrayOf(word.lowercase()))
        } catch (_: Exception) {}
    }

    companion object {
        private const val DATABASE_NAME = "my_keyboard_user_dict.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_WORDS = "user_words"
        private const val COL_ID = "id"
        private const val COL_WORD = "word"
        private const val COL_FREQ = "frequency"
        private const val COL_TIMESTAMP = "timestamp"
    }
}

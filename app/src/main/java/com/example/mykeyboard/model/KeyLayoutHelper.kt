package com.example.mykeyboard.model

enum class KeyType {
    CHARACTER,
    SHIFT,
    BACKSPACE,
    SPACE,
    ENTER,
    MODE_CHANGE,
    EMOJI,
    SETTINGS,
    COMMA,
    PERIOD,
    SPACER
}

enum class ShiftState {
    UNSHIFTED,
    SHIFTED_ONCE,
    CAPS_LOCKED
}

enum class KeyboardMode {
    ALPHA,
    SYMBOLS_1,
    SYMBOLS_2,
    EMOJI,
    DIALPAD
}

data class KeyModel(
    val primaryText: String,
    val shiftText: String = primaryText.uppercase(),
    val altText: String = "",
    val popupChars: List<String> = emptyList(),
    val type: KeyType = KeyType.CHARACTER,
    val weight: Float = 1.0f,
    val iconResId: Int = 0
)

object KeyLayoutHelper {
    fun getAlphaRows(isNumberRowEnabled: Boolean): List<List<KeyModel>> {
        val rows = mutableListOf<List<KeyModel>>()

        // Optional Number Row
        if (isNumberRowEnabled) {
            val numRow = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map {
                KeyModel(primaryText = it, popupChars = LONG_PRESS_MAP[it] ?: emptyList())
            }
            rows.add(numRow)
        }

        // Row 1: q w e r t y u i o p
        val r1Chars = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val r1Alt = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val r1 = r1Chars.mapIndexed { idx, char ->
            val alt = r1Alt[idx]
            KeyModel(
                primaryText = char,
                altText = alt,
                popupChars = getPopupCharsForKey(char, alt)
            )
        }
        rows.add(r1)

        // Row 2: a s d f g h j k l
        val r2Chars = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val r2Alt = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")")
        val r2 = r2Chars.mapIndexed { idx, char ->
            val alt = r2Alt[idx]
            KeyModel(
                primaryText = char,
                altText = alt,
                popupChars = getPopupCharsForKey(char, alt)
            )
        }
        rows.add(r2)

        // Row 3: [SHIFT] z x c v b n m [BACKSPACE]
        val r3 = mutableListOf<KeyModel>()
        r3.add(KeyModel(primaryText = "SHIFT", type = KeyType.SHIFT, weight = 1.5f))
        val r3Chars = listOf("z", "x", "c", "v", "b", "n", "m")
        val r3Alt = listOf("*", "\"", "'", ":", ";", "!", "?")
        r3Chars.forEachIndexed { idx, char ->
            val alt = r3Alt[idx]
            r3.add(
                KeyModel(
                    primaryText = char,
                    altText = alt,
                    popupChars = getPopupCharsForKey(char, alt)
                )
            )
        }
        r3.add(KeyModel(primaryText = "DEL", type = KeyType.BACKSPACE, weight = 1.5f))
        rows.add(r3)

        // Row 4: [?123] [EMOJI] [COMMA] [SPACE] [PERIOD] [ENTER]
        val r4 = listOf(
            KeyModel(primaryText = "?123", type = KeyType.MODE_CHANGE, weight = 1.5f),
            KeyModel(primaryText = "😀", type = KeyType.EMOJI, weight = 1.1f),
            KeyModel(primaryText = ",", type = KeyType.COMMA, weight = 1.0f),
            KeyModel(primaryText = " ", type = KeyType.SPACE, weight = 4.0f),
            KeyModel(
                primaryText = ".",
                shiftText = ".",
                popupChars = listOf("...", "!", "?", ",", "-", "@"),
                type = KeyType.PERIOD,
                weight = 1.0f
            ),
            KeyModel(primaryText = "ENTER", type = KeyType.ENTER, weight = 1.5f)
        )
        rows.add(r4)

        return rows
    }

    fun getSymbols1Rows(): List<List<KeyModel>> {
        val rows = mutableListOf<List<KeyModel>>()

        // Row 0: 1 2 3 4 5 6 7 8 9 0
        rows.add(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map {
            KeyModel(primaryText = it, popupChars = LONG_PRESS_MAP[it] ?: emptyList())
        })

        // Row 1: @ # $ % & - + ( ) /
        val r1Chars = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
        rows.add(r1Chars.map {
            KeyModel(primaryText = it, popupChars = LONG_PRESS_MAP[it] ?: emptyList())
        })

        // Row 2: = * " ' : ; ! ? _ \
        val r2Chars = listOf("=", "*", "\"", "'", ":", ";", "!", "?", "_", "\\")
        rows.add(r2Chars.map {
            KeyModel(primaryText = it, popupChars = LONG_PRESS_MAP[it] ?: emptyList())
        })

        // Row 3: [=\<] ~ ` | < > { } [ ] [BACKSPACE]
        val r3 = mutableListOf<KeyModel>()
        r3.add(KeyModel(primaryText = "=\\<", type = KeyType.MODE_CHANGE, weight = 1.4f))
        listOf("~", "`", "|", "<", ">", "{", "}", "[", "]").forEach {
            r3.add(KeyModel(primaryText = it, popupChars = LONG_PRESS_MAP[it] ?: emptyList()))
        }
        r3.add(KeyModel(primaryText = "DEL", type = KeyType.BACKSPACE, weight = 1.4f))
        rows.add(r3)

        // Row 4: [ABC] [EMOJI] [COMMA] [SPACE] [PERIOD] [ENTER]
        rows.add(
            listOf(
                KeyModel(primaryText = "ABC", type = KeyType.MODE_CHANGE, weight = 1.4f),
                KeyModel(primaryText = "😀", type = KeyType.EMOJI, weight = 1.1f),
                KeyModel(primaryText = ",", type = KeyType.COMMA, weight = 1.0f),
                KeyModel(primaryText = " ", type = KeyType.SPACE, weight = 4.0f),
                KeyModel(primaryText = ".", popupChars = listOf("...", "!", "?"), type = KeyType.PERIOD, weight = 1.0f),
                KeyModel(primaryText = "ENTER", type = KeyType.ENTER, weight = 1.4f)
            )
        )

        return rows
    }

    fun getSymbols2Rows(): List<List<KeyModel>> {
        val rows = mutableListOf<List<KeyModel>>()

        // Row 0: 1 2 3 4 5 6 7 8 9 0
        rows.add(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map {
            KeyModel(primaryText = it, popupChars = LONG_PRESS_MAP[it] ?: emptyList())
        })

        // Row 1: ~ ` | • √ π ÷ × ¶ ∆
        val r1Chars = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆")
        rows.add(r1Chars.map {
            KeyModel(primaryText = it, popupChars = LONG_PRESS_MAP[it] ?: emptyList())
        })

        // Row 2: £ ¢ € ¥ ₳ § © ® ™ ✓
        val r2Chars = listOf("£", "¢", "€", "¥", "₳", "§", "©", "®", "™", "✓")
        rows.add(r2Chars.map {
            KeyModel(primaryText = it, popupChars = LONG_PRESS_MAP[it] ?: emptyList())
        })

        // Row 3: [?123] ^ ° = { } \ < > [BACKSPACE]
        val r3 = mutableListOf<KeyModel>()
        r3.add(KeyModel(primaryText = "?123", type = KeyType.MODE_CHANGE, weight = 1.4f))
        listOf("^", "°", "=", "{", "}", "\\", "<", ">").forEach {
            r3.add(KeyModel(primaryText = it, popupChars = LONG_PRESS_MAP[it] ?: emptyList()))
        }
        r3.add(KeyModel(primaryText = "DEL", type = KeyType.BACKSPACE, weight = 1.4f))
        rows.add(r3)

        // Row 4: [ABC] [EMOJI] [COMMA] [SPACE] [PERIOD] [ENTER]
        rows.add(
            listOf(
                KeyModel(primaryText = "ABC", type = KeyType.MODE_CHANGE, weight = 1.4f),
                KeyModel(primaryText = "😀", type = KeyType.EMOJI, weight = 1.1f),
                KeyModel(primaryText = ",", type = KeyType.COMMA, weight = 1.0f),
                KeyModel(primaryText = " ", type = KeyType.SPACE, weight = 4.0f),
                KeyModel(primaryText = ".", popupChars = listOf("...", "!", "?"), type = KeyType.PERIOD, weight = 1.0f),
                KeyModel(primaryText = "ENTER", type = KeyType.ENTER, weight = 1.4f)
            )
        )

        return rows
    }

    fun getDialpadRows(): List<List<KeyModel>> {
        val rows = mutableListOf<List<KeyModel>>()

        // Row 1: 1, 2, 3
        rows.add(
            listOf(
                KeyModel(primaryText = "", type = KeyType.SPACER, weight = 1.0f),
                KeyModel(primaryText = "1", altText = "", popupChars = listOf("¹"), weight = 2.0f),
                KeyModel(primaryText = "2", altText = "", popupChars = listOf("²"), weight = 2.0f),
                KeyModel(primaryText = "3", altText = "", popupChars = listOf("³"), weight = 2.0f),
                KeyModel(primaryText = "", type = KeyType.SPACER, weight = 1.0f)
            )
        )

        // Row 2: 4, 5, 6
        rows.add(
            listOf(
                KeyModel(primaryText = "", type = KeyType.SPACER, weight = 1.0f),
                KeyModel(primaryText = "4", altText = "", popupChars = listOf("⁴"), weight = 2.0f),
                KeyModel(primaryText = "5", altText = "", popupChars = listOf("⁵"), weight = 2.0f),
                KeyModel(primaryText = "6", altText = "", popupChars = listOf("⁶"), weight = 2.0f),
                KeyModel(primaryText = "", type = KeyType.SPACER, weight = 1.0f)
            )
        )

        // Row 3: 7, 8, 9
        rows.add(
            listOf(
                KeyModel(primaryText = "", type = KeyType.SPACER, weight = 1.0f),
                KeyModel(primaryText = "7", altText = "", popupChars = listOf("⁷"), weight = 2.0f),
                KeyModel(primaryText = "8", altText = "", popupChars = listOf("⁸"), weight = 2.0f),
                KeyModel(primaryText = "9", altText = "", popupChars = listOf("⁹"), weight = 2.0f),
                KeyModel(primaryText = "", type = KeyType.SPACER, weight = 1.0f)
            )
        )

        // Row 4: *, 0 (+), #
        rows.add(
            listOf(
                KeyModel(primaryText = "", type = KeyType.SPACER, weight = 1.0f),
                KeyModel(primaryText = "*", altText = "", weight = 2.0f),
                KeyModel(primaryText = "0", altText = "+", popupChars = listOf("+"), weight = 2.0f),
                KeyModel(primaryText = "#", altText = "", weight = 2.0f),
                KeyModel(primaryText = "", type = KeyType.SPACER, weight = 1.0f)
            )
        )

        // Row 5: [Spacer] [ABC] [SPACE] [DEL] [Spacer]
        rows.add(
            listOf(
                KeyModel(primaryText = "", type = KeyType.SPACER, weight = 1.0f),
                KeyModel(primaryText = "ABC", type = KeyType.MODE_CHANGE, weight = 2.0f),
                KeyModel(primaryText = " ", type = KeyType.SPACE, weight = 2.0f),
                KeyModel(primaryText = "DEL", type = KeyType.BACKSPACE, weight = 2.0f),
                KeyModel(primaryText = "", type = KeyType.SPACER, weight = 1.0f)
            )
        )

        return rows
    }

    private val LONG_PRESS_MAP = mapOf(
        "a" to listOf("à", "á", "â", "ä", "æ", "ã", "å", "ā"),
        "c" to listOf("ç", "ć", "č"),
        "e" to listOf("è", "é", "ê", "ë", "ē", "ė", "ę"),
        "i" to listOf("î", "ï", "í", "ī", "į", "ì"),
        "l" to listOf("ł"),
        "n" to listOf("ñ", "ń"),
        "o" to listOf("ô", "ö", "ò", "ó", "œ", "ø", "ō", "õ"),
        "s" to listOf("ß", "ś", "š"),
        "u" to listOf("û", "ü", "ù", "ú", "ū"),
        "y" to listOf("ÿ"),
        "z" to listOf("ž", "ź", "ż"),
        "1" to listOf("1", "½", "⅓", "¼", "⅛", "¹"),
        "2" to listOf("2", "⅔", "²"),
        "3" to listOf("3", "¾", "⅜", "³"),
        "4" to listOf("4", "⁴"),
        "5" to listOf("5", "⅝", "⁵"),
        "6" to listOf("6", "⁶"),
        "7" to listOf("7", "⅞", "⁷"),
        "8" to listOf("8", "⁸"),
        "9" to listOf("9", "⁹"),
        "0" to listOf("0", "ⁿ", "∅", "⁰")
    )

    fun getPopupCharsForKey(char: String, altText: String): List<String> {
        val result = mutableListOf<String>()
        if (altText.isNotEmpty()) {
            result.add(altText)
        }
        val accents = LONG_PRESS_MAP[char] ?: emptyList()
        accents.forEach {
            if (!result.contains(it)) {
                result.add(it)
            }
        }
        return result
    }
}

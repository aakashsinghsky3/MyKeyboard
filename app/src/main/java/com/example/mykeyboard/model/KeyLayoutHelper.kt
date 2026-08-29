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
    PERIOD
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
    EMOJI
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

    private val LONG_PRESS_MAP = mapOf(
        "a" to listOf("à", "á", "â", "ä", "æ", "ã", "å", "ā", "ª"),
        "c" to listOf("ç", "ć", "č"),
        "e" to listOf("è", "é", "ê", "ë", "ē", "ė", "ę"),
        "i" to listOf("ì", "í", "î", "ï", "ī", "į"),
        "l" to listOf("ł"),
        "n" to listOf("ñ", "ń"),
        "o" to listOf("ò", "ó", "ô", "ö", "õ", "ø", "ō", "œ"),
        "s" to listOf("ß", "ś", "š", "$"),
        "u" to listOf("ù", "ú", "û", "ü", "ū", "ų"),
        "y" to listOf("ÿ", "ý"),
        "z" to listOf("ź", "ż", "ž"),
        "0" to listOf("º", "⁰", "∅"),
        "1" to listOf("¹", "½", "⅓", "¼", "⅛"),
        "2" to listOf("²", "⅔"),
        "3" to listOf("³", "¾", "⅜"),
        "4" to listOf("⁴"),
        "5" to listOf("⁵", "⅝"),
        "6" to listOf("⁶"),
        "7" to listOf("⁷", "⅞"),
        "8" to listOf("⁸"),
        "9" to listOf("⁹"),
        "$" to listOf("€", "£", "¥", "₹", "¢", "₱", "₩"),
        "?" to listOf("¿"),
        "!" to listOf("¡"),
        "%" to listOf("‰"),
        "-" to listOf("—", "–", "·"),
        "\"" to listOf("“", "”", "«", "»", "„"),
        "'" to listOf("‘", "’", "‚", "`")
    )

    fun getAlphaRows(includeNumberRow: Boolean): List<List<KeyModel>> {
        val rows = mutableListOf<List<KeyModel>>()

        if (includeNumberRow) {
            val numRow = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map {
                KeyModel(
                    primaryText = it,
                    shiftText = it,
                    popupChars = LONG_PRESS_MAP[it] ?: emptyList()
                )
            }
            rows.add(numRow)
        }

        // Row 1: q w e r t y u i o p
        val r1Chars = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val altRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        rows.add(r1Chars.mapIndexed { idx, ch ->
            KeyModel(
                primaryText = ch,
                altText = altRow1.getOrElse(idx) { "" },
                popupChars = (LONG_PRESS_MAP[ch] ?: emptyList())
            )
        })

        // Row 2: a s d f g h j k l
        val r2Chars = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val altRow2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")")
        rows.add(r2Chars.mapIndexed { idx, ch ->
            KeyModel(
                primaryText = ch,
                altText = altRow2.getOrElse(idx) { "" },
                popupChars = (LONG_PRESS_MAP[ch] ?: emptyList())
            )
        })

        // Row 3: [SHIFT] z x c v b n m [BACKSPACE]
        val r3 = mutableListOf<KeyModel>()
        r3.add(KeyModel(primaryText = "SHIFT", type = KeyType.SHIFT, weight = 1.5f))
        val r3Chars = listOf("z", "x", "c", "v", "b", "n", "m")
        val altRow3 = listOf("*", "\"", "'", ":", ";", "!", "?")
        r3.addAll(r3Chars.mapIndexed { idx, ch ->
            KeyModel(
                primaryText = ch,
                altText = altRow3.getOrElse(idx) { "" },
                popupChars = (LONG_PRESS_MAP[ch] ?: emptyList())
            )
        })
        r3.add(KeyModel(primaryText = "DEL", type = KeyType.BACKSPACE, weight = 1.5f))
        rows.add(r3)

        // Row 4: [?123] [EMOJI] [COMMA] [SPACE] [PERIOD] [ENTER]
        val r4 = listOf(
            KeyModel(primaryText = "?123", type = KeyType.MODE_CHANGE, weight = 1.4f),
            KeyModel(primaryText = "😀", type = KeyType.EMOJI, weight = 1.1f),
            KeyModel(primaryText = ",", shiftText = ",", type = KeyType.COMMA, weight = 1.0f),
            KeyModel(primaryText = " ", shiftText = " ", type = KeyType.SPACE, weight = 4.0f),
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
                KeyModel(primaryText = "ENTER", type = KeyType.ENTER, weight = 1.5f)
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
        rows.add(listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆").map {
            KeyModel(primaryText = it)
        })

        // Row 2: £ € ¥ ¢ ^ ° = § « »
        rows.add(listOf("£", "€", "¥", "¢", "^", "°", "=", "§", "«", "»").map {
            KeyModel(primaryText = it)
        })

        // Row 3: [?123] © ® ™ ✓ [ ] < > % [BACKSPACE]
        val r3 = mutableListOf<KeyModel>()
        r3.add(KeyModel(primaryText = "?123", type = KeyType.MODE_CHANGE, weight = 1.4f))
        listOf("©", "®", "™", "✓", "[", "]", "<", ">", "%").forEach {
            r3.add(KeyModel(primaryText = it))
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
                KeyModel(primaryText = ".", type = KeyType.PERIOD, weight = 1.0f),
                KeyModel(primaryText = "ENTER", type = KeyType.ENTER, weight = 1.5f)
            )
        )

        return rows
    }
}

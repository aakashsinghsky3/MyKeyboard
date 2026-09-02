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
    LANGUAGE_SWITCH,
    SPACER
}

enum class KeyboardLanguage(
    val id: String,
    val displayName: String,
    val spaceLabel: String
) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "हिंदी", "हिंदी"),
    HARYANVI("hr", "हरियाणवी", "हरियाणवी");

    companion object {
        fun fromId(id: String?): KeyboardLanguage {
            return values().firstOrNull { it.id == id } ?: ENGLISH
        }
    }
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
    fun getAlphaRows(isNumberRowEnabled: Boolean, language: KeyboardLanguage = KeyboardLanguage.ENGLISH): List<List<KeyModel>> {
        return when (language) {
            KeyboardLanguage.HINDI -> getHindiRows(isNumberRowEnabled)
            KeyboardLanguage.HARYANVI -> getHaryanviRows(isNumberRowEnabled)
            KeyboardLanguage.ENGLISH -> getEnglishRows(isNumberRowEnabled)
        }
    }

    private fun getEnglishRows(isNumberRowEnabled: Boolean): List<List<KeyModel>> {
        val rows = mutableListOf<List<KeyModel>>()

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

        // Row 2: a s d f g h j k l (with 0.5f side spacers)
        val r2List = mutableListOf<KeyModel>()
        r2List.add(KeyModel(primaryText = "", type = KeyType.SPACER, weight = 0.5f))
        val r2Chars = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val r2Alt = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")")
        r2Chars.forEachIndexed { idx, char ->
            val alt = r2Alt[idx]
            r2List.add(
                KeyModel(
                    primaryText = char,
                    altText = "",
                    popupChars = getPopupCharsForKey(char, alt)
                )
            )
        }
        r2List.add(KeyModel(primaryText = "", type = KeyType.SPACER, weight = 0.5f))
        rows.add(r2List)

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
                    altText = "",
                    popupChars = getPopupCharsForKey(char, alt)
                )
            )
        }
        r3.add(KeyModel(primaryText = "DEL", type = KeyType.BACKSPACE, weight = 1.5f))
        rows.add(r3)

        // Row 4: [?123] [🌐] [COMMA] [SPACE] [PERIOD] [ENTER]
        val r4 = listOf(
            KeyModel(primaryText = "?123", type = KeyType.MODE_CHANGE, weight = 1.25f),
            KeyModel(primaryText = "🌐", type = KeyType.LANGUAGE_SWITCH, weight = 1.0f),
            KeyModel(primaryText = ",", type = KeyType.COMMA, weight = 0.9f),
            KeyModel(primaryText = "English", type = KeyType.SPACE, weight = 4.0f),
            KeyModel(
                primaryText = ".",
                shiftText = ".",
                popupChars = listOf("...", "!", "?", ",", "-", "@"),
                type = KeyType.PERIOD,
                weight = 0.9f
            ),
            KeyModel(primaryText = "ENTER", type = KeyType.ENTER, weight = 1.5f)
        )
        rows.add(r4)

        return rows
    }

    fun getHindiRows(isNumberRowEnabled: Boolean): List<List<KeyModel>> {
        val rows = mutableListOf<List<KeyModel>>()

        if (isNumberRowEnabled) {
            val numRow = listOf("१", "२", "३", "४", "५", "६", "७", "८", "९", "०").map {
                KeyModel(primaryText = it)
            }
            rows.add(numRow)
        }

        // Row 1: क ख ग घ ङ च छ ज झ ञ
        val r1Chars = listOf("क", "ख", "ग", "घ", "ङ", "च", "छ", "ज", "झ", "ञ")
        val r1 = r1Chars.map { KeyModel(primaryText = it, popupChars = listOf(it, "${it}ा", "${it}ि", "${it}ी", "${it}ु", "${it}ू", "${it}े", "${it}ो")) }
        rows.add(r1)

        // Row 2: ट ठ ड ढ ण त थ द ध न
        val r2Chars = listOf("ट", "ठ", "ड", "ढ", "ण", "त", "थ", "द", "ध", "न")
        val r2 = r2Chars.map { KeyModel(primaryText = it, popupChars = listOf(it, "${it}ा", "${it}ि", "${it}ी", "${it}ु", "${it}ू", "${it}े", "${it}ो")) }
        rows.add(r2)

        // Row 3: [SHIFT] प फ ब भ म य र ल व [DEL]
        val r3 = mutableListOf<KeyModel>()
        r3.add(KeyModel(primaryText = "SHIFT", type = KeyType.SHIFT, weight = 1.5f))
        val r3Chars = listOf("प", "फ", "ब", "भ", "म", "य", "र", "ल", "व")
        r3Chars.forEach { r3.add(KeyModel(primaryText = it, popupChars = listOf(it, "${it}ा", "${it}ि", "${it}ी", "${it}ु", "${it}ू", "${it}े", "${it}ो"))) }
        r3.add(KeyModel(primaryText = "DEL", type = KeyType.BACKSPACE, weight = 1.5f))
        rows.add(r3)

        // Row 4: [?123] [🌐] [ श ] [ space (हिंदी) ] [ ह ] [Enter]
        val r4 = listOf(
            KeyModel(primaryText = "?123", type = KeyType.MODE_CHANGE, weight = 1.25f),
            KeyModel(primaryText = "🌐", type = KeyType.LANGUAGE_SWITCH, weight = 1.0f),
            KeyModel(primaryText = "श", type = KeyType.CHARACTER, weight = 0.9f),
            KeyModel(primaryText = "हिंदी", type = KeyType.SPACE, weight = 4.0f),
            KeyModel(primaryText = "ह", type = KeyType.CHARACTER, weight = 0.9f),
            KeyModel(primaryText = "ENTER", type = KeyType.ENTER, weight = 1.5f)
        )
        rows.add(r4)

        return rows
    }

    fun getHaryanviRows(isNumberRowEnabled: Boolean): List<List<KeyModel>> {
        val rows = mutableListOf<List<KeyModel>>()

        if (isNumberRowEnabled) {
            val numRow = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map {
                KeyModel(primaryText = it)
            }
            rows.add(numRow)
        }

        // Row 1: क ख ग घ च छ ज झ ट ठ
        val r1Chars = listOf("क", "ख", "ग", "घ", "च", "छ", "ज", "झ", "ट", "ठ")
        val r1 = r1Chars.map { KeyModel(primaryText = it, popupChars = listOf(it, "${it}ा", "${it}ि", "${it}ी", "${it}ु", "${it}ू", "${it}े", "${it}ो")) }
        rows.add(r1)

        // Row 2: ड ढ त थ द ध न प फ ब
        val r2Chars = listOf("ड", "ढ", "त", "थ", "द", "ध", "न", "प", "फ", "ब")
        val r2 = r2Chars.map { KeyModel(primaryText = it, popupChars = listOf(it, "${it}ा", "${it}ि", "${it}ी", "${it}ु", "${it}ू", "${it}े", "${it}ो")) }
        rows.add(r2)

        // Row 3: [SHIFT] भ म य र ल व श स ह [DEL]
        val r3 = mutableListOf<KeyModel>()
        r3.add(KeyModel(primaryText = "SHIFT", type = KeyType.SHIFT, weight = 1.5f))
        val r3Chars = listOf("भ", "म", "य", "र", "ल", "व", "श", "स", "ह")
        r3Chars.forEach { r3.add(KeyModel(primaryText = it, popupChars = listOf(it, "${it}ा", "${it}ि", "${it}ी", "${it}ु", "${it}ू", "${it}े", "${it}ो"))) }
        r3.add(KeyModel(primaryText = "DEL", type = KeyType.BACKSPACE, weight = 1.5f))
        rows.add(r3)

        // Row 4: [?123] [🌐] [ , ] [ space (हरियाणवी) ] [ . ] [Enter]
        val r4 = listOf(
            KeyModel(primaryText = "?123", type = KeyType.MODE_CHANGE, weight = 1.25f),
            KeyModel(primaryText = "🌐", type = KeyType.LANGUAGE_SWITCH, weight = 1.0f),
            KeyModel(primaryText = ",", type = KeyType.COMMA, weight = 0.9f),
            KeyModel(primaryText = "हरियाणवी", type = KeyType.SPACE, weight = 4.0f),
            KeyModel(primaryText = ".", type = KeyType.PERIOD, weight = 0.9f),
            KeyModel(primaryText = "ENTER", type = KeyType.ENTER, weight = 1.5f)
        )
        rows.add(r4)

        return rows
    }

    fun getSymbols1Rows(isNumberRowEnabled: Boolean = false): List<List<KeyModel>> {
        val rows = mutableListOf<List<KeyModel>>()

        if (isNumberRowEnabled) {
            rows.add(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map {
                KeyModel(primaryText = it, popupChars = LONG_PRESS_MAP[it] ?: emptyList())
            })
        }

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

    fun getSymbols2Rows(isNumberRowEnabled: Boolean = false): List<List<KeyModel>> {
        val rows = mutableListOf<List<KeyModel>>()

        if (isNumberRowEnabled) {
            rows.add(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map {
                KeyModel(primaryText = it, popupChars = LONG_PRESS_MAP[it] ?: emptyList())
            })
        }

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

        // Row 1: + | 1 | 2 | 3 | %
        rows.add(
            listOf(
                KeyModel(primaryText = "+", type = KeyType.CHARACTER, weight = 1.0f),
                KeyModel(primaryText = "1", popupChars = listOf("¹"), weight = 1.4f),
                KeyModel(primaryText = "2", popupChars = listOf("²"), weight = 1.4f),
                KeyModel(primaryText = "3", popupChars = listOf("³"), weight = 1.4f),
                KeyModel(primaryText = "%", type = KeyType.CHARACTER, weight = 1.0f)
            )
        )

        // Row 2: - | 4 | 5 | 6 | ␣
        rows.add(
            listOf(
                KeyModel(primaryText = "-", type = KeyType.CHARACTER, weight = 1.0f),
                KeyModel(primaryText = "4", popupChars = listOf("⁴"), weight = 1.4f),
                KeyModel(primaryText = "5", popupChars = listOf("⁵"), weight = 1.4f),
                KeyModel(primaryText = "6", popupChars = listOf("⁶"), weight = 1.4f),
                KeyModel(primaryText = " ", type = KeyType.SPACE, weight = 1.0f)
            )
        )

        // Row 3: * | 7 | 8 | 9 | ⌫
        rows.add(
            listOf(
                KeyModel(primaryText = "*", type = KeyType.CHARACTER, weight = 1.0f),
                KeyModel(primaryText = "7", popupChars = listOf("⁷"), weight = 1.4f),
                KeyModel(primaryText = "8", popupChars = listOf("⁸"), weight = 1.4f),
                KeyModel(primaryText = "9", popupChars = listOf("⁹"), weight = 1.4f),
                KeyModel(primaryText = "DEL", type = KeyType.BACKSPACE, weight = 1.0f)
            )
        )

        // Row 4: ABC | , | 0 | = | . | ENTER
        rows.add(
            listOf(
                KeyModel(primaryText = "ABC", type = KeyType.MODE_CHANGE, weight = 1.2f),
                KeyModel(primaryText = ",", type = KeyType.COMMA, weight = 0.8f),
                KeyModel(primaryText = "0", popupChars = listOf("+"), weight = 1.4f),
                KeyModel(primaryText = "=", type = KeyType.CHARACTER, weight = 1.0f),
                KeyModel(primaryText = ".", type = KeyType.PERIOD, weight = 0.6f),
                KeyModel(primaryText = "ENTER", type = KeyType.ENTER, weight = 1.2f)
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

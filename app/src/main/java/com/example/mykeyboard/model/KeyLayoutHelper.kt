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
    HINDI("hi", "Hindi", "हिंदी");

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
    fun getAlphaRows(
        isNumberRowEnabled: Boolean,
        language: KeyboardLanguage = KeyboardLanguage.ENGLISH,
        shiftState: ShiftState = ShiftState.UNSHIFTED
    ): List<List<KeyModel>> {
        return when (language) {
            KeyboardLanguage.HINDI -> getHindiRows(isNumberRowEnabled, shiftState)
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
            val alt = if (isNumberRowEnabled) "" else r1Alt[idx]
            KeyModel(
                primaryText = char,
                altText = alt,
                popupChars = getPopupCharsForKey(char, r1Alt[idx])
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

    fun getHindiRows(isNumberRowEnabled: Boolean, shiftState: ShiftState = ShiftState.UNSHIFTED): List<List<KeyModel>> {
        val rows = mutableListOf<List<KeyModel>>()

        if (isNumberRowEnabled) {
            val numRow = listOf("१", "२", "३", "४", "५", "६", "७", "८", "९", "०").map {
                KeyModel(primaryText = it)
            }
            rows.add(numRow)
        }

        if (shiftState != ShiftState.UNSHIFTED) {
            // VOWELS (अ से अः) & MATRAS MODE ON SHIFT
            // Row 1: अ आ इ ई उ ऊ ऋ ए ऐ ओ
            val r1Vowels = listOf("अ", "आ", "इ", "ई", "उ", "ऊ", "ऋ", "ए", "ऐ", "ओ")
            rows.add(r1Vowels.map { KeyModel(primaryText = it, popupChars = listOf(it, "ऑ", "ऍ", "ॐ")) })

            // Row 2: औ अं अः ा ि ी ु ू े ै
            val r2Matras = listOf("औ", "अं", "अः", "ा", "ि", "ी", "ु", "ू", "े", "ै")
            rows.add(r2Matras.map { KeyModel(primaryText = it) })

            // Row 3: [SHIFT] ो ौ ं ः ृ ॉ ॅ ऑ ऍ ॐ [DEL]
            val r3 = mutableListOf<KeyModel>()
            r3.add(KeyModel(primaryText = "अ/ा", type = KeyType.SHIFT, weight = 1.5f))
            val r3Extra = listOf("ो", "ौ", "ं", "ः", "ृ", "ॉ", "ॅ", "ऑ", "ऍ", "ॐ")
            r3Extra.forEach { r3.add(KeyModel(primaryText = it)) }
            r3.add(KeyModel(primaryText = "DEL", type = KeyType.BACKSPACE, weight = 1.5f))
            rows.add(r3)
        } else {
            // CONSONANTS MODE (क-ह) WITH MATRA POPUPS & ALT VOWELS
            // Row 1: क(अ) ख(आ) ग(इ) घ(ई) ङ(उ) च(ऊ) छ(ऋ) ज(ए) झ(ऐ) ञ(ओ)
            val r1Chars = listOf("क", "ख", "ग", "घ", "ङ", "च", "छ", "ज", "झ", "ञ")
            val r1Alt = listOf("अ", "आ", "इ", "ई", "उ", "ऊ", "ऋ", "ए", "ऐ", "ओ")
            val r1 = r1Chars.mapIndexed { idx, char ->
                val popups = when(char) {
                    "ज" -> listOf("ज", "ज़", "झ")
                    else -> listOf(char, "${char}ा", "${char}ि", "${char}ी", "${char}ु", "${char}ू", "${char}े", "${char}ै", "${char}ो", "${char}ौ", "${char}ं", "ा", "ि", "ी", "ु", "ू", "े", "ै", "ो", "ौ", "ं")
                }
                KeyModel(
                    primaryText = char,
                    altText = r1Alt[idx],
                    popupChars = popups
                )
            }
            rows.add(r1)

            // Row 2: ट(औ) ठ(अं) ड(अः) ढ(ा) ण(ि) त(ी) थ(ु) द(ू) ध(े) न(ै)
            val r2Chars = listOf("ट", "ठ", "ड", "ढ", "ण", "त", "थ", "द", "ध", "न")
            val r2Alt = listOf("औ", "अं", "अः", "ा", "ि", "ी", "ु", "ू", "े", "ै")
            val r2 = r2Chars.mapIndexed { idx, char ->
                val popups = when(char) {
                    "ड" -> listOf("ड", "ड़")
                    "ढ" -> listOf("ढ", "ढ़")
                    else -> listOf(char, "${char}ा", "${char}ि", "${char}ी", "${char}ु", "${char}ू", "${char}े", "${char}ै", "${char}ो", "${char}ौ", "${char}ं", "ा", "ि", "ी", "ु", "ू", "े", "ै", "ो", "ौ", "ं")
                }
                KeyModel(
                    primaryText = char,
                    altText = r2Alt[idx],
                    popupChars = popups
                )
            }
            rows.add(r2)

            // Row 3: [SHIFT] प फ ब भ म य र ल व श [DEL]
            val r3 = mutableListOf<KeyModel>()
            r3.add(KeyModel(primaryText = "अ/ा", type = KeyType.SHIFT, weight = 1.25f))
            val r3Chars = listOf("प", "फ", "ब", "भ", "म", "य", "र", "ल", "व", "श")
            val r3Alt = listOf("ो", "ौ", "ं", "ः", "ृ", "ॉ", "ॅ", "ऑ", "ऍ", "ष")
            r3Chars.forEachIndexed { idx, char ->
                val popups = when(char) {
                    "फ" -> listOf("फ", "फ़")
                    "श" -> listOf("श", "ष", "स", "श्र")
                    else -> listOf(char, "${char}ा", "${char}ि", "${char}ी", "${char}ु", "${char}ू", "${char}े", "${char}ै", "${char}ो", "${char}ौ", "${char}ं", "ा", "ि", "ी", "ु", "ू", "े", "ै", "ो", "ौ", "ं")
                }
                r3.add(
                    KeyModel(
                        primaryText = char,
                        altText = r3Alt[idx],
                        popupChars = popups
                    )
                )
            }
            r3.add(KeyModel(primaryText = "DEL", type = KeyType.BACKSPACE, weight = 1.25f))
            rows.add(r3)
        }

        // Row 4: [?123] [🌐] [ष] [स] [ह] [ space (हिंदी) ] [क्ष] [त्र] [ज्ञ] [ENTER]
        val r4 = listOf(
            KeyModel(primaryText = "?123", type = KeyType.MODE_CHANGE, weight = 1.25f),
            KeyModel(primaryText = "🌐", type = KeyType.LANGUAGE_SWITCH, weight = 1.0f),
            KeyModel(primaryText = "ष", type = KeyType.CHARACTER, weight = 0.8f),
            KeyModel(primaryText = "स", type = KeyType.CHARACTER, weight = 0.8f),
            KeyModel(primaryText = "ह", type = KeyType.CHARACTER, weight = 0.8f),
            KeyModel(primaryText = "हिंदी", type = KeyType.SPACE, weight = 3.5f),
            KeyModel(primaryText = "क्ष", type = KeyType.CHARACTER, weight = 0.8f),
            KeyModel(primaryText = "त्र", type = KeyType.CHARACTER, weight = 0.8f),
            KeyModel(primaryText = "ज्ञ", type = KeyType.CHARACTER, weight = 0.8f),
            KeyModel(primaryText = "ENTER", type = KeyType.ENTER, weight = 1.25f)
        )
        rows.add(r4)

        return rows
    }

    fun getHaryanviRows(isNumberRowEnabled: Boolean, shiftState: ShiftState = ShiftState.UNSHIFTED): List<List<KeyModel>> {
        val rows = mutableListOf<List<KeyModel>>()

        if (isNumberRowEnabled) {
            val numRow = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map {
                KeyModel(primaryText = it)
            }
            rows.add(numRow)
        }

        if (shiftState != ShiftState.UNSHIFTED) {
            // VOWELS (अ से अः) & MATRAS MODE ON SHIFT
            // Row 1: अ आ इ ई उ ऊ ऋ ए ऐ ओ
            val r1Vowels = listOf("अ", "आ", "इ", "ई", "उ", "ऊ", "ऋ", "ए", "ऐ", "ओ")
            rows.add(r1Vowels.map { KeyModel(primaryText = it, popupChars = listOf(it, "ऑ", "ऍ")) })

            // Row 2: औ अं अः ा ि ी ु ू े ै
            val r2Matras = listOf("औ", "अं", "अः", "ा", "ि", "ी", "ु", "ू", "े", "ै")
            rows.add(r2Matras.map { KeyModel(primaryText = it) })

            // Row 3: [SHIFT] ो ौ ं ः ृ ॉ ॅ ऑ ऍ [DEL]
            val r3 = mutableListOf<KeyModel>()
            r3.add(KeyModel(primaryText = "अ/ा", type = KeyType.SHIFT, weight = 1.5f))
            val r3Extra = listOf("ो", "ौ", "ं", "ः", "ृ", "ॉ", "ॅ", "ऑ", "ऍ")
            r3Extra.forEach { r3.add(KeyModel(primaryText = it)) }
            r3.add(KeyModel(primaryText = "DEL", type = KeyType.BACKSPACE, weight = 1.5f))
            rows.add(r3)
        } else {
            // CONSONANTS MODE (क-ह) WITH MATRA POPUPS & ALT VOWELS
            // Row 1: क(अ) ख(आ) ग(इ) घ(ई) च(उ) छ(ऊ) ज(ऋ) झ(ए) ट(ऐ) ठ(ओ)
            val r1Chars = listOf("क", "ख", "ग", "घ", "च", "छ", "ज", "झ", "ट", "ठ")
            val r1Alt = listOf("अ", "आ", "इ", "ई", "उ", "ऊ", "ऋ", "ए", "ऐ", "ओ")
            val r1 = r1Chars.mapIndexed { idx, char ->
                KeyModel(
                    primaryText = char,
                    altText = r1Alt[idx],
                    popupChars = listOf(char, "${char}ा", "${char}ि", "${char}ी", "${char}ु", "${char}ू", "${char}े", "${char}ै", "${char}ो", "${char}ौ", "${char}ं", "ा", "ि", "ी", "ु", "ू", "े", "ै", "ो", "ौ", "ं")
                )
            }
            rows.add(r1)

            // Row 2: ड(औ) ढ(अं) त(अः) थ(ा) द(ि) ध(ी) न(ु) प(ू) फ(े) ब(ै)
            val r2Chars = listOf("ड", "ढ", "त", "थ", "द", "ध", "न", "प", "फ", "ब")
            val r2Alt = listOf("औ", "अं", "अः", "ा", "ि", "ी", "ु", "ू", "े", "ै")
            val r2 = r2Chars.mapIndexed { idx, char ->
                KeyModel(
                    primaryText = char,
                    altText = r2Alt[idx],
                    popupChars = listOf(char, "${char}ा", "${char}ि", "${char}ी", "${char}ु", "${char}ू", "${char}े", "${char}ै", "${char}ो", "${char}ौ", "${char}ं", "ा", "ि", "ी", "ु", "ू", "े", "ै", "ो", "ौ", "ं")
                )
            }
            rows.add(r2)

            // Row 3: [SHIFT] भ म य र ल व श स ह [DEL]
            val r3 = mutableListOf<KeyModel>()
            r3.add(KeyModel(primaryText = "अ/ा", type = KeyType.SHIFT, weight = 1.5f))
            val r3Chars = listOf("भ", "म", "य", "र", "ल", "व", "श", "स", "ह")
            val r3Alt = listOf("ो", "ौ", "ं", "ः", "ृ", "ॉ", "ॅ", "ऑ", "ऍ")
            r3Chars.forEachIndexed { idx, char ->
                r3.add(
                    KeyModel(
                        primaryText = char,
                        altText = r3Alt[idx],
                        popupChars = listOf(char, "${char}ा", "${char}ि", "${char}ी", "${char}ु", "${char}ू", "${char}े", "${char}ै", "${char}ो", "${char}ौ", "${char}ं", "ा", "ि", "ी", "ु", "ू", "े", "ै", "ो", "ौ", "ं")
                    )
                )
            }
            r3.add(KeyModel(primaryText = "DEL", type = KeyType.BACKSPACE, weight = 1.5f))
            rows.add(r3)
        }

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

    fun getSymbols1Rows(
        isNumberRowEnabled: Boolean = false,
        language: KeyboardLanguage = KeyboardLanguage.ENGLISH
    ): List<List<KeyModel>> {
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

        val alphaLabel = if (language == KeyboardLanguage.HINDI) "अआइ" else "ABC"

        // Row 4: [ABC / अआइ] [EMOJI] [COMMA] [SPACE] [PERIOD] [ENTER]
        rows.add(
            listOf(
                KeyModel(primaryText = alphaLabel, type = KeyType.MODE_CHANGE, weight = 1.4f),
                KeyModel(primaryText = "😀", type = KeyType.EMOJI, weight = 1.1f),
                KeyModel(primaryText = ",", type = KeyType.COMMA, weight = 1.0f),
                KeyModel(primaryText = " ", type = KeyType.SPACE, weight = 4.0f),
                KeyModel(primaryText = ".", popupChars = listOf("...", "!", "?"), type = KeyType.PERIOD, weight = 1.0f),
                KeyModel(primaryText = "ENTER", type = KeyType.ENTER, weight = 1.4f)
            )
        )

        return rows
    }

    fun getSymbols2Rows(
        isNumberRowEnabled: Boolean = false,
        language: KeyboardLanguage = KeyboardLanguage.ENGLISH
    ): List<List<KeyModel>> {
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

        val alphaLabel = if (language == KeyboardLanguage.HINDI) "अआइ" else "ABC"

        // Row 4: [ABC / अआइ] [EMOJI] [COMMA] [SPACE] [PERIOD] [ENTER]
        rows.add(
            listOf(
                KeyModel(primaryText = alphaLabel, type = KeyType.MODE_CHANGE, weight = 1.4f),
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

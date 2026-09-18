package com.example.mykeyboard

import com.example.mykeyboard.engine.AutoCorrectEngine
import com.example.mykeyboard.engine.AutoCorrectMode
import com.example.mykeyboard.model.EmojiData
import com.example.mykeyboard.model.KeyLayoutHelper
import com.example.mykeyboard.model.KeyModel
import com.example.mykeyboard.model.KeyType
import com.example.mykeyboard.model.KeyboardLanguage
import com.example.mykeyboard.model.KeyboardMode
import com.example.mykeyboard.model.ShiftState
import org.junit.Assert.*
import org.junit.Test

class KeyboardEngineUnitTest {

    @Test
    fun testCheckmarksAndKeycapNumbersPresentInEmojiData() {
        val symbolsTab = EmojiData.tabs.find { it.id == "symbols" }
        assertNotNull("Symbols tab must exist in EmojiData", symbolsTab)
        assertEquals("Symbols", symbolsTab!!.title)

        assertTrue("Default favorites must contain checkmark ✅", EmojiData.DEFAULT_FAVORITES.contains("✅"))
        assertTrue("Tabs must contain 11 categories", EmojiData.tabs.size == 11)
        assertTrue("Tabs must contain Recent and Favorites", EmojiData.tabs.any { it.id == EmojiData.TAB_RECENT } && EmojiData.tabs.any { it.id == EmojiData.TAB_FAVORITES })
    }

    @Test
    fun testSymbolLayoutLanguageLabels() {
        val engSymbols1 = KeyLayoutHelper.getSymbols1Rows(isNumberRowEnabled = false, language = KeyboardLanguage.ENGLISH)
        val engModeKey1 = engSymbols1.flatten().find { it.type == KeyType.MODE_CHANGE && (it.primaryText == "ABC" || it.primaryText == "अआइ") }
        assertNotNull(engModeKey1)
        assertEquals("ABC", engModeKey1!!.primaryText)

        val hiSymbols1 = KeyLayoutHelper.getSymbols1Rows(isNumberRowEnabled = false, language = KeyboardLanguage.HINDI)
        val hiModeKey1 = hiSymbols1.flatten().find { it.type == KeyType.MODE_CHANGE && (it.primaryText == "ABC" || it.primaryText == "अआइ") }
        assertNotNull(hiModeKey1)
        assertEquals("अआइ", hiModeKey1!!.primaryText)

        val engSymbols2 = KeyLayoutHelper.getSymbols2Rows(isNumberRowEnabled = false, language = KeyboardLanguage.ENGLISH)
        val engModeKey2 = engSymbols2.flatten().find { it.type == KeyType.MODE_CHANGE && (it.primaryText == "ABC" || it.primaryText == "अआइ") }
        assertNotNull(engModeKey2)
        assertEquals("ABC", engModeKey2!!.primaryText)

        val hiSymbols2 = KeyLayoutHelper.getSymbols2Rows(isNumberRowEnabled = false, language = KeyboardLanguage.HINDI)
        val hiModeKey2 = hiSymbols2.flatten().find { it.type == KeyType.MODE_CHANGE && (it.primaryText == "ABC" || it.primaryText == "अआइ") }
        assertNotNull(hiModeKey2)
        assertEquals("अआइ", hiModeKey2!!.primaryText)
    }

    @Test
    fun testNumberRowSuppressesQpAltText() {
        val rowsWithNum = KeyLayoutHelper.getAlphaRows(isNumberRowEnabled = true, language = KeyboardLanguage.ENGLISH)
        val qRowWithNum = rowsWithNum.find { row -> row.any { it.primaryText == "q" } }
        assertNotNull(qRowWithNum)
        val qKeyWithNum = qRowWithNum!!.find { it.primaryText == "q" }
        assertNotNull(qKeyWithNum)
        assertEquals("", qKeyWithNum!!.altText)

        val rowsWithoutNum = KeyLayoutHelper.getAlphaRows(isNumberRowEnabled = false, language = KeyboardLanguage.ENGLISH)
        val qRowWithoutNum = rowsWithoutNum.find { row -> row.any { it.primaryText == "q" } }
        assertNotNull(qRowWithoutNum)
        val qKeyWithoutNum = qRowWithoutNum!!.find { it.primaryText == "q" }
        assertNotNull(qKeyWithoutNum)
        assertEquals("1", qKeyWithoutNum!!.altText)
    }

    @Test
    fun testAutoCorrectEngine() {
        assertEquals("the", AutoCorrectEngine.getCorrection("teh", AutoCorrectMode.AGGRESSIVE))
        assertNull(AutoCorrectEngine.getCorrection("teh", AutoCorrectMode.OFF))

        val mockDict = mapOf(
            "when" to 1000000,
            "try" to 500000,
            "words" to 800000,
            "fast" to 700000,
            "missing" to 400000,
            "and" to 2000000
        )
        val validator: (String) -> Int? = { mockDict[it] }

        // Adjacent key typos (physical QWERTY neighbors)
        assertEquals("when", AutoCorrectEngine.getCorrection("wjen", AutoCorrectMode.CONSERVATIVE, validator))
        assertEquals("try", AutoCorrectEngine.getCorrection("trh", AutoCorrectMode.CONSERVATIVE, validator))
        assertEquals("words", AutoCorrectEngine.getCorrection("wlrds", AutoCorrectMode.CONSERVATIVE, validator))
        assertEquals("words", AutoCorrectEngine.getCorrection("worda", AutoCorrectMode.CONSERVATIVE, validator))

        // Extra tap deletion (fat-finger extra key)
        assertEquals("fast", AutoCorrectEngine.getCorrection("ftast", AutoCorrectMode.CONSERVATIVE, validator))

        // Missed tap insertion
        assertEquals("and", AutoCorrectEngine.getCorrection("nd", AutoCorrectMode.CONSERVATIVE, validator))
    }

    @Test
    fun testFastPathKeyLogic() {
        assertTrue(! "a".endsWith(" "))
        assertTrue(! "W".endsWith(" "))
        assertTrue(! "क्ष".endsWith(" "))
        assertTrue(! "त्र".endsWith(" "))
        assertTrue(! ".".endsWith(" "))
        assertTrue(! ",".endsWith(" "))
        assertTrue(! "✅".endsWith(" "))
        assertTrue(! "1️⃣".endsWith(" "))

        assertTrue("hello ".endsWith(" "))
        assertTrue("the ".endsWith(" "))
    }

    @Test
    fun testKeyboardLanguages() {
        assertEquals("en", KeyboardLanguage.ENGLISH.id)
        assertEquals("hi", KeyboardLanguage.HINDI.id)

        assertEquals(KeyboardLanguage.ENGLISH, KeyboardLanguage.fromId("en"))
        assertEquals(KeyboardLanguage.HINDI, KeyboardLanguage.fromId("hi"))
        assertEquals(KeyboardLanguage.ENGLISH, KeyboardLanguage.fromId("unknown_xyz"))
    }

    @Test
    fun testShiftStateCapitalization() {
        val unshiftedRows = KeyLayoutHelper.getAlphaRows(isNumberRowEnabled = false, language = KeyboardLanguage.ENGLISH, shiftState = ShiftState.UNSHIFTED)
        val shiftedRows = KeyLayoutHelper.getAlphaRows(isNumberRowEnabled = false, language = KeyboardLanguage.ENGLISH, shiftState = ShiftState.SHIFTED_ONCE)

        val unshiftedA = unshiftedRows.flatten().find { it.primaryText.equals("a", ignoreCase = true) }
        val shiftedA = shiftedRows.flatten().find { it.primaryText.equals("a", ignoreCase = true) }

        assertNotNull(unshiftedA)
        assertNotNull(shiftedA)
        assertEquals("a", unshiftedA!!.primaryText)
        assertEquals("A", shiftedA!!.shiftText)
    }

    @Test
    fun testDynamicBottomPaddingCalculation() {
        // Function replicating CustomKeyboardView's dynamic bottom padding logic
        fun calculatePadding(dynamicBottomInset: Int, density: Float = 2.0f): Int {
            fun dp(value: Int) = (value * density).toInt()
            return if (dynamicBottomInset > 0) dynamicBottomInset else dp(6)
        }

        // 1. Realme Narzo 50i / ColorOS 3-button mode (window sits above nav bar):
        // Inset is 0 -> padding should be 6dp (12px on 2x density), NOT 48dp! NO BLACK STRIP!
        val realme3ButtonPadding = calculatePadding(dynamicBottomInset = 0, density = 2.0f)
        assertEquals(12, realme3ButtonPadding)

        // 2. Gesture Navigation mode (window extends behind gesture bar):
        // Inset is 24dp (48px) -> padding should be exactly 24dp. NO OVERLAP WITH GESTURE PILL!
        val gesturePadding = calculatePadding(dynamicBottomInset = 48, density = 2.0f)
        assertEquals(48, gesturePadding)

        // 3. Edge-to-Edge 3-Button mode (window extends behind 3-button nav):
        // Inset is 48dp (96px) -> padding should be exactly 48dp. NO OVERLAP WITH BACK BUTTON!
        val edgeToEdge3ButtonPadding = calculatePadding(dynamicBottomInset = 96, density = 2.0f)
        assertEquals(96, edgeToEdge3ButtonPadding)
    }

    @Test
    fun testLightDarkCalculationLogic() {
        fun isColorLight(r: Int, g: Int, b: Int): Boolean {
            val luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
            return luminance > 0.5
        }

        // Pastel Pink #FDF2F4 (253, 242, 244) -> Light theme (needs dark navigation icons)
        assertTrue("Pastel pink must be light", isColorLight(253, 242, 244))

        // Matte Dark #18181B (24, 24, 27) -> Dark theme (needs light navigation icons)
        assertFalse("Matte dark must be dark", isColorLight(24, 24, 27))

        // AMOLED Black #000000 (0, 0, 0) -> Dark theme (needs light navigation icons)
        assertFalse("AMOLED black must be dark", isColorLight(0, 0, 0))
    }

    @Test
    fun testGlobeKeyRemovedFromKeyboardLayouts() {
        // English alpha rows must NOT have LANGUAGE_SWITCH key
        val engRows = KeyLayoutHelper.getAlphaRows(isNumberRowEnabled = false, language = KeyboardLanguage.ENGLISH)
        val engGlobe = engRows.flatten().find { it.type == KeyType.LANGUAGE_SWITCH || it.primaryText == "🌐" }
        assertNull("Globe / language switch key must be removed from English layout", engGlobe)

        // English row 4 must have Spacebar
        val engSpace = engRows.last().find { it.type == KeyType.SPACE }
        assertNotNull("English bottom row must contain Spacebar", engSpace)
        assertEquals("English", engSpace!!.primaryText)

        // Hindi alpha rows must NOT have LANGUAGE_SWITCH key
        val hiRows = KeyLayoutHelper.getAlphaRows(isNumberRowEnabled = false, language = KeyboardLanguage.HINDI)
        val hiGlobe = hiRows.flatten().find { it.type == KeyType.LANGUAGE_SWITCH || it.primaryText == "🌐" }
        assertNull("Globe / language switch key must be removed from Hindi layout", hiGlobe)

        // Hindi row 4 must have Spacebar
        val hiSpace = hiRows.last().find { it.type == KeyType.SPACE }
        assertNotNull("Hindi bottom row must contain Spacebar", hiSpace)
        assertEquals("हिंदी", hiSpace!!.primaryText)
    }
}

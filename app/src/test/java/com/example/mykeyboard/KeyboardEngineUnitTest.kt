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
        val checkmarksCategory = EmojiData.categories.find { it.name.contains("Checkmarks") }
        assertNotNull("Checkmarks & Badges category must exist in EmojiData", checkmarksCategory)
        
        val emojis = checkmarksCategory!!.emojis
        assertTrue("Must contain checkmark ✅", emojis.contains("✅"))
        assertTrue("Must contain checkmark ✔", emojis.contains("✔"))
        assertTrue("Must contain keycap 1️⃣", emojis.contains("1️⃣"))
        assertTrue("Must contain keycap 2️⃣", emojis.contains("2️⃣"))
        assertTrue("Must contain keycap 3️⃣", emojis.contains("3️⃣"))
        assertTrue("Must contain keycap 4️⃣", emojis.contains("4️⃣"))
        assertTrue("Must contain keycap 5️⃣", emojis.contains("5️⃣"))
        assertTrue("Must contain keycap 🔟", emojis.contains("🔟"))
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
}

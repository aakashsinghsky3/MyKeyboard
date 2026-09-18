package com.example.mykeyboard.settings

import android.view.View
import android.widget.LinearLayout
import com.example.mykeyboard.R
import com.example.mykeyboard.engine.AutoCorrectMode
import com.example.mykeyboard.settings.SettingsUi.Type
import kotlin.math.roundToInt

/** Keyboard: layout & size, feedback, typing assistance, and gesture tips. */
class KeyboardPage(private val host: SettingsHost) : SettingsPage {
    private val ui = host.ui
    private val prefs = host.prefs
    private val ctx = ui.context
    private lateinit var vibrationBlock: View

    override val view: View = build()

    private fun build(): View {
        val (scroll, col) = ui.page()
        col.addView(ui.pageHeader("Keyboard", "Layout, size, feedback and typing help."))

        // ---- Layout & size ----
        col.addView(ui.sectionLabel("Layout & size"))
        val (layoutCard, layoutCol) = ui.card()
        layoutCol.addView(ui.switchRow(R.drawable.ic_numbers, "Number row", "Show 1–0 above the letters", prefs.isNumberRowEnabled) {
            prefs.isNumberRowEnabled = it
        }.first.root)
        layoutCol.addView(ui.divider())
        val (heightBlock, _) = ui.sliderBlock(R.drawable.ic_height, "Keyboard height", 85f, 125f, 5f, prefs.heightScale * 100f,
            format = { heightLabel(it) }) { prefs.heightScale = it / 100f }
        layoutCol.addView(heightBlock)
        layoutCol.addView(ui.divider())
        layoutCol.addView(ui.row(R.drawable.ic_space_bar, "Key spacing", "Visual gaps only — touch areas stay edge-to-edge").root)
        val spacings = listOf(0.6f, 1.0f, 1.4f)
        val current = spacings.indexOfFirst { kotlin.math.abs(it - prefs.keySpacingScale) < 0.05f }.let { if (it < 0) 1 else it }
        val (spacingSeg, _) = ui.segmented(listOf("Compact", "Normal", "Wide"), current) { i -> prefs.keySpacingScale = spacings[i] }
        layoutCol.addView(spacingSeg, ui.lp(bottom = ui.dp(16)).apply { marginStart = ui.dp(16); marginEnd = ui.dp(16) })
        ui.add(col, layoutCard, 4)

        // ---- Feedback ----
        col.addView(ui.sectionLabel("Feedback"))
        val (fbCard, fbCol) = ui.card()
        fbCol.addView(ui.switchRow(R.drawable.ic_vibration, "Vibrate on keypress", "Haptic feedback while typing", prefs.isHapticEnabled) {
            prefs.isHapticEnabled = it
            setVibrationEnabled(it)
        }.first.root)
        val (vib, _) = ui.sliderBlock(null, "Vibration strength", 5f, 60f, 5f, prefs.hapticDuration.toFloat().coerceIn(5f, 60f),
            format = { strengthLabel(it) }) { prefs.hapticDuration = it.roundToInt().toLong() }
        vibrationBlock = vib.apply { setPadding(ui.dp(56), 0, 0, 0) }
        fbCol.addView(vibrationBlock)
        fbCol.addView(ui.divider())
        fbCol.addView(ui.switchRow(R.drawable.ic_volume, "Sound on keypress", "Uses your system click volume", prefs.isSoundEnabled) {
            prefs.isSoundEnabled = it
        }.first.root)
        fbCol.addView(ui.divider())
        fbCol.addView(ui.switchRow(R.drawable.ic_touch, "Popup on keypress", "Show a larger letter above your finger", prefs.isPopupEnabled) {
            prefs.isPopupEnabled = it
        }.first.root)
        ui.add(col, fbCard, 4)
        setVibrationEnabled(prefs.isHapticEnabled)

        // ---- Typing ----
        col.addView(ui.sectionLabel("Typing & suggestions"))
        val (typeCard, typeCol) = ui.card()
        typeCol.addView(ui.row(R.drawable.ic_spellcheck, "Auto-correction", "How boldly misspelled words are fixed as you type").root)
        val modes = listOf(AutoCorrectMode.OFF, AutoCorrectMode.CONSERVATIVE, AutoCorrectMode.AGGRESSIVE)
        val (acSeg, _) = ui.segmented(listOf("Off", "Gentle", "Strong"), modes.indexOf(prefs.autoCorrectMode).coerceAtLeast(0)) { i ->
            prefs.autoCorrectMode = modes[i]
        }
        typeCol.addView(acSeg, ui.lp(bottom = ui.dp(16)).apply { marginStart = ui.dp(16); marginEnd = ui.dp(16) })
        typeCol.addView(ui.divider())
        typeCol.addView(ui.switchRow(R.drawable.ic_shift, "Auto-capitalization", "Capitalize the first word of each sentence", prefs.isAutoCapsEnabled) {
            prefs.isAutoCapsEnabled = it
        }.first.root)
        typeCol.addView(ui.divider())
        typeCol.addView(ui.row(R.drawable.ic_book, "Dictionary & languages", "Learned words, active language", ui.chevron()) {
            host.navigateTo(SettingsTab.LANGUAGE)
        }.root)
        ui.add(col, typeCard, 4)

        // ---- Tips ----
        col.addView(ui.sectionLabel("Gestures"))
        val (tipsCard, tipsCol) = ui.card(ui.dp(4))
        listOf(
            "Slide on the space bar" to "Move the cursor precisely",
            "Hold a key" to "Accents, numbers and symbols",
            "Hold the space bar" to "Switch language",
            "Hold a suggestion" to "Remove it from suggestions",
            "Hold backspace" to "Delete quickly"
        ).forEachIndexed { i, (t, d) ->
            if (i > 0) tipsCol.addView(ui.divider(16))
            tipsCol.addView(ui.row(null, t, d).root)
        }
        ui.add(col, tipsCard, 0)
        return scroll
    }

    private fun setVibrationEnabled(on: Boolean) {
        vibrationBlock.alpha = if (on) 1f else 0.4f
        fun deep(v: View) { v.isEnabled = on; if (v is LinearLayout) for (i in 0 until v.childCount) deep(v.getChildAt(i)) }
        deep(vibrationBlock)
    }

    private fun heightLabel(v: Float): String = when {
        v < 92f -> "Compact"
        v <= 105f -> "Standard"
        v <= 115f -> "Tall"
        else -> "Extra tall"
    } + " · ${v.roundToInt()}%"

    private fun strengthLabel(v: Float): String = when {
        v <= 15f -> "Light"
        v <= 35f -> "Medium"
        else -> "Strong"
    }
}

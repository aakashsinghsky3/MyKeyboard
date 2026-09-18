package com.example.mykeyboard.settings

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.mykeyboard.R
import com.example.mykeyboard.settings.SettingsUi.ButtonKind
import com.example.mykeyboard.settings.SettingsUi.Type
import com.example.mykeyboard.theme.ThemePreviewView
import com.example.mykeyboard.utils.KeyboardPreferences

/** Home: brand hero + readiness, 2-step setup, try-it field, current look, quick toggles, privacy. */
class HomePage(private val host: SettingsHost) : SettingsPage {
    private val ui = host.ui
    private val prefs = host.prefs
    private val ctx = ui.context

    private lateinit var statusPill: TextView
    private lateinit var setupCard: View
    private lateinit var step1Action: TextView
    private lateinit var step2Action: TextView
    private lateinit var step1Title: TextView
    private lateinit var step2Title: TextView
    private lateinit var lookPreview: ThemePreviewView
    private lateinit var lookName: TextView

    override val view: View = build()

    private fun build(): View {
        val (scroll, col) = ui.page()

        // ---- Hero (always brand-dark, in both app themes) ----
        val hero = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(20), ui.dp(20), ui.dp(20), ui.dp(20))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF2A2A2A.toInt(), 0xFF151515.toInt())).apply {
                cornerRadius = ui.dpf(26f)
            }
        }
        val heroTop = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        heroTop.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_launcher_foreground)
            scaleX = 1.7f; scaleY = 1.7f
            contentDescription = null
        }, LinearLayout.LayoutParams(ui.dp(64), ui.dp(64)))
        val heroTexts = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        heroTexts.addView(ui.text("Keyboard", Type.HEADLINE, ui.offWhite))
        heroTexts.addView(ui.text("Fast, private typing that looks the way you want.", Type.BODY_SMALL, 0xFFBDB7AF.toInt()), ui.lp(top = ui.dp(4)))
        heroTop.addView(heroTexts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = ui.dp(14) })
        hero.addView(heroTop)
        statusPill = ui.pill("", 0x33FFB020, 0xFFFFD166.toInt())
        hero.addView(statusPill, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = ui.dp(16) })
        ui.add(col, hero, 4)

        // ---- Setup ----
        col.addView(ui.sectionLabel("Get started"))
        val (setup, setupCol) = ui.card()
        step1Action = ui.button("Enable", ButtonKind.FILLED) { host.openInputMethodSettings() }
        val s1 = ui.row(null, "1  ·  Turn on Keyboard", "Allow it in Android’s keyboard settings", step1Action)
        step1Title = s1.title
        setupCol.addView(s1.root)
        setupCol.addView(ui.divider(16))
        step2Action = ui.button("Select", ButtonKind.FILLED) { host.showInputMethodPicker() }
        val s2 = ui.row(null, "2  ·  Make it your keyboard", "Choose Keyboard as the active input method", step2Action)
        step2Title = s2.title
        setupCol.addView(s2.root)
        setupCard = setup
        ui.add(col, setup, 4)

        // ---- Try it ----
        col.addView(ui.sectionLabel("Try it"))
        val (tryCard, tryCol) = ui.card(ui.dp(12))
        val input = EditText(ctx).apply {
            hint = "Tap here and start typing…"
            setHintTextColor(ui.onSurfaceVariant)
            setTextColor(ui.onSurface)
            textSize = 16f
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
            setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12))
            background = ui.rounded(ui.surfaceVariant, 16f)
        }
        tryCol.addView(input, ui.lp())
        val tryActions = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        tryActions.addView(ui.button("Clear", ButtonKind.TEXT) { input.setText("") })
        tryActions.addView(ui.button("Type", ButtonKind.TONAL, R.drawable.ic_keyboard) {
            input.requestFocus()
            @Suppress("DEPRECATION")
            (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = ui.dp(8) })
        tryCol.addView(tryActions, ui.lp(top = ui.dp(10)))
        ui.add(col, tryCard, 4)

        // ---- Current look ----
        col.addView(ui.sectionLabel("Your keyboard"))
        val (look, lookCol) = ui.card(ui.dp(12))
        lookPreview = ThemePreviewView(ctx).apply {
            clipToOutline = true
            background = ui.rounded(0, 16f)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        lookCol.addView(lookPreview, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(132)))
        val lookRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(ui.dp(4), ui.dp(12), 0, 0) }
        lookName = ui.text("", Type.TITLE_SMALL)
        lookRow.addView(lookName, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        lookRow.addView(ui.button("Customize", ButtonKind.TONAL, R.drawable.ic_palette) { host.navigateTo(SettingsTab.APPEARANCE) })
        lookCol.addView(lookRow)
        ui.add(col, look, 4)

        // ---- Quick settings ----
        col.addView(ui.sectionLabel("Quick settings"))
        val (quick, quickCol) = ui.card()
        quickCol.addView(ui.switchRow(R.drawable.ic_vibration, "Vibrate on keypress", null, prefs.isHapticEnabled) { prefs.isHapticEnabled = it }.first.root)
        quickCol.addView(ui.divider())
        quickCol.addView(ui.switchRow(R.drawable.ic_volume, "Sound on keypress", null, prefs.isSoundEnabled) { prefs.isSoundEnabled = it }.first.root)
        quickCol.addView(ui.divider())
        quickCol.addView(ui.row(R.drawable.ic_tune, "All keyboard settings", "Layout, feedback and typing", ui.chevron()) { host.navigateTo(SettingsTab.KEYBOARD) }.root)
        ui.add(col, quick, 4)

        // ---- Privacy ----
        col.addView(ui.sectionLabel("Privacy"))
        val (privacy, privacyCol) = ui.card()
        privacyCol.addView(ui.row(R.drawable.ic_shield, "100% offline & private",
            "No internet permission. What you type, learned words, clipboard and photos never leave this device.").root)
        ui.add(col, privacy, 0)

        return scroll
    }

    override fun onShown() {
        val enabled = host.isKeyboardEnabled()
        val selected = host.isKeyboardSelected()
        val ready = enabled && selected
        statusPill.text = if (ready) "●  Ready to type" else "●  Setup needed"
        statusPill.setTextColor(if (ready) 0xFF8EDBA8.toInt() else 0xFFFFD166.toInt())
        statusPill.background = ui.rounded(if (ready) 0x2E8EDBA8 else 0x33FFB020, 14f)

        styleStep(step1Action, step1Title, enabled, "Enable")
        styleStep(step2Action, step2Title, selected, "Select")
        step2Action.isEnabled = enabled
        step2Action.alpha = if (enabled) 1f else 0.5f
        refreshLook()
    }

    private fun styleStep(action: TextView, title: TextView, done: Boolean, label: String) {
        action.text = if (done) "Done" else label
        action.setTextColor(if (done) ui.success else ui.onPrimary)
        action.background = if (done) ui.rounded(ui.withAlpha(ui.success, 0.14f), 22f) else ui.ripple(ui.rounded(ui.primary, 22f), 22f)
        action.isClickable = !done
        title.alpha = if (done) 0.7f else 1f
    }

    private fun refreshLook() {
        val theme = prefs.theme
        lookPreview.setTheme(theme, prefs.keyPressEffectOverride, prefs.isHighContrastEnabled)
        lookName.text = theme.displayName
    }

    override fun onPreferenceChanged(key: String?) {
        if (key == null || key in KeyboardPreferences.APPEARANCE_KEYS) refreshLook()
    }
}

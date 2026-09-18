package com.example.mykeyboard.settings

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.example.mykeyboard.R
import com.example.mykeyboard.engine.ClipboardHistoryManager
import com.example.mykeyboard.settings.SettingsUi.ButtonKind
import com.example.mykeyboard.settings.SettingsUi.Type
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Advanced: clipboard, accessibility, privacy, system shortcuts, reset, about. */
class AdvancedPage(private val host: SettingsHost) : SettingsPage {
    private val ui = host.ui
    private val prefs = host.prefs
    private val ctx = ui.context
    private val io = SettingsIo.executor

    override val view: View = build()

    private fun build(): View {
        val (scroll, col) = ui.page()
        col.addView(ui.pageHeader("Advanced", "Clipboard, accessibility, privacy and resets."))

        col.addView(ui.sectionLabel("Clipboard"))
        val (clipCard, clipCol) = ui.card()
        clipCol.addView(ui.switchRow(R.drawable.ic_paste, "Clipboard history", "Keep copied text for one-tap paste and pinning", prefs.isClipboardHistoryEnabled) {
            prefs.isClipboardHistoryEnabled = it
        }.first.root)
        clipCol.addView(ui.divider())
        clipCol.addView(ui.row(R.drawable.ic_delete, "Clear clipboard history", "Removes every saved clip, including pinned") {
            confirm("Clear clipboard history?", "All saved and pinned clips will be deleted from this device.", "Clear") {
                io.execute { ClipboardHistoryManager(ctx.applicationContext).apply { clearAll(); close() } }
                Toast.makeText(ctx, "Clipboard history cleared", Toast.LENGTH_SHORT).show()
            }
        }.root)
        ui.add(col, clipCard, 4)

        col.addView(ui.sectionLabel("Accessibility"))
        val (a11yCard, a11yCol) = ui.card()
        a11yCol.addView(ui.switchRow(R.drawable.ic_contrast, "High contrast keys", "Stronger borders and label contrast", prefs.isHighContrastEnabled) {
            prefs.isHighContrastEnabled = it
        }.first.root)
        a11yCol.addView(ui.divider())
        a11yCol.addView(ui.switchRow(R.drawable.ic_motion, "Reduce motion", "Keys change state instantly — no bounce or ripple", prefs.isReduceMotionEnabled) {
            prefs.isReduceMotionEnabled = it
        }.first.root)
        a11yCol.addView(ui.divider())
        a11yCol.addView(ui.row(R.drawable.ic_text_size, "Text size & key look", "Adjust in Appearance", ui.chevron()) {
            host.navigateTo(SettingsTab.APPEARANCE)
        }.root)
        ui.add(col, a11yCard, 4)

        col.addView(ui.sectionLabel("Privacy"))
        val (privCard, privCol) = ui.card(ui.dp(16))
        val head = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(ui.iconTile(R.drawable.ic_shield), LinearLayout.LayoutParams(ui.dp(40), ui.dp(40)).apply { marginEnd = ui.dp(14) })
        head.addView(ui.text("Private by design", Type.TITLE_SMALL))
        privCol.addView(head)
        listOf(
            "No internet permission — the app cannot go online.",
            "Typing, learned words and clipboard stay on this device.",
            "Photo backgrounds are copied privately into the app.",
            "No analytics, tracking or ads."
        ).forEach { line ->
            val r = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP }
            r.addView(ImageView(ctx).apply { setImageResource(R.drawable.ic_check); setColorFilter(ui.success) }, LinearLayout.LayoutParams(ui.dp(18), ui.dp(18)).apply { marginEnd = ui.dp(10); topMargin = ui.dp(1) })
            r.addView(ui.text(line, Type.BODY_SMALL, ui.onSurfaceVariant), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            privCol.addView(r, ui.lp(top = ui.dp(12)))
        }
        ui.add(col, privCard, 4)

        col.addView(ui.sectionLabel("System"))
        val (sysCard, sysCol) = ui.card()
        sysCol.addView(ui.row(R.drawable.ic_settings, "Android keyboard settings", "Enable or disable keyboards", ui.chevron()) { host.openInputMethodSettings() }.root)
        sysCol.addView(ui.divider())
        sysCol.addView(ui.row(R.drawable.ic_keyboard, "Switch keyboard", "Pick the active input method", ui.chevron()) { host.showInputMethodPicker() }.root)
        ui.add(col, sysCard, 4)

        col.addView(ui.sectionLabel("Reset"))
        val (resetCard, resetCol) = ui.card(ui.dp(16))
        resetCol.addView(ui.text("Restore defaults if something doesn’t feel right. Learned words and clipboard are kept.", Type.BODY_SMALL, ui.onSurfaceVariant))
        val buttons = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        buttons.addView(ui.button("Appearance", ButtonKind.OUTLINED, R.drawable.ic_restore) {
            confirm("Reset appearance?", "Theme, key style, accent, spacing and text size return to defaults.", "Reset") {
                prefs.resetAppearance()
                host.rebuildPages()
            }
        })
        buttons.addView(ui.button("All settings", ButtonKind.DANGER) {
            confirm("Reset all settings?", "Every keyboard and app setting returns to its default. Learned words and clipboard history are not affected.", "Reset all") {
                prefs.resetAll()
                host.applyAppThemeMode("system")
                host.rebuildPages()
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = ui.dp(8) })
        resetCol.addView(buttons, ui.lp(top = ui.dp(14)))
        ui.add(col, resetCard, 4)

        // About
        val about = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, ui.dp(28), 0, ui.dp(8))
        }
        about.addView(ImageView(ctx).apply { setImageResource(R.drawable.ic_brand_logo); contentDescription = null }, LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)))
        about.addView(ui.text("Keyboard", Type.TITLE_SMALL).apply { gravity = Gravity.CENTER }, ui.lp(w = ViewGroup.LayoutParams.WRAP_CONTENT, top = ui.dp(10)))
        about.addView(ui.text("Version ${host.appVersion()}", Type.BODY_SMALL, ui.onSurfaceVariant), ui.lp(w = ViewGroup.LayoutParams.WRAP_CONTENT, top = ui.dp(2)))
        col.addView(about)
        return scroll
    }

    private fun confirm(title: String, message: String, action: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(ctx)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(action) { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

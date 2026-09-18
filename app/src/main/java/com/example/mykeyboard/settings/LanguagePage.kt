package com.example.mykeyboard.settings

import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.mykeyboard.R
import com.example.mykeyboard.engine.UserDictionaryDb
import com.example.mykeyboard.model.KeyboardLanguage
import com.example.mykeyboard.settings.SettingsUi.Type
import com.example.mykeyboard.utils.KeyboardPreferences
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Language & input: active language, how to switch, and the personal dictionary. */
class LanguagePage(private val host: SettingsHost) : SettingsPage {
    private val ui = host.ui
    private val prefs = host.prefs
    private val ctx = ui.context
    private val io = SettingsIo.executor
    private val main = Handler(Looper.getMainLooper())

    private val languageCards = ArrayList<Pair<KeyboardLanguage, MaterialCardView>>()
    private val radios = ArrayList<ImageView>()
    private lateinit var learnedRow: SettingsUi.Row
    private lateinit var blockedRow: SettingsUi.Row

    override val view: View = build()

    private fun build(): View {
        val (scroll, col) = ui.page()
        col.addView(ui.pageHeader("Language & input", "Choose your typing language and manage what the keyboard learns."))

        col.addView(ui.sectionLabel("Active language"))
        val details = mapOf(
            KeyboardLanguage.ENGLISH to Triple("English", "QWERTY · predictions & auto-correct", "Aa"),
            KeyboardLanguage.HINDI to Triple("हिंदी · Hindi", "Devanagari layout · Hindi & Haryanvi words", "अ")
        )
        KeyboardLanguage.values().forEach { lang ->
            val (name, desc, glyph) = details[lang] ?: Triple(lang.displayName, "", lang.displayName.take(2))
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14))
            }
            row.addView(ui.text(glyph, Type.TITLE, ui.onPrimaryContainer).apply {
                gravity = Gravity.CENTER
                background = ui.rounded(ui.primaryContainer, 14f)
            }, LinearLayout.LayoutParams(ui.dp(44), ui.dp(44)).apply { marginEnd = ui.dp(16) })
            val texts = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            texts.addView(ui.text(name, Type.TITLE_SMALL))
            texts.addView(ui.text(desc, Type.BODY_SMALL, ui.onSurfaceVariant), ui.lp(top = ui.dp(3)))
            row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val radio = ImageView(ctx).apply {
                setImageResource(R.drawable.ic_check)
                val p = ui.dp(4)
                setPadding(p, p, p, p)
            }
            row.addView(radio, LinearLayout.LayoutParams(ui.dp(26), ui.dp(26)))
            radios.add(radio)
            val card = MaterialCardView(ctx).apply {
                radius = ui.dpf(20f)
                cardElevation = 0f
                setCardBackgroundColor(ui.surface)
                addView(row)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    prefs.currentLanguage = lang.id
                    refreshLanguages()
                }
            }
            languageCards.add(lang to card)
            ui.add(col, card, 10)
        }
        col.addView(ui.text("Tip: hold the space bar or tap the globe key to switch while typing.", Type.BODY_SMALL, ui.onSurfaceVariant).apply {
            setPadding(ui.dp(8), ui.dp(2), ui.dp(8), 0)
        })

        col.addView(ui.sectionLabel("Dictionary"))
        val (dictCard, dictCol) = ui.card()
        learnedRow = ui.row(R.drawable.ic_book, "Learned words", "Counting…", ui.chevron()) { showLearnedWords() }
        dictCol.addView(learnedRow.root)
        dictCol.addView(ui.divider())
        blockedRow = ui.row(R.drawable.ic_delete, "Removed suggestions", "Words you long-pressed to hide")
        dictCol.addView(blockedRow.root)
        ui.add(col, dictCard, 4)

        col.addView(ui.sectionLabel("Predictions"))
        val (predCard, predCol) = ui.card()
        predCol.addView(ui.row(R.drawable.ic_spellcheck, "Auto-correction & suggestions", "Adjust how boldly words are corrected", ui.chevron()) {
            host.navigateTo(SettingsTab.KEYBOARD)
        }.root)
        predCol.addView(ui.divider())
        predCol.addView(ui.row(R.drawable.ic_shield, "Learning stays on this device",
            "Words you type are learned locally to improve suggestions and are never uploaded.").root)
        ui.add(col, predCard, 0)

        refreshLanguages()
        return scroll
    }

    private fun refreshLanguages() {
        val active = prefs.currentLanguage
        languageCards.forEachIndexed { i, (lang, card) ->
            val selected = lang.id == active
            card.strokeWidth = ui.dp(if (selected) 2 else 1)
            card.setStrokeColor(if (selected) ui.primary else ui.outline)
            card.contentDescription = lang.displayName + if (selected) ", active" else ""
            radios[i].background = if (selected) ui.rounded(ui.primary, 13f) else ui.rounded(0, 13f, ui.outline, 2f)
            radios[i].setColorFilter(if (selected) ui.onPrimary else 0)
        }
    }

    private fun refreshDictionaryCounts() {
        io.execute {
            val db = UserDictionaryDb(ctx.applicationContext)
            val learned = db.getLearnedWords().size
            val blocked = db.getBlacklistedWords().size
            db.close()
            main.post {
                learnedRow.subtitle?.text = when (learned) {
                    0 -> "Nothing learned yet"
                    1 -> "1 word · tap to review"
                    else -> "${if (learned >= 200) "200+" else learned.toString()} words · tap to review"
                }
                blockedRow.subtitle?.text = if (blocked == 0) "None — hold a suggestion to hide it" else "$blocked hidden from suggestions"
            }
        }
    }

    private fun showLearnedWords() {
        io.execute {
            val db = UserDictionaryDb(ctx.applicationContext)
            val words = db.getLearnedWords().keys.toList()
            db.close()
            main.post {
                val activity = ctx as? android.app.Activity
                if (activity != null && (activity.isFinishing || activity.isDestroyed)) return@post
                if (words.isEmpty()) {
                    Toast.makeText(ctx, "No learned words yet", Toast.LENGTH_SHORT).show()
                    return@post
                }
                MaterialAlertDialogBuilder(ctx)
                    .setTitle("Learned words")
                    .setItems(words.map { it as CharSequence }.toTypedArray()) { _, which -> confirmDelete(words[which]) }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }
    }

    private fun confirmDelete(word: String) {
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Forget “$word”?")
            .setMessage("It will no longer be suggested from your typing history.")
            .setPositiveButton("Forget") { _, _ ->
                io.execute {
                    val db = UserDictionaryDb(ctx.applicationContext)
                    db.deleteWord(word)
                    db.close()
                    main.post {
                        Toast.makeText(ctx, "Forgot “$word”", Toast.LENGTH_SHORT).show()
                        refreshDictionaryCounts()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onShown() {
        refreshLanguages()
        refreshDictionaryCounts()
    }

    override fun onPreferenceChanged(key: String?) {
        if (key == null || key == KeyboardPreferences.KEY_LANGUAGE) refreshLanguages()
    }
}

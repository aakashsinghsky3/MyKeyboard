package com.example.mykeyboard.settings

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.mykeyboard.R
import com.example.mykeyboard.model.KeyboardTheme
import com.example.mykeyboard.theme.BuiltInThemes
import com.example.mykeyboard.theme.GalleryFilter
import com.example.mykeyboard.theme.KeyStyles
import com.example.mykeyboard.theme.ThemePreviewView
import com.example.mykeyboard.utils.KeyboardPreferences
import com.google.android.material.card.MaterialCardView

/**
 * Theme picker: category chips + a two-column grid of live miniature keyboards.
 * Tapping a card applies it immediately (the big preview above updates through the
 * preference listener); the active theme carries an amber outline and check badge.
 * Long-press toggles favourite.
 */
class ThemeGalleryController(
    private val ui: SettingsUi,
    private val prefs: KeyboardPreferences,
    private val onApplied: () -> Unit
) {
    private val context = ui.context
    private var filter = GalleryFilter.FEATURED
    private val grid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val cards = ArrayList<Pair<KeyboardTheme, CardViews>>()

    private class CardViews(val card: MaterialCardView, val badge: ImageView, val name: TextView, val heart: TextView)

    private val filters = listOf(
        GalleryFilter.FEATURED, GalleryFilter.SIMPLE, GalleryFilter.PROFESSIONAL, GalleryFilter.DARK,
        GalleryFilter.COLORFUL, GalleryFilter.FUN, GalleryFilter.THREE_D, GalleryFilter.GAMING, GalleryFilter.FAVORITES
    )

    fun build(): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(ui.chips(filters.map { it.displayName }, filters.indexOf(filter)) { i ->
            filter = filters[i]
            buildGrid(animate = true)
        }, ui.lp(bottom = ui.dp(14)))
        addView(grid)
        buildGrid(animate = false)
    }

    private fun themesFor(f: GalleryFilter): List<KeyboardTheme> = when (f) {
        GalleryFilter.FAVORITES -> BuiltInThemes.ALL.filter { it.id in prefs.favoriteThemeIds }
        else -> BuiltInThemes.ALL.filter { BuiltInThemes.matches(it, f) }
    }

    private fun buildGrid(animate: Boolean) {
        grid.removeAllViews()
        cards.clear()
        val list = themesFor(filter)
        if (list.isEmpty()) {
            grid.addView(ui.text(
                if (filter == GalleryFilter.FAVORITES) "No favourites yet. Long-press any theme to add it here." else "No themes in this category yet.",
                SettingsUi.Type.BODY_SMALL, ui.onSurfaceVariant
            ).apply { gravity = Gravity.CENTER; setPadding(ui.dp(16), ui.dp(28), ui.dp(16), ui.dp(28)) })
            return
        }
        list.chunked(2).forEachIndexed { rowIndex, pair ->
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEachIndexed { i, theme ->
                val v = card(theme)
                row.addView(v.card, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (i == 0) marginEnd = ui.dp(6) else marginStart = ui.dp(6)
                })
                cards.add(theme to v)
                if (animate) ui.enter(v.card, (rowIndex * 40L).coerceAtMost(240L))
            }
            if (pair.size == 1) row.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f).apply { marginStart = ui.dp(6) })
            grid.addView(row, ui.lp(bottom = ui.dp(12)))
        }
        refreshStates()
    }

    private fun card(theme: KeyboardTheme): CardViews {
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(12))
        }
        val previewFrame = FrameLayout(context)
        previewFrame.addView(ThemePreviewView(context).apply {
            setTheme(theme)
            clipToOutline = true
            background = ui.rounded(0, 14f)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, ui.dp(92)))
        val badge = ImageView(context).apply {
            setImageResource(R.drawable.ic_check)
            setColorFilter(ui.onPrimary)
            val p = ui.dp(4)
            setPadding(p, p, p, p)
            background = ui.rounded(ui.primary, 12f, ui.surface, 2f)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        previewFrame.addView(badge, FrameLayout.LayoutParams(ui.dp(24), ui.dp(24), Gravity.TOP or Gravity.END).apply {
            topMargin = ui.dp(6); marginEnd = ui.dp(6)
        })
        column.addView(previewFrame)

        val nameRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val name = ui.text(theme.displayName, SettingsUi.Type.LABEL).apply { maxLines = 1 }
        nameRow.addView(name, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val heart = ui.text("♥", SettingsUi.Type.LABEL, ui.accentInk)
        nameRow.addView(heart)
        column.addView(nameRow, ui.lp(top = ui.dp(10)).apply { marginStart = ui.dp(4); marginEnd = ui.dp(4) })
        column.addView(ui.text(theme.keyStyle.displayName, SettingsUi.Type.BODY_SMALL, ui.onSurfaceVariant).apply {
            textSize = 12f
            maxLines = 1
        }, ui.lp(top = ui.dp(2)).apply { marginStart = ui.dp(4) })

        val card = MaterialCardView(context).apply {
            radius = ui.dpf(20f)
            cardElevation = 0f
            setCardBackgroundColor(ui.surface)
            addView(column)
            isClickable = true
            isFocusable = true
            contentDescription = "${theme.displayName} theme"
            setOnClickListener { apply(theme, this) }
            setOnLongClickListener {
                val fav = prefs.toggleFavoriteTheme(theme.id)
                Toast.makeText(context, if (fav) "Added to favourites" else "Removed from favourites", Toast.LENGTH_SHORT).show()
                if (filter == GalleryFilter.FAVORITES) buildGrid(animate = true) else refreshStates()
                true
            }
        }
        return CardViews(card, badge, name, heart)
    }

    private fun apply(theme: KeyboardTheme, view: View) {
        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(ui.emphasized).start()
        }.start()
        if (theme.id == prefs.baseTheme.id) return
        prefs.baseTheme = theme
        KeyStyles.byId(prefs.keyStyleOverrideId)?.let { if (!theme.isStyleCompatible(it)) prefs.keyStyleOverrideId = null }
        refreshStates()
        onApplied()
    }

    /** Updates selection visuals in place (no rebuild). */
    fun refreshStates() {
        val active = prefs.baseTheme.id
        val favs = prefs.favoriteThemeIds
        cards.forEach { (theme, v) ->
            val selected = theme.id == active
            v.card.strokeWidth = ui.dp(if (selected) 2 else 1)
            v.card.setStrokeColor(if (selected) ui.primary else ui.outline)
            v.badge.visibility = if (selected) View.VISIBLE else View.GONE
            v.heart.visibility = if (theme.id in favs) View.VISIBLE else View.GONE
            v.card.isSelected = selected
            v.card.contentDescription = "${theme.displayName} theme" + if (selected) ", active" else ""
        }
    }
}

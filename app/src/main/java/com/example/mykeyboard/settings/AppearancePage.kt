package com.example.mykeyboard.settings

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.mykeyboard.R
import com.example.mykeyboard.settings.SettingsUi.ButtonKind
import com.example.mykeyboard.settings.SettingsUi.Type
import com.example.mykeyboard.theme.KeyPressEffect
import com.example.mykeyboard.theme.KeyStyles
import com.example.mykeyboard.theme.ThemePreviewView
import com.example.mykeyboard.utils.KeyboardPreferences
import java.io.File
import kotlin.math.roundToInt

/**
 * Appearance: live keyboard preview on top, then theme gallery, key style / text size /
 * accent / animation, photo background, and the settings app's own light–dark mode.
 * Every change writes a preference; the preview and the running keyboard both follow.
 */
class AppearancePage(private val host: SettingsHost) : SettingsPage {
    private val ui = host.ui
    private val prefs = host.prefs
    private val ctx = ui.context

    private lateinit var preview: ThemePreviewView
    private lateinit var previewTitle: TextView
    private lateinit var previewSubtitle: TextView
    private lateinit var gallery: ThemeGalleryController
    private lateinit var styleHolder: FrameLayout
    private lateinit var accentRow: LinearLayout
    private lateinit var photoRow: SettingsUi.Row
    private lateinit var photoRemove: TextView
    private lateinit var photoVisibility: View

    private val accentChoices = listOf(
        0 to "Theme",
        0xFFFFB020.toInt() to "Amber",
        0xFFFF7A59.toInt() to "Coral",
        0xFFEF5C8A.toInt() to "Rose",
        0xFFA682FF.toInt() to "Violet",
        0xFF34C6B8.toInt() to "Teal",
        0xFF5DBB63.toInt() to "Green",
        0xFFD9D4CE.toInt() to "Silver",
        0xFF2B2B2B.toInt() to "Graphite"
    )

    override val view: View = build()

    private fun build(): View {
        val (scroll, col) = ui.page()
        col.addView(ui.pageHeader("Appearance", "Pick a theme and fine-tune how your keys look. Changes apply instantly."))

        // ---- Live preview ----
        val (previewCard, previewCol) = ui.card(ui.dp(10))
        preview = ThemePreviewView(ctx).apply {
            interactive = true
            detailed = true
            clipToOutline = true
            background = ui.rounded(0, 16f)
            contentDescription = "Live keyboard preview. Tap keys to feel the press effect."
        }
        previewCol.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(212)))
        val info = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(ui.dp(6), ui.dp(12), ui.dp(6), ui.dp(4)) }
        val infoTexts = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        previewTitle = ui.text("", Type.TITLE_SMALL)
        previewSubtitle = ui.text("", Type.BODY_SMALL, ui.onSurfaceVariant)
        infoTexts.addView(previewTitle)
        infoTexts.addView(previewSubtitle, ui.lp(top = ui.dp(2)))
        info.addView(infoTexts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        info.addView(ui.pill("Tap keys to try", ui.surfaceVariant, ui.onSurfaceVariant))
        previewCol.addView(info)
        ui.add(col, previewCard, 4)

        // ---- Themes ----
        col.addView(ui.sectionLabel("Theme"))
        gallery = ThemeGalleryController(ui, prefs) { refreshDynamic() }
        col.addView(gallery.build())

        // ---- Keys ----
        col.addView(ui.sectionLabel("Keys"))
        val (keysCard, keysCol) = ui.card()
        keysCol.addView(ui.row(R.drawable.ic_brush, "Key style", "Shape, depth and surface of every key").root)
        styleHolder = FrameLayout(ctx).apply { setPadding(ui.dp(12), 0, ui.dp(12), ui.dp(14)) }
        keysCol.addView(styleHolder)
        keysCol.addView(ui.divider(16))

        val (textBlock, _) = ui.sliderBlock(R.drawable.ic_text_size, "Text size", 85f, 130f, 5f, prefs.labelScale * 100f,
            format = { "${it.roundToInt()}%" }) { prefs.labelScale = it / 100f }
        keysCol.addView(textBlock)
        keysCol.addView(ui.divider(16))

        keysCol.addView(ui.row(R.drawable.ic_color_fill, "Accent color", "Enter key, active Shift and highlights").root)
        accentRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
            setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(16))
        }
        keysCol.addView(HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
            addView(accentRow)
        })
        keysCol.addView(ui.divider(16))

        keysCol.addView(ui.row(R.drawable.ic_motion, "Key animation", "Visual feedback only — typing is never delayed").root)
        val effects = listOf<KeyPressEffect?>(null, KeyPressEffect.NONE, KeyPressEffect.SUBTLE, KeyPressEffect.POP, KeyPressEffect.GLOW)
        val (effectSeg, _) = ui.segmented(listOf("Theme", "None", "Subtle", "Pop", "Glow"), effects.indexOf(prefs.keyPressEffectOverride).coerceAtLeast(0)) { i ->
            prefs.keyPressEffectOverride = effects[i]
        }
        keysCol.addView(effectSeg, ui.lp(bottom = ui.dp(16)).apply { marginStart = ui.dp(16); marginEnd = ui.dp(16) })
        ui.add(col, keysCard, 4)

        // ---- Background ----
        col.addView(ui.sectionLabel("Background"))
        val (bgCard, bgCol) = ui.card()
        val actions = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        photoRemove = ui.button("Remove", ButtonKind.TEXT) {
            prefs.customBgPath = null
            Toast.makeText(ctx, "Photo background removed", Toast.LENGTH_SHORT).show()
            refreshDynamic()
        }
        actions.addView(photoRemove)
        actions.addView(ui.button("Choose", ButtonKind.TONAL, R.drawable.ic_image) { host.pickBackgroundImage() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = ui.dp(4) })
        photoRow = ui.row(R.drawable.ic_image, "Photo background", "", null)
        bgCol.addView(photoRow.root)
        bgCol.addView(actions, ui.lp(bottom = ui.dp(8)).apply { marginStart = ui.dp(72) })
        val (visBlock, _) = ui.sliderBlock(null, "Photo visibility", 20f, 100f, 5f, prefs.customBgOpacity * 100f,
            format = { "${it.roundToInt()}%" }) { prefs.customBgOpacity = it / 100f }
        photoVisibility = visBlock
        bgCol.addView(ui.divider(16))
        bgCol.addView(visBlock.apply { setPadding(ui.dp(56), 0, 0, 0) })
        ui.add(col, bgCard, 4)

        // ---- App appearance ----
        col.addView(ui.sectionLabel("App appearance"))
        val (modeCard, modeCol) = ui.card()
        modeCol.addView(ui.row(R.drawable.ic_contrast, "Light / dark mode", "For this settings app. Your keyboard keeps its theme.").root)
        val modes = listOf("system", "light", "dark")
        val (modeSeg, _) = ui.segmented(listOf("System", "Light", "Dark"), modes.indexOf(prefs.appThemeMode).coerceAtLeast(0)) { i ->
            prefs.appThemeMode = modes[i]
            // Let the indicator finish sliding before the activity recreates.
            view.postDelayed({ host.applyAppThemeMode(modes[i]) }, 240)
        }
        modeCol.addView(modeSeg, ui.lp(bottom = ui.dp(16)).apply { marginStart = ui.dp(16); marginEnd = ui.dp(16) })
        ui.add(col, modeCard, 4)

        col.addView(ui.button("Reset appearance", ButtonKind.TEXT, R.drawable.ic_restore) {
            prefs.resetAppearance()
            Toast.makeText(ctx, "Appearance reset", Toast.LENGTH_SHORT).show()
            host.rebuildPages()
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL; topMargin = ui.dp(12)
        })

        refreshDynamic()
        return scroll
    }


    private fun buildAccentSwatches() {
        accentRow.removeAllViews()
        val current = prefs.accentColorOverride
        val themeAccent = prefs.baseTheme.palette.keyAction
        val ringSize = ui.dp(44)
        val dotSize = ui.dp(34)
        accentChoices.forEach { (color, name) ->
            val selected = color == current
            val swatchColor = if (color == 0) themeAccent else color
            val item = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                clipChildren = false
                clipToPadding = false
                isClickable = true
                contentDescription = "$name accent" + if (selected) ", selected" else ""
                setOnClickListener {
                    prefs.accentColorOverride = color
                    buildAccentSwatches()
                }
            }
            val ring = FrameLayout(ctx).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.TRANSPARENT)
                    if (selected) {
                        setStroke(ui.dp(2), ui.onSurface)
                    }
                }
            }
            val dot = View(ctx).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(swatchColor)
                    setStroke(ui.dp(1), ui.withAlpha(ui.onSurface, 0.12f))
                }
            }
            ring.addView(dot, FrameLayout.LayoutParams(dotSize, dotSize, Gravity.CENTER))
            if (color == 0) {
                ring.addView(
                    ui.text("A", Type.LABEL, if (com.example.mykeyboard.theme.ThemeColor.isLight(swatchColor)) ui.charcoal else Color.WHITE).apply { gravity = Gravity.CENTER },
                    FrameLayout.LayoutParams(dotSize, dotSize, Gravity.CENTER)
                )
            }
            item.addView(ring, LinearLayout.LayoutParams(ringSize, ringSize).apply { gravity = Gravity.CENTER_HORIZONTAL })
            item.addView(
                ui.text(name, Type.BODY_SMALL, if (selected) ui.onSurface else ui.onSurfaceVariant).apply { textSize = 11.5f },
                ui.lp(w = ViewGroup.LayoutParams.WRAP_CONTENT, top = ui.dp(4))
            )
            accentRow.addView(item, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = ui.dp(10) })
        }
    }

    private fun buildStyleChips() {
        styleHolder.removeAllViews()
        val base = prefs.baseTheme
        val options = listOf<com.example.mykeyboard.theme.KeyStyle?>(null) + KeyStyles.ALL.filter { it.id != base.keyStyle.id && base.isStyleCompatible(it) }
        val currentId = prefs.keyStyleOverrideId
        val selected = options.indexOfFirst { it?.id == currentId }.coerceAtLeast(0)
        styleHolder.addView(ui.chips(options.map { it?.displayName ?: "Theme default · ${base.keyStyle.displayName}" }, selected) { i ->
            prefs.keyStyleOverrideId = options[i]?.id
        })
    }

    private fun refreshDynamic() {
        buildStyleChips()
        buildAccentSwatches()
        gallery.refreshStates()
        refreshPreview()
        val hasPhoto = !prefs.customBgPath.isNullOrEmpty() && File(prefs.customBgPath!!).exists()
        photoRow.subtitle?.text = if (hasPhoto) "Using your photo · readability is adjusted automatically" else "Use one of your photos behind the keys"
        photoRemove.visibility = if (hasPhoto) View.VISIBLE else View.GONE
        photoVisibility.alpha = if (hasPhoto) 1f else 0.4f
        photoVisibility.isEnabled = hasPhoto
        setEnabledDeep(photoVisibility, hasPhoto)
    }

    private fun setEnabledDeep(v: View, enabled: Boolean) {
        v.isEnabled = enabled
        if (v is ViewGroup) for (i in 0 until v.childCount) setEnabledDeep(v.getChildAt(i), enabled)
    }

    private fun refreshPreview() {
        val theme = prefs.theme
        preview.setTheme(theme, prefs.keyPressEffectOverride, prefs.isHighContrastEnabled)
        previewTitle.text = theme.displayName
        previewSubtitle.text = "${theme.category.displayName} · ${theme.keyStyle.displayName}"
    }

    override fun onShown() = refreshDynamic()

    override fun onPreferenceChanged(key: String?) {
        when {
            key == null -> refreshDynamic()
            key == KeyboardPreferences.KEY_CUSTOM_BG_PATH || key == KeyboardPreferences.KEY_THEME -> refreshDynamic()
            key in KeyboardPreferences.APPEARANCE_KEYS -> refreshPreview()
        }
    }
}

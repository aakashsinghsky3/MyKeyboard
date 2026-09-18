package com.example.mykeyboard.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.mykeyboard.engine.AutoCorrectMode
import com.example.mykeyboard.model.KeyboardTheme
import com.example.mykeyboard.theme.BuiltInThemes
import com.example.mykeyboard.theme.KeyPressEffect
import com.example.mykeyboard.theme.KeyStyles

class KeyboardPreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val defaultTheme: KeyboardTheme
        get() {
            val isDark = when (appThemeMode) {
                "dark" -> true
                "light" -> false
                else -> {
                    val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            }
            return if (isDark) BuiltInThemes.DEFAULT_DARK else BuiltInThemes.DEFAULT_LIGHT
        }

    /** Base theme selected in the gallery (without the user's key-style override). */
    var baseTheme: KeyboardTheme
        get() {
            val stored = prefs.getString(KEY_THEME, null)
            return if (stored != null) KeyboardTheme.fromId(stored) else defaultTheme
        }
        set(value) {
            val recent = (listOf(value.id) + recentThemeIds.filter { it != value.id }).take(MAX_RECENT)
            prefs.edit()
                .putString(KEY_THEME, value.id)
                .putString(KEY_RECENT_THEMES, recent.joinToString(","))
                .apply()
        }

    /** Effective theme: base theme + key-style, accent colour and key-spacing overrides. */
    var theme: KeyboardTheme
        get() = baseTheme
            .withKeyStyle(KeyStyles.byId(keyStyleOverrideId))
            .withAccent(accentColorOverride)
            .withKeySpacing(keySpacingScale)
        set(value) { baseTheme = value }

    /** 0 = use the theme's own accent (Enter / active Shift / highlights). */
    var accentColorOverride: Int
        get() = prefs.getInt(KEY_ACCENT_COLOR, 0)
        set(value) = prefs.edit().putInt(KEY_ACCENT_COLOR, value).apply()

    /** Gap between keys relative to the theme (0.6 compact … 1.4 wide). Hitboxes are unchanged. */
    var keySpacingScale: Float
        get() = prefs.getFloat(KEY_KEY_SPACING, 1.0f).coerceIn(0.5f, 1.5f)
        set(value) = prefs.edit().putFloat(KEY_KEY_SPACING, value.coerceIn(0.5f, 1.5f)).apply()

    /** Settings-app appearance: "system", "light" or "dark". Does not affect the keyboard theme. */
    var appThemeMode: String
        get() = prefs.getString(KEY_APP_THEME_MODE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_APP_THEME_MODE, value).apply()

    /** Restores every setting to its default (learned words and clipboard are kept). */
    fun resetAll() {
        // Emoji recents/favourites are personal content, not settings – keep them.
        val recent = prefs.getString(KEY_RECENT_EMOJIS, null)
        val favorites = prefs.getString(KEY_FAVORITE_EMOJIS, null)
        val clipboardHistory = prefs.getBoolean(KEY_CLIPBOARD_HISTORY, true)
        prefs.edit().clear().apply {
            recent?.let { putString(KEY_RECENT_EMOJIS, it) }
            favorites?.let { putString(KEY_FAVORITE_EMOJIS, it) }
            // Privacy choice, not an appearance setting: never silently re-enable history.
            putBoolean(KEY_CLIPBOARD_HISTORY, clipboardHistory)
            remove(KEY_THEME)
        }.apply()
    }

    /** Null = use the theme's own button style. */
    var keyStyleOverrideId: String?
        get() = prefs.getString(KEY_KEY_STYLE, null)
        set(value) = prefs.edit().putString(KEY_KEY_STYLE, value).apply()

    /** Null = theme default press effect. */
    var keyPressEffectOverride: KeyPressEffect?
        get() = KeyPressEffect.fromId(prefs.getString(KEY_PRESS_EFFECT, null))
        set(value) = prefs.edit().putString(KEY_PRESS_EFFECT, value?.id).apply()

    val effectivePressEffect: KeyPressEffect
        get() = keyPressEffectOverride ?: theme.pressEffect

    var isReduceMotionEnabled: Boolean
        get() = prefs.getBoolean(KEY_REDUCE_MOTION, false)
        set(value) = prefs.edit().putBoolean(KEY_REDUCE_MOTION, value).apply()

    var isHighContrastEnabled: Boolean
        get() = prefs.getBoolean(KEY_HIGH_CONTRAST, false)
        set(value) = prefs.edit().putBoolean(KEY_HIGH_CONTRAST, value).apply()

    /** Key label scale, 0.85..1.3. */
    var labelScale: Float
        get() = prefs.getFloat(KEY_LABEL_SCALE, 1.15f).coerceIn(0.85f, 1.3f)
        set(value) = prefs.edit().putFloat(KEY_LABEL_SCALE, value.coerceIn(0.85f, 1.3f)).apply()

    var favoriteThemeIds: Set<String>
        get() = prefs.getStringSet(KEY_FAVORITE_THEMES, emptySet())?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_FAVORITE_THEMES, HashSet(value)).apply()

    val recentThemeIds: List<String>
        get() = prefs.getString(KEY_RECENT_THEMES, "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    fun toggleFavoriteTheme(id: String): Boolean {
        val set = favoriteThemeIds.toMutableSet()
        val nowFavorite = if (id in set) { set.remove(id); false } else { set.add(id); true }
        favoriteThemeIds = set
        return nowFavorite
    }

    // ---- Emoji panel (stored locally only) ----------------------------------------------------

    /** Most-recent-first. */
    var recentEmojis: List<String>
        get() = prefs.getString(KEY_RECENT_EMOJIS, "")?.split(EMOJI_SEP)?.filter { it.isNotEmpty() } ?: emptyList()
        private set(value) = prefs.edit().putString(KEY_RECENT_EMOJIS, value.joinToString(EMOJI_SEP)).apply()

    fun recordRecentEmoji(emoji: String): List<String> {
        val updated = (listOf(emoji) + recentEmojis.filter { it != emoji }).take(MAX_RECENT_EMOJIS)
        recentEmojis = updated
        return updated
    }

    var favoriteEmojis: List<String>
        get() {
            val raw = prefs.getString(KEY_FAVORITE_EMOJIS, null)
                ?: return com.example.mykeyboard.model.EmojiData.DEFAULT_FAVORITES
            return raw.split(EMOJI_SEP).filter { it.isNotEmpty() }
        }
        set(value) = prefs.edit().putString(KEY_FAVORITE_EMOJIS, value.joinToString(EMOJI_SEP)).apply()

    /** Returns true if the emoji is now a favourite. */
    fun toggleFavoriteEmoji(emoji: String): Boolean {
        val list = favoriteEmojis.toMutableList()
        return if (list.remove(emoji)) { favoriteEmojis = list; false } else { list.add(0, emoji); favoriteEmojis = list; true }
    }

    fun resetAppearance() {
        prefs.edit()
            .remove(KEY_THEME)
            .remove(KEY_KEY_STYLE)
            .remove(KEY_PRESS_EFFECT)
            .remove(KEY_ACCENT_COLOR)
            .remove(KEY_KEY_SPACING)
            .remove(KEY_LABEL_SCALE)
            .apply()
    }

    var isHapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC, false)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC, value).apply()

    var hapticDuration: Long
        get() = prefs.getLong(KEY_HAPTIC_DURATION, 20L)
        set(value) = prefs.edit().putLong(KEY_HAPTIC_DURATION, value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, false)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var isPopupEnabled: Boolean
        get() = prefs.getBoolean(KEY_POPUP, false)
        set(value) = prefs.edit().putBoolean(KEY_POPUP, value).apply()

    var isNumberRowEnabled: Boolean
        get() = prefs.getBoolean(KEY_NUMBER_ROW, true)
        set(value) = prefs.edit().putBoolean(KEY_NUMBER_ROW, value).apply()

    var isAutoCapsEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CAPS, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CAPS, value).apply()

    var heightScale: Float
        get() = prefs.getFloat(KEY_HEIGHT_SCALE, 1.10f)
        set(value) = prefs.edit().putFloat(KEY_HEIGHT_SCALE, value).apply()

    var currentLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var autoCorrectMode: AutoCorrectMode
        get() {
            val modeId = prefs.getString(KEY_AUTO_CORRECT_MODE, AutoCorrectMode.OFF.id)
            return AutoCorrectMode.fromId(modeId)
        }
        set(value) {
            prefs.edit().putString(KEY_AUTO_CORRECT_MODE, value.id).apply()
        }

    var customBgPath: String?
        get() = prefs.getString(KEY_CUSTOM_BG_PATH, null)
        set(value) = prefs.edit().putString(KEY_CUSTOM_BG_PATH, value).apply()

    var customBgOpacity: Float
        get() = prefs.getFloat(KEY_CUSTOM_BG_OPACITY, 0.70f)
        set(value) = prefs.edit().putFloat(KEY_CUSTOM_BG_OPACITY, value).apply()

    var isClipboardHistoryEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLIPBOARD_HISTORY, true)
        set(value) = prefs.edit().putBoolean(KEY_CLIPBOARD_HISTORY, value).apply()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREFS_NAME = "my_keyboard_prefs"
        const val KEY_LANGUAGE = "key_language"
        const val KEY_THEME = "pref_theme"
        const val KEY_HAPTIC = "pref_haptic"
        const val KEY_HAPTIC_DURATION = "pref_haptic_duration"
        const val KEY_SOUND = "pref_sound"
        const val KEY_POPUP = "pref_popup"
        const val KEY_NUMBER_ROW = "pref_number_row"
        const val KEY_AUTO_CAPS = "pref_auto_caps"
        const val KEY_HEIGHT_SCALE = "pref_height_scale"
        const val KEY_AUTO_CORRECT_MODE = "pref_auto_correct_mode"
        const val KEY_CUSTOM_BG_PATH = "pref_custom_bg_path"
        const val KEY_CUSTOM_BG_OPACITY = "pref_custom_bg_opacity"
        const val KEY_CLIPBOARD_HISTORY = "pref_clipboard_history"
        const val KEY_KEY_STYLE = "pref_key_style"
        const val KEY_ACCENT_COLOR = "pref_accent_color"
        const val KEY_KEY_SPACING = "pref_key_spacing"
        const val KEY_APP_THEME_MODE = "pref_app_theme_mode"
        const val KEY_PRESS_EFFECT = "pref_key_press_effect"
        const val KEY_REDUCE_MOTION = "pref_reduce_motion"
        const val KEY_HIGH_CONTRAST = "pref_high_contrast"
        const val KEY_LABEL_SCALE = "pref_label_scale"
        const val KEY_FAVORITE_THEMES = "pref_favorite_themes"
        const val KEY_RECENT_THEMES = "pref_recent_themes"
        private const val MAX_RECENT = 6
        const val KEY_RECENT_EMOJIS = "pref_recent_emojis"
        const val KEY_FAVORITE_EMOJIS = "pref_favorite_emojis"
        private const val MAX_RECENT_EMOJIS = 48
        private const val EMOJI_SEP = "\u001F"

        /** Preference keys that change how the keyboard looks (trigger a theme re-apply). */
        val APPEARANCE_KEYS = setOf(
            KEY_THEME, KEY_NUMBER_ROW, KEY_HEIGHT_SCALE, KEY_CUSTOM_BG_PATH, KEY_CUSTOM_BG_OPACITY,
            KEY_KEY_STYLE, KEY_PRESS_EFFECT, KEY_REDUCE_MOTION, KEY_HIGH_CONTRAST, KEY_LABEL_SCALE,
            KEY_ACCENT_COLOR, KEY_KEY_SPACING
        )
    }
}

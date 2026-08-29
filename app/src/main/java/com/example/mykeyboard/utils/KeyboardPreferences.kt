package com.example.mykeyboard.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.mykeyboard.model.KeyboardTheme

class KeyboardPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var theme: KeyboardTheme
        get() {
            val themeId = prefs.getString(KEY_THEME, KeyboardTheme.MATERIAL_DARK.id)
            return KeyboardTheme.fromId(themeId)
        }
        set(value) {
            prefs.edit().putString(KEY_THEME, value.id).apply()
        }

    var isHapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC, value).apply()

    var hapticDuration: Long
        get() = prefs.getLong(KEY_HAPTIC_DURATION, 20L)
        set(value) = prefs.edit().putLong(KEY_HAPTIC_DURATION, value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, false)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var isPopupEnabled: Boolean
        get() = prefs.getBoolean(KEY_POPUP, true)
        set(value) = prefs.edit().putBoolean(KEY_POPUP, value).apply()

    var isNumberRowEnabled: Boolean
        get() = prefs.getBoolean(KEY_NUMBER_ROW, false)
        set(value) = prefs.edit().putBoolean(KEY_NUMBER_ROW, value).apply()

    var isAutoCapsEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CAPS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CAPS, value).apply()

    var heightScale: Float
        get() = prefs.getFloat(KEY_HEIGHT_SCALE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_HEIGHT_SCALE, value).apply()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREFS_NAME = "my_keyboard_prefs"
        const val KEY_THEME = "pref_theme"
        const val KEY_HAPTIC = "pref_haptic"
        const val KEY_HAPTIC_DURATION = "pref_haptic_duration"
        const val KEY_SOUND = "pref_sound"
        const val KEY_POPUP = "pref_popup"
        const val KEY_NUMBER_ROW = "pref_number_row"
        const val KEY_AUTO_CAPS = "pref_auto_caps"
        const val KEY_HEIGHT_SCALE = "pref_height_scale"
    }
}

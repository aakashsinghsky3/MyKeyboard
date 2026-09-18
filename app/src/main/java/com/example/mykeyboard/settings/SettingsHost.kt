package com.example.mykeyboard.settings

import android.view.View
import com.example.mykeyboard.utils.KeyboardPreferences

/** What a settings page needs from the activity. */
interface SettingsHost {
    val prefs: KeyboardPreferences
    val ui: SettingsUi
    fun navigateTo(tab: SettingsTab)
    fun pickBackgroundImage()
    fun isKeyboardEnabled(): Boolean
    fun isKeyboardSelected(): Boolean
    fun openInputMethodSettings()
    fun showInputMethodPicker()
    fun applyAppThemeMode(mode: String)
    fun appVersion(): String
    /** Rebuild all pages from current preferences (after a reset). */
    fun rebuildPages()
}

enum class SettingsTab(val title: String, val iconRes: Int) {
    HOME("Home", com.example.mykeyboard.R.drawable.ic_home),
    APPEARANCE("Appearance", com.example.mykeyboard.R.drawable.ic_palette),
    KEYBOARD("Keyboard", com.example.mykeyboard.R.drawable.ic_keyboard),
    LANGUAGE("Language", com.example.mykeyboard.R.drawable.ic_translate),
    ADVANCED("Advanced", com.example.mykeyboard.R.drawable.ic_tune)
}

/** A settings screen. Views are built once and refreshed in place. */
interface SettingsPage {
    val view: View
    /** Called every time the tab becomes visible (and on activity resume while visible). */
    fun onShown() {}
    /** A shared preference changed (from this app or the keyboard). */
    fun onPreferenceChanged(key: String?) {}
}

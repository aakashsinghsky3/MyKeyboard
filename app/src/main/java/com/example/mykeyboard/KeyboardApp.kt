package com.example.mykeyboard

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.mykeyboard.utils.KeyboardPreferences

/** Applies the settings app's light/dark choice before any activity starts (no extra recreate). */
class KeyboardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(
            when (KeyboardPreferences(this).appThemeMode) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}

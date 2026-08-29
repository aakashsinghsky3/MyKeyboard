package com.example.mykeyboard

import android.content.Intent
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import com.example.mykeyboard.model.ShiftState
import com.example.mykeyboard.utils.KeyboardPreferences
import com.example.mykeyboard.view.CustomKeyboardView

class MyKeyboardService : InputMethodService(),
    CustomKeyboardView.KeyboardActionListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var preferences: KeyboardPreferences
    private var keyboardView: CustomKeyboardView? = null
    private var lastSpaceTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        preferences = KeyboardPreferences(this)
        preferences.registerListener(this)
    }

    override fun onDestroy() {
        preferences.unregisterListener(this)
        super.onDestroy()
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }

    override fun onCreateInputView(): View {
        val view = CustomKeyboardView(this).apply {
            setActionListener(this@MyKeyboardService)
            applyTheme(preferences.theme)
        }
        keyboardView = view
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        keyboardView?.resetToAlpha()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (info != null) {
            keyboardView?.setImeOptions(info.imeOptions, info.actionLabel)
        }
        checkAutoCaps()
        updateCurrentWordSuggestions()
    }

    // ---------------------------------------------------------------------------------------------
    // Keyboard Action Listeners
    // ---------------------------------------------------------------------------------------------
    override fun onTextKey(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        checkAutoCaps()
        updateCurrentWordSuggestions()
    }

    override fun onBackspace() {
        val ic = currentInputConnection ?: return
        val selectedText = ic.getSelectedText(0)
        if (!TextUtils.isEmpty(selectedText)) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
        checkAutoCaps()
        updateCurrentWordSuggestions()
    }

    override fun onSpace() {
        val ic = currentInputConnection ?: return
        val now = System.currentTimeMillis()

        // Double space for ". " shortcut
        if (now - lastSpaceTime < 450) {
            val textBefore = ic.getTextBeforeCursor(2, 0)?.toString() ?: ""
            if (textBefore.endsWith(" ") && !textBefore.endsWith(". ")) {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(". ", 1)
                lastSpaceTime = 0L
                checkAutoCaps()
                updateCurrentWordSuggestions()
                return
            }
        }

        ic.commitText(" ", 1)
        lastSpaceTime = now
        checkAutoCaps()
        updateCurrentWordSuggestions()
    }

    override fun onEnter(actionId: Int) {
        val ic = currentInputConnection ?: return
        if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(actionId)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    override fun onOpenEmoji() {
        // Handled inside CustomKeyboardView
    }

    override fun onOpenSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        startActivity(intent)
    }

    override fun onMoveCursor(offset: Int) {
        val keyCode = if (offset < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        val count = kotlin.math.abs(offset)
        for (i in 0 until count) {
            sendDownUpKeyEvents(keyCode)
        }
    }

    override fun onPasteClipboard(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        checkAutoCaps()
        updateCurrentWordSuggestions()
    }

    // ---------------------------------------------------------------------------------------------
    // Auto-Capitalization & Suggestions
    // ---------------------------------------------------------------------------------------------
    private fun checkAutoCaps() {
        if (!preferences.isAutoCapsEnabled) return

        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(3, 0)?.toString() ?: ""

        val shouldCap = when {
            textBefore.isEmpty() -> true
            textBefore.endsWith("\n") -> true
            textBefore.endsWith(". ") -> true
            textBefore.endsWith("? ") -> true
            textBefore.endsWith("! ") -> true
            else -> false
        }

        if (shouldCap) {
            keyboardView?.setShiftState(ShiftState.SHIFTED_ONCE)
        }
    }

    private fun updateCurrentWordSuggestions() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(20, 0)?.toString() ?: ""
        val lastWord = textBefore.split(Regex("[^a-zA-Z0-9']")).lastOrNull() ?: ""
        keyboardView?.updateSuggestions(lastWord)
    }

    // ---------------------------------------------------------------------------------------------
    // Preference Changes
    // ---------------------------------------------------------------------------------------------
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            KeyboardPreferences.KEY_THEME -> {
                keyboardView?.applyTheme(preferences.theme)
            }
            KeyboardPreferences.KEY_NUMBER_ROW,
            KeyboardPreferences.KEY_HEIGHT_SCALE -> {
                keyboardView?.applyTheme(preferences.theme)
            }
        }
    }
}
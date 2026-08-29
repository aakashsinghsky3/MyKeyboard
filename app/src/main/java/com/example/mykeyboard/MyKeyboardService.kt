package com.example.mykeyboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import com.example.mykeyboard.engine.AutoCorrectEngine
import com.example.mykeyboard.engine.ClipboardHistoryManager
import com.example.mykeyboard.engine.PredictionEngine
import com.example.mykeyboard.engine.ProfessionalToneEngine
import com.example.mykeyboard.engine.UndoRedoManager
import android.widget.Toast
import com.example.mykeyboard.model.ShiftState
import com.example.mykeyboard.utils.KeyboardPreferences
import com.example.mykeyboard.view.CustomKeyboardView

class MyKeyboardService : InputMethodService(),
    CustomKeyboardView.KeyboardActionListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var preferences: KeyboardPreferences
    private lateinit var predictionEngine: PredictionEngine
    private lateinit var undoRedoManager: UndoRedoManager
    private lateinit var clipboardHistoryManager: ClipboardHistoryManager

    private var keyboardView: CustomKeyboardView? = null
    private var lastSpaceTime: Long = 0L
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override fun onCreate() {
        super.onCreate()
        preferences = KeyboardPreferences(this)
        preferences.registerListener(this)
        predictionEngine = PredictionEngine(this)
        undoRedoManager = UndoRedoManager()
        clipboardHistoryManager = ClipboardHistoryManager(this)

        setupClipboardListener()
    }

    override fun onDestroy() {
        preferences.unregisterListener(this)
        removeClipboardListener()
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
        undoRedoManager.clear()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (info != null) {
            keyboardView?.setImeOptions(info.imeOptions, info.actionLabel)
        }
        checkAutoCaps()
        updatePredictions()
        recordCurrentSnapshot()
    }

    // ---------------------------------------------------------------------------------------------
    // Keyboard Action Listeners
    // ---------------------------------------------------------------------------------------------
    override fun onTextKey(text: String) {
        val ic = currentInputConnection ?: return
        recordCurrentSnapshot()

        // If committing a suggestion with a trailing space, learn the word
        val trimmed = text.trim()
        if (trimmed.isNotEmpty() && !trimmed.contains(" ")) {
            predictionEngine.learnWord(trimmed)
        }

        // Replace partial prefix if committing a suggestion word
        val textBefore = ic.getTextBeforeCursor(20, 0)?.toString() ?: ""
        val lastWord = textBefore.split(Regex("[^a-zA-Z0-9']")).lastOrNull() ?: ""

        if (text.startsWith(lastWord, ignoreCase = true) && lastWord.isNotEmpty() && text.length > lastWord.length) {
            ic.deleteSurroundingText(lastWord.length, 0)
        }

        ic.commitText(text, 1)
        checkAutoCaps()
        updatePredictions()
        recordCurrentSnapshot()
    }

    override fun onBackspace() {
        val ic = currentInputConnection ?: return
        recordCurrentSnapshot()

        val selectedText = ic.getSelectedText(0)
        if (!TextUtils.isEmpty(selectedText)) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
        checkAutoCaps()
        updatePredictions()
        recordCurrentSnapshot()
    }

    override fun onSpace() {
        val ic = currentInputConnection ?: return
        recordCurrentSnapshot()

        val now = System.currentTimeMillis()
        val textBefore = ic.getTextBeforeCursor(30, 0)?.toString() ?: ""

        // 1. Double space for ". " shortcut
        if (now - lastSpaceTime < 450) {
            if (textBefore.endsWith(" ") && !textBefore.endsWith(". ")) {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(". ", 1)
                lastSpaceTime = 0L
                checkAutoCaps()
                updatePredictions()
                recordCurrentSnapshot()
                return
            }
        }

        // 2. Auto-Correction on Space
        val words = textBefore.trimEnd().split(Regex("[^a-zA-Z0-9']")).filter { it.isNotEmpty() }
        val lastWord = words.lastOrNull() ?: ""

        if (lastWord.isNotEmpty()) {
            predictionEngine.learnWord(lastWord)

            val correction = AutoCorrectEngine.getCorrection(lastWord, preferences.autoCorrectMode)
            if (correction != null && correction != lastWord) {
                ic.deleteSurroundingText(lastWord.length, 0)
                ic.commitText("$correction ", 1)
                lastSpaceTime = now
                checkAutoCaps()
                updatePredictions()
                recordCurrentSnapshot()
                return
            }
        }

        ic.commitText(" ", 1)
        lastSpaceTime = now
        checkAutoCaps()
        updatePredictions()
        recordCurrentSnapshot()
    }

    override fun onEnter(actionId: Int) {
        val ic = currentInputConnection ?: return
        recordCurrentSnapshot()

        if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(actionId)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    override fun onOpenEmoji() {}

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
        recordCurrentSnapshot()
        ic.commitText(text, 1)
        checkAutoCaps()
        updatePredictions()
        recordCurrentSnapshot()
    }

    override fun onUndo() {
        val ic = currentInputConnection ?: return
        undoRedoManager.undo(ic)
        checkAutoCaps()
        updatePredictions()
    }

    override fun onRedo() {
        val ic = currentInputConnection ?: return
        undoRedoManager.redo(ic)
        checkAutoCaps()
        updatePredictions()
    }

    override fun onAddWordToDictionary(word: String) {
        predictionEngine.addCustomWord(word)
        updatePredictions()
    }

    override fun onProfessionalRephrase() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(1000, 0)?.toString() ?: ""
        val cleanText = textBefore.trim()
        if (cleanText.isEmpty()) {
            Toast.makeText(this, "Type a sentence first, then tap 💼 Professional Tone!", Toast.LENGTH_SHORT).show()
            return
        }

        val options = ProfessionalToneEngine.getProfessionalRephrasings(cleanText)
        if (options.isNotEmpty()) {
            keyboardView?.showProfessionalSuggestions(cleanText, options)
        }
    }

    override fun onReplaceText(oldText: String, newText: String) {
        val ic = currentInputConnection ?: return
        recordCurrentSnapshot()
        ic.deleteSurroundingText(oldText.length, 0)
        ic.commitText(newText, 1)
        checkAutoCaps()
        updatePredictions()
        recordCurrentSnapshot()
    }

    // ---------------------------------------------------------------------------------------------
    // Undo / Redo & Prediction Helpers
    // ---------------------------------------------------------------------------------------------
    private fun recordCurrentSnapshot() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(1000, 0)?.toString() ?: ""
        val textAfter = ic.getTextAfterCursor(1000, 0)?.toString() ?: ""
        val fullText = textBefore + textAfter
        undoRedoManager.recordState(fullText, textBefore.length)
    }

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

    private fun updatePredictions() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""

        val isAfterSpace = textBefore.endsWith(" ")
        val allWords = textBefore.trim().split(Regex("[^a-zA-Z0-9']")).filter { it.isNotEmpty() }

        val prefix = if (isAfterSpace) "" else (allWords.lastOrNull() ?: "")
        val prevWords = if (isAfterSpace) allWords else allWords.dropLast(1)

        keyboardView?.updatePredictions(prefix, prevWords)
    }

    // ---------------------------------------------------------------------------------------------
    // Clipboard Listener
    // ---------------------------------------------------------------------------------------------
    private fun setupClipboardListener() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
                if (preferences.isClipboardHistoryEnabled) {
                    val clip = cm?.primaryClip?.getItemAt(0)?.text?.toString()
                    if (!clip.isNullOrEmpty()) {
                        clipboardHistoryManager.addClip(clip)
                        keyboardView?.refreshClipboard()
                    }
                }
            }
            cm?.addPrimaryClipChangedListener(clipboardListener)
        } catch (_: Exception) {}
    }

    private fun removeClipboardListener() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardListener?.let { cm?.removePrimaryClipChangedListener(it) }
        } catch (_: Exception) {}
    }

    // ---------------------------------------------------------------------------------------------
    // Preference Changes
    // ---------------------------------------------------------------------------------------------
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            KeyboardPreferences.KEY_THEME,
            KeyboardPreferences.KEY_NUMBER_ROW,
            KeyboardPreferences.KEY_HEIGHT_SCALE,
            KeyboardPreferences.KEY_CUSTOM_BG_PATH,
            KeyboardPreferences.KEY_CUSTOM_BG_OPACITY -> {
                keyboardView?.applyTheme(preferences.theme)
            }
        }
    }
}
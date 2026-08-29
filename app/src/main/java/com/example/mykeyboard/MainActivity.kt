package com.example.mykeyboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mykeyboard.engine.AutoCorrectMode
import com.example.mykeyboard.model.KeyboardTheme
import com.example.mykeyboard.utils.KeyboardPreferences
import com.google.android.material.materialswitch.MaterialSwitch
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var preferences: KeyboardPreferences

    private lateinit var tvStep1Status: TextView
    private lateinit var tvStep2Status: TextView
    private lateinit var etTestTyping: EditText
    private lateinit var btnClearTest: TextView
    private lateinit var layoutThemesContainer: LinearLayout

    // Custom Background Views
    private lateinit var btnChooseImage: TextView
    private lateinit var btnRemoveImage: TextView
    private lateinit var tvOpacityValue: TextView
    private lateinit var seekbarOpacity: SeekBar

    // Auto-Correction Views
    private lateinit var btnAutocorrectOff: TextView
    private lateinit var btnAutocorrectConservative: TextView
    private lateinit var btnAutocorrectAggressive: TextView

    // Preferences Switches
    private lateinit var switchVoice: MaterialSwitch
    private lateinit var switchClipboard: MaterialSwitch
    private lateinit var switchNumberRow: MaterialSwitch
    private lateinit var switchKeyPopup: MaterialSwitch
    private lateinit var switchHaptic: MaterialSwitch
    private lateinit var switchSound: MaterialSwitch
    private lateinit var switchAutoCaps: MaterialSwitch

    // Height Adjuster
    private lateinit var btnHeightCompact: TextView
    private lateinit var btnHeightNormal: TextView
    private lateinit var btnHeightTall: TextView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            saveCustomBackground(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        preferences = KeyboardPreferences(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        initViews()
        setupWizard()
        setupCustomBackground()
        setupAutoCorrectModes()
        setupThemePicker()
        setupPreferences()
        setupHeightAdjuster()
    }

    override fun onResume() {
        super.onResume()
        updateWizardStatus()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            updateWizardStatus()
        }
    }

    private fun initViews() {
        tvStep1Status = findViewById(R.id.tv_step1_status)
        tvStep2Status = findViewById(R.id.tv_step2_status)
        etTestTyping = findViewById(R.id.et_test_typing)
        btnClearTest = findViewById(R.id.btn_clear_test)
        layoutThemesContainer = findViewById(R.id.layout_themes_container)

        btnChooseImage = findViewById(R.id.btn_choose_image)
        btnRemoveImage = findViewById(R.id.btn_remove_image)
        tvOpacityValue = findViewById(R.id.tv_opacity_value)
        seekbarOpacity = findViewById(R.id.seekbar_opacity)

        btnAutocorrectOff = findViewById(R.id.btn_autocorrect_off)
        btnAutocorrectConservative = findViewById(R.id.btn_autocorrect_conservative)
        btnAutocorrectAggressive = findViewById(R.id.btn_autocorrect_aggressive)

        switchVoice = findViewById(R.id.switch_voice)
        switchClipboard = findViewById(R.id.switch_clipboard)
        switchNumberRow = findViewById(R.id.switch_number_row)
        switchKeyPopup = findViewById(R.id.switch_key_popup)
        switchHaptic = findViewById(R.id.switch_haptic)
        switchSound = findViewById(R.id.switch_sound)
        switchAutoCaps = findViewById(R.id.switch_autocaps)

        btnHeightCompact = findViewById(R.id.btn_height_compact)
        btnHeightNormal = findViewById(R.id.btn_height_normal)
        btnHeightTall = findViewById(R.id.btn_height_tall)

        btnClearTest.setOnClickListener {
            etTestTyping.setText("")
        }

        etTestTyping.setOnClickListener {
            etTestTyping.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(etTestTyping, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupWizard() {
        findViewById<View>(R.id.btn_step1_enable).setOnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            startActivity(intent)
        }

        findViewById<View>(R.id.btn_step2_switch).setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        }
    }

    private fun updateWizardStatus() {
        val isEnabled = isKeyboardEnabled()
        val isSelected = isKeyboardSelected()

        if (isEnabled) {
            tvStep1Status.text = "Enabled ✓"
            tvStep1Status.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
        } else {
            tvStep1Status.text = "Enable"
            tvStep1Status.setTextColor(ContextCompat.getColor(this, R.color.primary))
        }

        if (isSelected) {
            tvStep2Status.text = "Active ✓"
            tvStep2Status.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
        } else {
            tvStep2Status.text = "Select"
            tvStep2Status.setTextColor(ContextCompat.getColor(this, R.color.primary))
        }
    }

    private fun isKeyboardEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
        val list = imm.enabledInputMethodList
        val myPackage = packageName
        return list.any { it.packageName == myPackage }
    }

    private fun isKeyboardSelected(): Boolean {
        val currentIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        return currentIme.contains(packageName)
    }

    private fun setupCustomBackground() {
        btnChooseImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnRemoveImage.setOnClickListener {
            preferences.customBgPath = null
            Toast.makeText(this, "Custom background removed", Toast.LENGTH_SHORT).show()
        }

        val opacityPct = (preferences.customBgOpacity * 100).toInt()
        seekbarOpacity.progress = opacityPct
        tvOpacityValue.text = "$opacityPct%"

        seekbarOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val clamped = progress.coerceIn(20, 100)
                tvOpacityValue.text = "$clamped%"
                preferences.customBgOpacity = clamped / 100f
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun saveCustomBackground(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val file = File(filesDir, "custom_keyboard_bg.png")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            preferences.customBgPath = file.absolutePath
            Toast.makeText(this, "Custom photo background applied! ✓", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load photo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAutoCorrectModes() {
        updateAutoCorrectButtons(preferences.autoCorrectMode)

        btnAutocorrectOff.setOnClickListener {
            preferences.autoCorrectMode = AutoCorrectMode.OFF
            updateAutoCorrectButtons(AutoCorrectMode.OFF)
        }

        btnAutocorrectConservative.setOnClickListener {
            preferences.autoCorrectMode = AutoCorrectMode.CONSERVATIVE
            updateAutoCorrectButtons(AutoCorrectMode.CONSERVATIVE)
        }

        btnAutocorrectAggressive.setOnClickListener {
            preferences.autoCorrectMode = AutoCorrectMode.AGGRESSIVE
            updateAutoCorrectButtons(AutoCorrectMode.AGGRESSIVE)
        }
    }

    private fun updateAutoCorrectButtons(mode: AutoCorrectMode) {
        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        val defaultBgColor = ContextCompat.getColor(this, R.color.surface_card)

        val setButtonActive = { btn: TextView, active: Boolean ->
            val bg = GradientDrawable().apply {
                cornerRadius = dpToPx(8).toFloat()
                setColor(if (active) primaryColor else defaultBgColor)
            }
            btn.background = bg
            btn.setTextColor(if (active) Color.WHITE else ContextCompat.getColor(this, R.color.text_primary))
        }

        setButtonActive(btnAutocorrectOff, mode == AutoCorrectMode.OFF)
        setButtonActive(btnAutocorrectConservative, mode == AutoCorrectMode.CONSERVATIVE)
        setButtonActive(btnAutocorrectAggressive, mode == AutoCorrectMode.AGGRESSIVE)
    }

    private fun setupThemePicker() {
        layoutThemesContainer.removeAllViews()

        val currentSelectedTheme = preferences.theme

        KeyboardTheme.values().forEach { theme ->
            val isSelected = theme == currentSelectedTheme

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                val pad = dpToPx(12)
                setPadding(pad, pad, pad, pad)
                val cardWidth = dpToPx(120)
                layoutParams = LinearLayout.LayoutParams(cardWidth, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dpToPx(10)
                }

                val bg = GradientDrawable().apply {
                    cornerRadius = dpToPx(12).toFloat()
                    setColor(theme.backgroundColor)
                    val strokeColor = if (isSelected) ContextCompat.getColor(this@MainActivity, R.color.secondary) else Color.parseColor("#334155")
                    val strokeWidth = if (isSelected) dpToPx(2) else dpToPx(1)
                    setStroke(strokeWidth, strokeColor)
                }
                background = bg

                val swatchRow = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(24)).apply {
                        bottomMargin = dpToPx(8)
                    }

                    addView(View(this@MainActivity).apply {
                        val keyBg = GradientDrawable().apply {
                            cornerRadius = dpToPx(4).toFloat()
                            setColor(theme.keyNormalColor)
                        }
                        background = keyBg
                        layoutParams = LinearLayout.LayoutParams(dpToPx(22), dpToPx(22)).apply { marginEnd = dpToPx(3) }
                    })

                    addView(View(this@MainActivity).apply {
                        val keyBg = GradientDrawable().apply {
                            cornerRadius = dpToPx(4).toFloat()
                            setColor(theme.keyActionColor)
                        }
                        background = keyBg
                        layoutParams = LinearLayout.LayoutParams(dpToPx(22), dpToPx(22)).apply { marginEnd = dpToPx(3) }
                    })

                    addView(View(this@MainActivity).apply {
                        val keyBg = GradientDrawable().apply {
                            cornerRadius = dpToPx(4).toFloat()
                            setColor(theme.keySpecialColor)
                        }
                        background = keyBg
                        layoutParams = LinearLayout.LayoutParams(dpToPx(22), dpToPx(22))
                    })
                }
                addView(swatchRow)

                val nameTv = TextView(this@MainActivity).apply {
                    text = if (isSelected) "${theme.displayName} ✓" else theme.displayName
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setTextColor(theme.textColorPrimary)
                }
                addView(nameTv)

                setOnClickListener {
                    preferences.theme = theme
                    setupThemePicker()
                }
            }

            layoutThemesContainer.addView(card)
        }
    }

    private fun setupPreferences() {
        switchVoice.isChecked = preferences.isVoiceTypingEnabled
        switchVoice.setOnCheckedChangeListener { _, isChecked ->
            preferences.isVoiceTypingEnabled = isChecked
        }

        switchClipboard.isChecked = preferences.isClipboardHistoryEnabled
        switchClipboard.setOnCheckedChangeListener { _, isChecked ->
            preferences.isClipboardHistoryEnabled = isChecked
        }

        switchNumberRow.isChecked = preferences.isNumberRowEnabled
        switchNumberRow.setOnCheckedChangeListener { _, isChecked ->
            preferences.isNumberRowEnabled = isChecked
        }

        switchKeyPopup.isChecked = preferences.isPopupEnabled
        switchKeyPopup.setOnCheckedChangeListener { _, isChecked ->
            preferences.isPopupEnabled = isChecked
        }

        switchHaptic.isChecked = preferences.isHapticEnabled
        switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            preferences.isHapticEnabled = isChecked
        }

        switchSound.isChecked = preferences.isSoundEnabled
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            preferences.isSoundEnabled = isChecked
        }

        switchAutoCaps.isChecked = preferences.isAutoCapsEnabled
        switchAutoCaps.setOnCheckedChangeListener { _, isChecked ->
            preferences.isAutoCapsEnabled = isChecked
        }
    }

    private fun setupHeightAdjuster() {
        updateHeightButtons(preferences.heightScale)

        btnHeightCompact.setOnClickListener {
            preferences.heightScale = 0.88f
            updateHeightButtons(0.88f)
        }

        btnHeightNormal.setOnClickListener {
            preferences.heightScale = 1.0f
            updateHeightButtons(1.0f)
        }

        btnHeightTall.setOnClickListener {
            preferences.heightScale = 1.15f
            updateHeightButtons(1.15f)
        }
    }

    private fun updateHeightButtons(currentScale: Float) {
        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        val defaultBgColor = ContextCompat.getColor(this, R.color.surface_card)

        val setButtonActive = { btn: TextView, active: Boolean ->
            val bg = GradientDrawable().apply {
                cornerRadius = dpToPx(8).toFloat()
                setColor(if (active) primaryColor else defaultBgColor)
            }
            btn.background = bg
            btn.setTextColor(if (active) Color.WHITE else ContextCompat.getColor(this, R.color.text_primary))
        }

        setButtonActive(btnHeightCompact, currentScale < 0.95f)
        setButtonActive(btnHeightNormal, currentScale in 0.95f..1.05f)
        setButtonActive(btnHeightTall, currentScale > 1.05f)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
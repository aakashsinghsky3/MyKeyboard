package com.example.mykeyboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mykeyboard.model.KeyboardTheme
import com.example.mykeyboard.utils.KeyboardPreferences
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var preferences: KeyboardPreferences

    private lateinit var tvStep1Status: TextView
    private lateinit var tvStep2Status: TextView
    private lateinit var etTestTyping: EditText
    private lateinit var btnClearTest: TextView
    private lateinit var layoutThemesContainer: LinearLayout

    private lateinit var switchNumberRow: MaterialSwitch
    private lateinit var switchKeyPopup: MaterialSwitch
    private lateinit var switchHaptic: MaterialSwitch
    private lateinit var switchSound: MaterialSwitch
    private lateinit var switchAutoCaps: MaterialSwitch

    private lateinit var btnHeightCompact: TextView
    private lateinit var btnHeightNormal: TextView
    private lateinit var btnHeightTall: TextView

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
        setupThemePicker()
        setupPreferences()
        setupHeightAdjuster()
    }

    override fun onResume() {
        super.onResume()
        updateWizardStatus()
    }

    private fun initViews() {
        tvStep1Status = findViewById(R.id.tv_step1_status)
        tvStep2Status = findViewById(R.id.tv_step2_status)
        etTestTyping = findViewById(R.id.et_test_typing)
        btnClearTest = findViewById(R.id.btn_clear_test)
        layoutThemesContainer = findViewById(R.id.layout_themes_container)

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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            updateWizardStatus()
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

                // Swatch preview (3 key shapes)
                val swatchRow = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(24)).apply {
                        bottomMargin = dpToPx(8)
                    }

                    // Key normal
                    addView(View(this@MainActivity).apply {
                        val keyBg = GradientDrawable().apply {
                            cornerRadius = dpToPx(4).toFloat()
                            setColor(theme.keyNormalColor)
                        }
                        background = keyBg
                        layoutParams = LinearLayout.LayoutParams(dpToPx(22), dpToPx(22)).apply { marginEnd = dpToPx(3) }
                    })

                    // Key action
                    addView(View(this@MainActivity).apply {
                        val keyBg = GradientDrawable().apply {
                            cornerRadius = dpToPx(4).toFloat()
                            setColor(theme.keyActionColor)
                        }
                        background = keyBg
                        layoutParams = LinearLayout.LayoutParams(dpToPx(22), dpToPx(22)).apply { marginEnd = dpToPx(3) }
                    })

                    // Key space
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

                // Theme Name
                val nameTv = TextView(this@MainActivity).apply {
                    text = theme.displayName
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setTextColor(theme.textColorPrimary)
                    if (isSelected) {
                        text = "${theme.displayName} ✓"
                    }
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
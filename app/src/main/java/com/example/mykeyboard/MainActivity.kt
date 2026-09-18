package com.example.mykeyboard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mykeyboard.settings.AdvancedPage
import com.example.mykeyboard.settings.AppearancePage
import com.example.mykeyboard.settings.HomePage
import com.example.mykeyboard.settings.KeyboardPage
import com.example.mykeyboard.settings.LanguagePage
import com.example.mykeyboard.settings.SettingsHost
import com.example.mykeyboard.settings.SettingsPage
import com.example.mykeyboard.settings.SettingsTab
import com.example.mykeyboard.settings.SettingsUi
import com.example.mykeyboard.utils.KeyboardPreferences
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import java.io.File
import java.io.FileOutputStream

/**
 * Settings app shell: brand top bar, five destinations in a Material 3 navigation bar,
 * fade-through page transitions. Pages are built lazily and kept alive while the activity lives.
 */
class MainActivity : AppCompatActivity(), SettingsHost, SharedPreferences.OnSharedPreferenceChangeListener {

    override lateinit var prefs: KeyboardPreferences
    override lateinit var ui: SettingsUi

    private lateinit var content: FrameLayout
    private lateinit var navBar: BottomNavigationView
    private val pages = HashMap<SettingsTab, SettingsPage>()
    private var currentTab = SettingsTab.HOME

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) saveCustomBackground(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = KeyboardPreferences(this)
        // Leave the launch (splash) theme before inflating anything.
        setTheme(R.style.Theme_MyKeyboard)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ui = SettingsUi(this)

        currentTab = savedInstanceState?.getString(STATE_TAB)?.let { name -> SettingsTab.values().firstOrNull { it.name == name } } ?: SettingsTab.HOME

        val root = findViewById<FrameLayout>(R.id.main)
        val shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val topBar = buildTopBar()
        shell.addView(topBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        content = FrameLayout(this)
        shell.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        navBar = buildNavBar()
        shell.addView(navBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(shell, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.setPadding(bars.left + ui.dp(20), bars.top + ui.dp(10), bars.right + ui.dp(20), ui.dp(6))
            navBar.setPadding(bars.left, 0, bars.right, bars.bottom)
            content.setPadding(bars.left, 0, bars.right, 0)
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentTab != SettingsTab.HOME) navigateTo(SettingsTab.HOME) else finish()
            }
        })

        navBar.selectedItemId = navId(currentTab)
        showTab(currentTab, animate = false)
        prefs.registerListener(this)
    }

    override fun onDestroy() {
        prefs.unregisterListener(this)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_TAB, currentTab.name)
    }

    override fun onResume() {
        super.onResume()
        pages[currentTab]?.onShown()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // The input-method picker is a dialog: refresh setup status when it closes.
        if (hasFocus && currentTab == SettingsTab.HOME) pages[currentTab]?.onShown()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        pages.values.forEach { it.onPreferenceChanged(key) }
    }

    // ---------------------------------------------------------------------------------------
    // Chrome
    // ---------------------------------------------------------------------------------------

    private fun buildTopBar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(ui.background)
        addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_brand_logo)
            contentDescription = null
        }, LinearLayout.LayoutParams(ui.dp(34), ui.dp(34)).apply { marginEnd = ui.dp(12) })
        addView(ui.text("Keyboard", SettingsUi.Type.TITLE))
    }

    private fun buildNavBar(): BottomNavigationView = BottomNavigationView(this).apply {
        SettingsTab.values().forEach { tab ->
            menu.add(0, navId(tab), tab.ordinal, tab.title).setIcon(tab.iconRes)
        }
        labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        setBackgroundColor(ui.surface)
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        itemIconTintList = ColorStateList(states, intArrayOf(ui.onPrimaryContainer, ui.onSurfaceVariant))
        itemTextColor = ColorStateList(states, intArrayOf(ui.onSurface, ui.onSurfaceVariant))
        itemActiveIndicatorColor = ColorStateList.valueOf(ui.primaryContainer)
        setOnItemSelectedListener { item ->
            val tab = SettingsTab.values().firstOrNull { navId(it) == item.itemId } ?: SettingsTab.HOME
            if (tab != currentTab) showTab(tab, animate = true)
            true
        }
    }

    private fun pageFor(tab: SettingsTab): SettingsPage = pages.getOrPut(tab) {
        when (tab) {
            SettingsTab.HOME -> HomePage(this)
            SettingsTab.APPEARANCE -> AppearancePage(this)
            SettingsTab.KEYBOARD -> KeyboardPage(this)
            SettingsTab.LANGUAGE -> LanguagePage(this)
            SettingsTab.ADVANCED -> AdvancedPage(this)
        }
    }

    /** Material fade-through: outgoing fades out quickly, incoming fades + rises in. */
    private fun showTab(tab: SettingsTab, animate: Boolean) {
        val outgoing = pages[currentTab]?.view?.takeIf { it.parent === content }
        currentTab = tab
        val page = pageFor(tab)
        val incoming = page.view
        if (incoming.parent == null) content.addView(incoming, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        page.onShown()
        if (outgoing != null && outgoing !== incoming) {
            if (animate) {
                outgoing.animate().alpha(0f).setDuration(90).withEndAction { outgoing.visibility = View.GONE; outgoing.alpha = 1f }.start()
            } else {
                outgoing.visibility = View.GONE
            }
        }
        incoming.visibility = View.VISIBLE
        if (animate) {
            incoming.alpha = 0f
            incoming.translationY = ui.dpf(16f)
            incoming.animate().alpha(1f).translationY(0f).setStartDelay(70).setDuration(240).setInterpolator(ui.emphasized).start()
        } else {
            incoming.alpha = 1f
            incoming.translationY = 0f
        }
        incoming.bringToFront()
    }

    // ---------------------------------------------------------------------------------------
    // SettingsHost
    // ---------------------------------------------------------------------------------------

    override fun navigateTo(tab: SettingsTab) {
        if (navBar.selectedItemId != navId(tab)) navBar.selectedItemId = navId(tab)
        else if (tab != currentTab) showTab(tab, animate = true)
    }

    override fun rebuildPages() {
        pages.values.forEach { content.removeView(it.view) }
        pages.clear()
        showTab(currentTab, animate = true)
    }

    override fun pickBackgroundImage() {
        pickImageLauncher.launch("image/*")
    }

    override fun isKeyboardEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
        return imm.enabledInputMethodList.any { it.packageName == packageName }
    }

    override fun isKeyboardSelected(): Boolean {
        val currentIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        return currentIme.contains(packageName)
    }

    override fun openInputMethodSettings() {
        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    }

    override fun showInputMethodPicker() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showInputMethodPicker()
    }

    override fun applyAppThemeMode(mode: String) {
        AppCompatDelegate.setDefaultNightMode(nightModeFor(mode))
    }

    override fun appVersion(): String =
        try { packageManager.getPackageInfo(packageName, 0).versionName ?: "" } catch (_: Exception) { "" }

    /** Menu ids start at 1 (0 is Menu.NONE). */
    private fun navId(tab: SettingsTab) = tab.ordinal + 1

    private fun nightModeFor(mode: String): Int = when (mode) {
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    // ---------------------------------------------------------------------------------------
    // Photo background (unchanged behaviour: centre-crop to keyboard ratio, stored privately)
    // ---------------------------------------------------------------------------------------

    private fun saveCustomBackground(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) {
                Toast.makeText(this, "Unable to decode image", Toast.LENGTH_SHORT).show()
                return
            }

            val croppedBitmap = cropToKeyboardRatio(originalBitmap)
            val file = File(filesDir, "custom_keyboard_bg.png")
            val outputStream = FileOutputStream(file)
            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
            outputStream.close()

            if (croppedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }

            // Re-set the path so listeners fire even when the file name is unchanged.
            setBackgroundPath(file.absolutePath)
            Toast.makeText(this, "Photo background applied", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load photo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setBackgroundPath(path: String) {
        prefs.customBgPath = null
        prefs.customBgPath = path
    }

    private fun cropToKeyboardRatio(source: Bitmap): Bitmap {
        val targetRatio = 2.1f // Standard keyboard width-to-height ratio (~1080 x 514)
        val srcWidth = source.width
        val srcHeight = source.height
        val srcRatio = srcWidth.toFloat() / srcHeight.toFloat()

        val cropWidth: Int
        val cropHeight: Int
        val startX: Int
        val startY: Int

        if (srcRatio > targetRatio) {
            cropHeight = srcHeight
            cropWidth = (srcHeight * targetRatio).toInt()
            startX = (srcWidth - cropWidth) / 2
            startY = 0
        } else {
            cropWidth = srcWidth
            cropHeight = (srcWidth / targetRatio).toInt()
            startX = 0
            startY = (srcHeight - cropHeight) / 2
        }

        val cropped = Bitmap.createBitmap(source, startX, startY, cropWidth, cropHeight)
        return Bitmap.createScaledBitmap(cropped, 1080, 514, true)
    }

    private companion object {
        const val STATE_TAB = "settings_tab"
    }
}

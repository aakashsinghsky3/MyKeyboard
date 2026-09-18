package com.example.mykeyboard.model

import android.content.Context
import android.graphics.Paint
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.mykeyboard.R
import java.util.concurrent.Executors

/** One browsable emoji. [skins] holds the five skin-tone variants when the emoji supports them. */
class EmojiItem(val emoji: String, val skins: Array<String>?, val keywords: String)

class EmojiSection(val header: String, val items: List<EmojiItem>)

class EmojiTab(val id: String, val title: String, val iconResId: Int)

/**
 * Offline emoji catalogue (≈1,900 emojis, Unicode Emoji 17 via emojibase-data, MIT).
 *
 * Loaded once per process from assets/emoji_catalog.tsv on a background thread, filtered to
 * glyphs this device can actually draw (Paint.hasGlyph), and kept in memory. Opening the panel
 * again never re-parses anything.
 */
object EmojiData {

    const val TAB_RECENT = "recent"
    const val TAB_FAVORITES = "favorites"

    val tabs: List<EmojiTab> = listOf(
        EmojiTab(TAB_RECENT, "Recent", R.drawable.ic_tab_recent),
        EmojiTab("smileys", "Smileys", R.drawable.ic_tab_smiley),
        EmojiTab("people", "People", R.drawable.ic_tab_people),
        EmojiTab("animals", "Animals", R.drawable.ic_tab_nature),
        EmojiTab("food", "Food", R.drawable.ic_tab_food),
        EmojiTab("travel", "Travel", R.drawable.ic_tab_travel),
        EmojiTab("activities", "Activities", R.drawable.ic_tab_activities),
        EmojiTab("objects", "Objects", R.drawable.ic_tab_objects),
        EmojiTab("symbols", "Symbols", R.drawable.ic_tab_symbols),
        EmojiTab("flags", "Flags", R.drawable.ic_tab_flags),
        EmojiTab(TAB_FAVORITES, "Favorites", R.drawable.ic_tab_favorites)
    )

    /** Shown in Recent until the user has picked a few emojis. */
    val POPULAR = listOf(
        "😂", "❤️", "🤣", "👍", "😭", "🙏", "😘", "🥰", "😍", "😊", "🎉", "😁", "💕", "🥺", "😅", "🔥",
        "☺️", "🤦", "🤷", "🙄", "😆", "🤗", "😉", "🎂", "🤔", "👏", "🙂", "😳", "🥳", "😎", "👌", "💜",
        "😔", "💪", "✨", "💖", "👀", "😋", "😏", "💯"
    )

    /** Seed for Favorites on first run (the app's previous curated "Favorites" set). */
    val DEFAULT_FAVORITES = listOf(
        "✅", "💖", "✨", "👑", "🌸", "💎", "🫶", "🦋", "🎀", "🌷", "🌹", "🌺", "🌻", "🌼", "💐", "🌟",
        "💫", "🦄", "🧸", "🎨", "🧁", "🍫", "🎂", "🍭", "🍓", "🎁", "💌", "🌈", "💕", "🥰", "☀️", "🌙"
    )

    @Volatile var isLoaded = false
        private set

    /** Sections per tab id (catalogue tabs only). */
    @Volatile private var sectionsByTab: Map<String, List<EmojiSection>> = emptyMap()
    @Volatile private var itemsByEmoji: Map<String, EmojiItem> = emptyMap()
    @Volatile private var allItems: List<EmojiItem> = emptyList()

    private val executor = Executors.newSingleThreadExecutor()
    private val main by lazy { Handler(Looper.getMainLooper()) }
    private val pending = ArrayList<() -> Unit>()
    @Volatile private var loading = false

    fun sections(tabId: String): List<EmojiSection> = sectionsByTab[tabId] ?: emptyList()

    fun item(emoji: String): EmojiItem? = itemsByEmoji[emoji] ?: itemsByEmoji[stripSkinTone(emoji)]

    /** Loads asynchronously; [onReady] runs on the main thread (immediately if already loaded). */
    fun ensureLoaded(context: Context, onReady: () -> Unit) {
        if (isLoaded) { onReady(); return }
        synchronized(pending) {
            pending.add(onReady)
            if (loading) return
            loading = true
        }
        val appContext = context.applicationContext
        executor.execute {
            try { load(appContext) } catch (_: Throwable) { }
            isLoaded = true
            main.post {
                val callbacks = synchronized(pending) { ArrayList(pending).also { pending.clear() } }
                callbacks.forEach { it() }
            }
        }
    }

    private fun load(context: Context) {
        val paint = Paint()
        val maxVersion = maxEmojiVersionForSdk()
        val canCheckGlyph = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        fun supported(emoji: String, version: Float): Boolean {
            if (canCheckGlyph) {
                return try { paint.hasGlyph(emoji) } catch (_: Throwable) { version <= maxVersion }
            }
            return version <= maxVersion
        }

        val byTab = LinkedHashMap<String, MutableList<EmojiSection>>()
        val byEmoji = HashMap<String, EmojiItem>(2600)
        val all = ArrayList<EmojiItem>(2000)
        var currentTab: String? = null
        var currentHeader = ""
        var currentItems = ArrayList<EmojiItem>()

        fun flush() {
            val tab = currentTab ?: return
            if (currentItems.isNotEmpty()) byTab.getOrPut(tab) { ArrayList() }.add(EmojiSection(currentHeader, currentItems))
            currentItems = ArrayList()
        }

        context.assets.open("emoji_catalog.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                if (line.isEmpty() || line[0] == '#') return@forEach
                val parts = line.split('\t')
                if (parts[0] == "S" && parts.size >= 3) {
                    flush()
                    currentTab = parts[1]
                    currentHeader = parts[2]
                    return@forEach
                }
                if (parts.size < 4) return@forEach
                val emoji = parts[0]
                val version = parts[1].toFloatOrNull() ?: 0f
                if (!supported(emoji, version)) return@forEach
                val skins = if (parts[2].isEmpty()) null else parts[2].split(' ').toTypedArray()
                val item = EmojiItem(emoji, skins, parts[3])
                currentItems.add(item)
                if (!byEmoji.containsKey(emoji)) { byEmoji[emoji] = item; all.add(item) }
                skins?.forEach { byEmoji.putIfAbsent(it, item) }
            }
        }
        flush()
        sectionsByTab = byTab
        itemsByEmoji = byEmoji
        allItems = all
    }

    /**
     * Offline keyword search. Whole-word prefix matches rank above substring matches;
     * results are capped so the strip stays light.
     */
    fun search(query: String, limit: Int = 60): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty() || !isLoaded) return emptyList()
        val terms = q.split(' ').filter { it.isNotEmpty() }
        val strong = ArrayList<String>()
        val weak = ArrayList<String>()
        for (item in allItems) {
            val kw = item.keywords
            var allMatch = true
            var allPrefix = true
            for (t in terms) {
                if (!kw.contains(t)) { allMatch = false; break }
                if (!wordStartsWith(kw, t)) allPrefix = false
            }
            if (!allMatch) continue
            if (allPrefix) strong.add(item.emoji) else weak.add(item.emoji)
            if (strong.size >= limit) break
        }
        return (strong + weak).take(limit)
    }

    private fun wordStartsWith(keywords: String, term: String): Boolean {
        var start = 0
        while (start <= keywords.length) {
            if (keywords.startsWith(term, start)) return true
            val next = keywords.indexOf(' ', start)
            if (next < 0) return false
            start = next + 1
        }
        return false
    }

    fun stripSkinTone(emoji: String): String {
        val sb = StringBuilder(emoji.length)
        var i = 0
        while (i < emoji.length) {
            val cp = emoji.codePointAt(i)
            if (cp !in 0x1F3FB..0x1F3FF) sb.appendCodePoint(cp)
            i += Character.charCount(cp)
        }
        return sb.toString()
    }

    /** Conservative fallback when Paint.hasGlyph is unavailable or fails. */
    private fun maxEmojiVersionForSdk(): Float = when (Build.VERSION.SDK_INT) {
        24 -> 3f
        25 -> 4f
        26, 27 -> 5f
        28 -> 11f
        29 -> 12f
        30 -> 13f
        31, 32 -> 13.1f
        33 -> 14f
        34 -> 15f
        35 -> 15.1f
        36 -> 16f
        else -> 17f
    }
}

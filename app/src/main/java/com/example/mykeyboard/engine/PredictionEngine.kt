package com.example.mykeyboard.engine

import android.content.Context

data class SuggestionResult(
    val left: String?,
    val center: String?,
    val right: String?,
    val isAutoCorrect: Boolean = false
)

class PredictionEngine(context: Context) {

    private val userDb = UserDictionaryDb(context)
    private val assetDictionary = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val prefixIndex = java.util.concurrent.ConcurrentHashMap<String, MutableList<Pair<String, Int>>>()

    init {
        // Load 333,000+ word dictionary (3.3 Lakh words) from dictionary.txt.gz asynchronously
        Thread {
            try {
                val tempMap = mutableMapOf<String, Int>()
                val tempPrefixMap = mutableMapOf<String, MutableList<Pair<String, Int>>>()

                val gzipStream = java.util.zip.GZIPInputStream(context.assets.open("dictionary.txt.gz"))
                gzipStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.trim().split("\\s+".toRegex())
                        if (parts.size == 2) {
                            val word = parts[0].lowercase()
                            val freq = parts[1].toIntOrNull() ?: 1
                            if (word.isNotEmpty()) {
                                tempMap[word] = freq

                                val maxLen = minOf(4, word.length)
                                for (len in 1..maxLen) {
                                    val prefix = word.substring(0, len)
                                    val list = tempPrefixMap.getOrPut(prefix) { mutableListOf() }
                                    if (list.size < 500) {
                                        list.add(Pair(word, freq))
                                    }
                                }
                            }
                        }
                    }
                }
                assetDictionary.putAll(tempMap)
                tempPrefixMap.forEach { (k, v) ->
                    prefixIndex[k] = v.sortedByDescending { it.second }.toMutableList()
                }
            } catch (_: Exception) {}
        }.start()
    }

    private val N_GRAM_MAP = mapOf(
        "i am" to listOf("going", "here", "ready", "happy", "doing", "sure", "not", "fine"),
        "i" to listOf("am", "will", "have", "want", "think", "know", "can", "need", "was", "like"),
        "how are" to listOf("you", "things", "they", "we"),
        "how" to listOf("are", "is", "do", "can", "was", "about", "to", "much"),
        "thank" to listOf("you", "god", "everyone", "so"),
        "thank you" to listOf("so", "very", "for", "much", "again"),
        "thanks" to listOf("for", "again", "a lot", "bro", "so"),
        "going" to listOf("to", "home", "there", "back", "well", "on"),
        "going to" to listOf("be", "the", "do", "get", "see", "have", "call"),
        "let me" to listOf("know", "see", "think", "tell", "check", "get"),
        "let me know" to listOf("if", "when", "what", "your"),
        "let" to listOf("me", "us", "it", "them"),
        "see" to listOf("you", "what", "how", "if", "that"),
        "see you" to listOf("soon", "tomorrow", "later", "there", "again"),
        "at the" to listOf("same", "end", "moment", "time", "beginning"),
        "what" to listOf("is", "are", "do", "about", "time", "happened", "did"),
        "what is" to listOf("the", "your", "this", "that", "name"),
        "where" to listOf("are", "is", "were", "did", "can", "you"),
        "where are" to listOf("you", "they", "we"),
        "when" to listOf("are", "is", "will", "can", "do", "you"),
        "can you" to listOf("please", "help", "send", "check", "call", "do"),
        "can" to listOf("you", "i", "we", "be", "do", "get"),
        "please" to listOf("let", "find", "help", "send", "check", "confirm", "call"),
        "looking" to listOf("forward", "for", "at", "into", "good"),
        "looking forward" to listOf("to", "hearing", "seeing"),
        "good" to listOf("morning", "afternoon", "evening", "luck", "job", "night", "idea", "day"),
        "have a" to listOf("great", "good", "nice", "wonderful", "safe"),
        "have" to listOf("a", "been", "to", "you", "done", "got"),
        "would be" to listOf("great", "good", "nice", "awesome", "helpful"),
        "would" to listOf("be", "like", "love", "you", "have"),
        "are you" to listOf("sure", "ready", "there", "coming", "okay", "free"),
        "are" to listOf("you", "they", "we", "not", "there"),
        "do you" to listOf("know", "think", "have", "want", "need", "like"),
        "do" to listOf("not", "you", "it", "this", "that"),
        "will be" to listOf("there", "ready", "able", "great", "done"),
        "will" to listOf("be", "have", "do", "call", "send", "try"),
        "want to" to listOf("know", "see", "go", "be", "have", "do"),
        "want" to listOf("to", "you", "it", "some"),
        "need to" to listOf("know", "do", "get", "be", "have", "check"),
        "need" to listOf("to", "you", "help", "some", "more"),
        "there is" to listOf("a", "no", "some", "an", "nothing"),
        "there" to listOf("is", "are", "will", "was", "were"),
        "it is" to listOf("a", "the", "not", "very", "good", "important"),
        "it" to listOf("is", "was", "will", "would", "has", "can"),
        "we are" to listOf("going", "here", "ready", "looking", "happy"),
        "we" to listOf("are", "have", "will", "can", "need", "want"),
        "they are" to listOf("not", "all", "here", "going", "very"),
        "they" to listOf("are", "have", "will", "were", "said"),
        "talk to" to listOf("you", "me", "him", "her"),
        "talk to you" to listOf("later", "soon", "tomorrow"),
        "take care" to listOf("of", "and", "bro"),
        "happy" to listOf("birthday", "diwali", "new", "holidays"),
        "happy birthday" to listOf("to", "dear", "wishing"),

        // Hinglish Next-Word Predictions
        "kya" to listOf("kar", "rahe", "hua", "hai", "h", "baat", "chal"),
        "kya kar" to listOf("rahe", "raha", "rahi", "ho"),
        "kya hua" to listOf("bhai", "yaar", "kuch", "aaj"),
        "kya chal" to listOf("raha", "rahe", "hai"),
        "kya chal raha" to listOf("hai", "h"),
        "kaise" to listOf("ho", "hai", "h", "chal", "kare"),
        "kaise ho" to listOf("aap", "bhai", "yaar", "sab"),
        "kaha" to listOf("ho", "hai", "ja", "par", "se"),
        "kaha ho" to listOf("aap", "bhai", "yaar"),
        "main" to listOf("bhi", "theek", "hu", "hoon", "aaj", "ghar"),
        "mai" to listOf("bhi", "theek", "hu", "hoon", "aaj", "ghar"),
        "main theek" to listOf("hu", "hoon", "bhai", "yaar"),
        "aap" to listOf("kaise", "kaha", "kya", "batao", "sunao"),
        "aap kaise" to listOf("ho", "hain"),
        "tum" to listOf("kaise", "kaha", "kya", "kab", "aao"),
        "haan" to listOf("bhai", "yaar", "sahi", "bol", "theek"),
        "ha" to listOf("bhai", "yaar", "sahi", "bol", "theek"),
        "theek" to listOf("hai", "h", "bhai", "hona"),
        "theek hai" to listOf("bhai", "yaar", "toh", "fir"),
        "thik" to listOf("hai", "h", "bhai", "hona"),
        "achha" to listOf("hai", "thik", "bhai", "theek"),
        "acha" to listOf("hai", "thik", "bhai", "theek"),
        "bhai" to listOf("kya", "kaha", "suno", "sun", "kaisa"),
        "yaar" to listOf("kya", "suno", "matlab", "sahi", "chal"),
        "bolo" to listOf("bhai", "kya", "sun", "yaar"),
        "batao" to listOf("kya", "bhai", "aaj", "kaisa"),
        "sun" to listOf("na", "bhai", "yaar", "ek"),
        "suno" to listOf("na", "bhai", "yaar", "ek"),
        "chalo" to listOf("theek", "chal", "milte", "aao"),
        "milte" to listOf("hain", "hai", "kal", "shaam"),
        "milte hain" to listOf("phir", "kal", "shaam", "baad"),
        "kal" to listOf("milte", "aana", "karte", "chalenge"),
        "kal milte" to listOf("hain", "bhai", "bye"),
        "aaj" to listOf("nahi", "kya", "aao", "karte", "raat"),
        "aaj raat" to listOf("ko", "milte", "kya"),
        "nahi" to listOf("pata", "yaar", "bhai", "hoga", "hai"),
        "nhi" to listOf("pata", "yaar", "bhai", "hoga", "hai"),
        "pata" to listOf("nahi", "h", "hai", "chal"),
        "bahut" to listOf("badhiya", "accha", "mast", "sahi"),
        "bhot" to listOf("badhiya", "accha", "mast", "sahi"),
        "mast" to listOf("hai", "yaar", "bhai"),
        "badhiya" to listOf("hai", "h", "bhai"),
        "sahi" to listOf("hai", "baat", "h"),
        "sahi hai" to listOf("bhai", "boss", "yaar"),
        "shukriya" to listOf("bhai", "aapka", "yaar"),
        "dhanyawad" to listOf("aapka", "bhai"),
        "namaste" to listOf("ji", "aapko"),
        "kuch" to listOf("nahi", "bhi", "karo", "bolo"),
        "ab" to listOf("kya", "chal", "niklo", "aao"),
        "ghar" to listOf("par", "aao", "ja", "chalo", "pe"),
        "ghar pe" to listOf("sab", "ho", "aao", "hai"),
        "kab" to listOf("aana", "aaoge", "chalna", "hai"),
        "kyun" to listOf("bhai", "nahi", "kya"),
        "kyu" to listOf("bhai", "nahi", "kya"),
        "sab" to listOf("badhiya", "theek", "kuch"),
        "sab theek" to listOf("hai", "h")
    )

    private val COMMON_DICTIONARY = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
        "hello", "thanks", "please", "keyboard", "android", "awesome", "great", "today", "tomorrow",
        "friend", "family", "message", "happy", "ready", "morning", "night", "home", "work", "school",
        "phone", "email", "place", "thing", "love", "help", "need", "call", "start", "finish",

        // High-frequency English dictionary (600+ words)
        "laptop", "laptops", "apple", "apples", "macbook", "iphone", "android", "ipad", "computer", "application",
        "about", "above", "account", "across", "action", "activity", "actually", "address", "almost", "already",
        "also", "although", "always", "amount", "another", "answer", "anyone", "anything", "anyway",
        "around", "article", "available", "awesome", "beautiful", "because", "become", "before", "behind", "believe",
        "between", "billion", "business", "camera", "cannot", "center", "certain", "change", "children", "company",
        "computer", "condition", "consider", "continue", "country", "course", "create", "current", "customer", "daughter",
        "decide", "decision", "definitely", "department", "describe", "design", "detail", "development", "difference", "different",
        "difficult", "director", "discover", "discuss", "discussion", "disease", "doctor", "during", "economy", "education",
        "effect", "effort", "either", "election", "element", "email", "employee", "enough", "entire", "environment",
        "especially", "establish", "evening", "eventually", "everything", "evidence", "exactly", "example", "executive", "experience",
        "explain", "family", "father", "feeling", "financial", "finish", "follow", "former", "forward", "friend",
        "future", "general", "government", "happen", "happy", "health", "history", "hospital", "however", "husband",
        "important", "impossible", "include", "increase", "indeed", "information", "inside", "instead", "interest", "international",
        "internet", "interview", "itself", "knowledge", "language", "leader", "learning", "location", "machine", "management",
        "manager", "market", "marriage", "material", "matter", "medical", "meeting", "member", "mention", "message",
        "messenger", "million", "minute", "moment", "mother", "movement", "national", "nature", "necessary",
        "network", "nevertheless", "newspaper", "nothing", "number", "office", "official", "offline", "online", "operation",
        "opportunity", "option", "organization", "original", "outside", "package", "parent", "particular", "patient", "pattern",
        "payment", "people", "performance", "period", "person", "personal", "phone", "picture", "police", "policy",
        "political", "position", "positive", "possible", "power", "practice", "prepare", "president", "pressure", "pretty",
        "probably", "problem", "process", "produce", "product", "production", "professional", "program", "project", "property",
        "provide", "public", "purpose", "quality", "question", "quickly", "quite", "rather", "really", "reason",
        "receive", "recent", "recently", "recommend", "record", "relationship", "remember", "report", "represent", "request",
        "require", "research", "resource", "respond", "response", "responsibility", "result", "return", "safety", "school",
        "screen", "search", "security", "sentence", "service", "setting", "several", "should", "similar", "simple",
        "simply", "situation", "social", "society", "solution", "someone", "something", "sometimes", "somewhere", "special",
        "specific", "standard", "station", "strategy", "street", "strength", "strong", "student", "subject", "success",
        "successful", "suddenly", "support", "surface", "system", "teacher", "technology", "together", "tomorrow", "tonight",
        "total", "towards", "treatment", "understand", "understanding", "university", "until", "usually", "value", "various",
        "vehicle", "version", "victory", "video", "village", "voice", "waiting", "walking", "watching", "water",
        "weapon", "weather", "website", "welcome", "whatsapp", "whatever", "whether", "which", "while", "white",
        "whole", "window", "within", "without", "woman", "wonderful", "worker", "working", "world", "worry",
        "would", "writer", "writing", "written", "wrong", "yesterday", "yourself",

        // Hinglish Vocabulary Words
        "kya", "kaise", "kaha", "kahan", "kab", "kyu", "kyun", "main", "mai", "hum", "aap", "tum", "tu",
        "haan", "ha", "nahi", "nhi", "na", "theek", "thik", "achha", "acha", "badhiya", "mast", "bhai",
        "yaar", "dost", "bolo", "suno", "sun", "dekho", "chalo", "chal", "milte", "kal", "aaj", "ab",
        "abhi", "raat", "subah", "shaam", "ghar", "kaam", "pata", "hoga", "hona", "karo", "karna",
        "rahe", "raha", "rahi", "hai", "hain", "hoon", "hu", "tha", "thi", "the", "bhi", "bahut", "bhot",
        "kam", "jyada", "zyada", "sahi", "galat", "shukriya", "dhanyawad", "namaste", "alvida", "waise",
        "lekin", "magar", "par", "pe", "se", "ko", "ke", "ki", "ka", "aur", "ya", "toh", "to", "matlab",
        "kuch", "sab", "apna", "apni", "apne", "mera", "meri", "mere", "tera", "teri", "tere", "unka",
        "unki", "unke", "iska", "iski", "iske",

        // Hindi Devanagari Words
        "नमस्ते", "आप", "कैसे", "हैं", "धन्यवाद", "आज", "कल", "समय", "काम", "घर", "दोस्त", "परिवार", "भारत",
        "बात", "सुप्रभात", "शुभरात्रि", "क्या", "कब", "कहाँ", "क्यों", "अच्छा", "हाँ", "नहीं", "ठीक", "अरे",

        // Haryanvi Devanagari & Roman Words
        "रामराम", "किमे", "कड़े", "इब", "थारे", "मारे", "किते", "घणा", "छाछ", "हुक्का", "चौपाल", "बाता",
        "बाळक", "ताऊ", "ताई", "कोन्या", "आछो", "चाले", "धाकड़", "सुणो", "इबै", "काका", "काकी", "छोरा",
        "छोरी", "भीतर", "बाहर", "खाणा", "पीणा", "सोणा", "बढ़िया", "मोज", "कड़ेन", "बेटा", "बेटी", "गाडी",
        "ramram", "kime", "kade", "ib", "thare", "mare", "kite", "ghana", "balak", "tau", "tai", "konya",
        "acho", "chale", "dhakad", "suno", "ibai", "chora", "chori", "bhitar", "bahar", "khana", "peena",
        "sona", "badhiya", "mauj", "batao", "chalo", "rupya", "kharcha", "gaadi", "tractor", "khet"
    )

    fun learnWord(word: String) {
        userDb.learnWord(word)
    }

    fun addCustomWord(word: String) {
        userDb.addCustomWord(word)
    }

    fun getSuggestions(
        prefix: String,
        previousWords: List<String>,
        autoCorrectMode: AutoCorrectMode = AutoCorrectMode.CONSERVATIVE,
        language: String = "en"
    ): SuggestionResult {
        val learnedWords = userDb.getLearnedWords()

        if (prefix.isEmpty()) {
            // Context-based next word prediction
            val context2 = previousWords.takeLast(2).joinToString(" ").lowercase()
            val context1 = previousWords.lastOrNull()?.lowercase() ?: ""

            val predictions = mutableListOf<String>()

            // 1. Try 2-word context match (highest precision)
            N_GRAM_MAP[context2]?.let { list ->
                predictions.addAll(list)
            }

            // 2. Try 1-word context match
            N_GRAM_MAP[context1]?.let { list ->
                list.forEach { if (!predictions.contains(it)) predictions.add(it) }
            }

            // 3. Learned user words that fit
            learnedWords.keys.take(3).forEach {
                if (!predictions.contains(it)) predictions.add(it)
            }

            // 4. Language-specific default high-frequency fallbacks
            val defaults = if (language == "hi") {
                listOf("नमस्ते", "आप", "कैसे", "हैं", "धन्यवाद", "रामराम", "बढ़िया", "काम", "घर")
            } else {
                listOf("the", "you", "to", "is", "in", "bhai", "hai", "and")
            }
            defaults.forEach {
                if (!predictions.contains(it)) predictions.add(it)
            }

            val c0 = predictions.getOrNull(0)
            val c1 = predictions.getOrNull(1)
            val c2 = predictions.getOrNull(2)

            return SuggestionResult(
                left = c1?.let { formatCasing(it, previousWords) },
                center = c0?.let { formatCasing(it, previousWords) },
                right = c2?.let { formatCasing(it, previousWords) }
            )
        }

        val cleanPrefix = prefix.lowercase()

        // 1. Check direct typo engine match
        var autoCorrectMatch = AutoCorrectEngine.getCorrection(prefix, autoCorrectMode)

        // 2. Collect candidate matches with robust scoring
        val candidateScores = mutableMapOf<String, Long>()

        // Priority 1: User learned words
        learnedWords.forEach { (word, freq) ->
            if (word.startsWith(cleanPrefix)) {
                val bonus = if (word == cleanPrefix) 50_000_000L else 0L
                candidateScores[word] = maxOf(candidateScores[word] ?: 0L, 10_000_000L + freq * 1000L + bonus)
            }
        }

        // Priority 2: Fast Prefix Index
        val indexedList = prefixIndex[cleanPrefix.take(minOf(4, cleanPrefix.length))]
        if (indexedList != null) {
            indexedList.forEach { (word, freq) ->
                if (word.startsWith(cleanPrefix)) {
                    val bonus = if (word == cleanPrefix) 50_000_000L else 0L
                    candidateScores[word] = maxOf(candidateScores[word] ?: 0L, freq.toLong() + bonus)
                }
            }
        }

        // Priority 3: Full Asset Dictionary Fallback scan
        assetDictionary.forEach { (word, freq) ->
            if (word.startsWith(cleanPrefix) && !candidateScores.containsKey(word)) {
                val bonus = if (word == cleanPrefix) 50_000_000L else 0L
                candidateScores[word] = maxOf(candidateScores[word] ?: 0L, freq.toLong() + bonus)
            }
        }

        // Priority 4: COMMON_DICTIONARY Guarantee
        COMMON_DICTIONARY.forEach { word ->
            if (word.startsWith(cleanPrefix)) {
                val bonus = if (word == cleanPrefix) 50_000_000L else 0L
                candidateScores[word] = maxOf(candidateScores[word] ?: 0L, 500_000L + bonus)
            }
        }

        // Sort candidates by total score descending
        val allMatches = candidateScores.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .toMutableList()

        // 3. If word is typed with typo and not in dictionary, find closest typo correction
        if (autoCorrectMatch == null && cleanPrefix.length >= 3 && !assetDictionary.containsKey(cleanPrefix) && !learnedWords.containsKey(cleanPrefix)) {
            val closestCorrection = assetDictionary.entries
                .filter { (w, _) -> levenshteinDistance(cleanPrefix, w) <= 2 && abs(cleanPrefix.length - w.length) <= 2 }
                .maxByOrNull { (w, freq) -> freq - (levenshteinDistance(cleanPrefix, w) * 10000) }
                ?.key

            if (closestCorrection != null && closestCorrection != cleanPrefix) {
                autoCorrectMatch = closestCorrection
            }
        }

        val center = autoCorrectMatch ?: allMatches.firstOrNull() ?: prefix
        val otherMatches = allMatches.filter { it.lowercase() != center.lowercase() }.toMutableList()

        var left: String? = if (autoCorrectMatch != null) prefix else if (prefix.lowercase() != center.lowercase()) prefix else otherMatches.removeFirstOrNull()
        var right: String? = otherMatches.removeFirstOrNull()

        if (left == null) {
            left = otherMatches.removeFirstOrNull() ?: prefix
        }

        if (right == null) {
            right = otherMatches.removeFirstOrNull() ?: COMMON_DICTIONARY.firstOrNull { it.lowercase() != center.lowercase() && it.lowercase() != (left?.lowercase() ?: "") }
        }

        return SuggestionResult(
            left = left?.let { matchCasing(prefix, it) },
            center = matchCasing(prefix, center),
            right = right?.let { matchCasing(prefix, it) },
            isAutoCorrect = autoCorrectMatch != null
        )
    }

    private fun matchCasing(original: String, target: String): String {
        return when {
            original.all { it.isUpperCase() } -> target.uppercase()
            original.firstOrNull()?.isUpperCase() == true -> target.replaceFirstChar { it.uppercase() }
            else -> target
        }
    }

    private fun formatCasing(word: String, prevWords: List<String>): String {
        val lastWord = prevWords.lastOrNull() ?: ""
        if (lastWord.endsWith(".") || lastWord.endsWith("?") || lastWord.endsWith("!")) {
            return word.replaceFirstChar { it.uppercase() }
        }
        return word
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun abs(n: Int) = if (n < 0) -n else n
}

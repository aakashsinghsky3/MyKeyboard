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

    private val N_GRAM_MAP = mapOf(
        "i am" to listOf("going", "here", "ready", "happy", "doing", "sure", "not"),
        "i" to listOf("am", "will", "have", "want", "think", "know", "can", "need"),
        "how are" to listOf("you", "things", "they"),
        "how" to listOf("are", "is", "do", "can", "was"),
        "thank" to listOf("you", "god", "everyone"),
        "thank you" to listOf("so", "very", "for", "much"),
        "thanks" to listOf("for", "again", "a lot"),
        "going" to listOf("to", "home", "there", "back", "well"),
        "going to" to listOf("be", "the", "do", "get", "see", "have"),
        "let me" to listOf("know", "see", "think", "tell", "check"),
        "let" to listOf("me", "us", "it", "them"),
        "see" to listOf("you", "what", "how", "if", "that"),
        "see you" to listOf("soon", "tomorrow", "later", "there"),
        "at the" to listOf("same", "end", "moment", "time", "beginning"),
        "what" to listOf("is", "are", "do", "about", "time", "happened"),
        "what is" to listOf("the", "your", "this", "that"),
        "where" to listOf("are", "is", "were", "did", "can"),
        "when" to listOf("are", "is", "will", "can", "do"),
        "can you" to listOf("please", "help", "send", "check", "call"),
        "can" to listOf("you", "i", "we", "be", "do"),
        "please" to listOf("let", "find", "help", "send", "check", "confirm"),
        "looking" to listOf("forward", "for", "at", "into", "good"),
        "looking forward" to listOf("to", "hearing", "seeing"),
        "good" to listOf("morning", "afternoon", "evening", "luck", "job", "night", "idea"),
        "have a" to listOf("great", "good", "nice", "wonderful", "safe"),
        "have" to listOf("a", "been", "to", "you", "done", "got"),
        "would be" to listOf("great", "good", "nice", "awesome", "helpful"),
        "would" to listOf("be", "like", "love", "you", "have"),
        "are you" to listOf("sure", "ready", "there", "coming", "okay"),
        "are" to listOf("you", "they", "we", "not", "there"),
        "do you" to listOf("know", "think", "have", "want", "need", "like"),
        "do" to listOf("not", "you", "it", "this", "that"),
        "will be" to listOf("there", "ready", "able", "great", "done"),
        "will" to listOf("be", "have", "do", "call", "send"),
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

        // Hinglish Next-Word Predictions
        "kya" to listOf("kar", "rahe", "hua", "hai", "h", "baat"),
        "kya kar" to listOf("rahe", "raha", "rahi", "ho"),
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
        "thik" to listOf("hai", "h", "bhai", "hona"),
        "achha" to listOf("hai", "thik", "bhai", "theek"),
        "acha" to listOf("hai", "thik", "bhai", "theek"),
        "bhai" to listOf("kya", "kaha", "suno", "sun", "kaisa"),
        "yaar" to listOf("kya", "suno", "matlab", "sahi", "chal"),
        "bolo" to listOf("bhai", "kya", "sun", "yaar"),
        "sun" to listOf("na", "bhai", "yaar", "ek"),
        "suno" to listOf("na", "bhai", "yaar", "ek"),
        "chalo" to listOf("theek", "chal", "milte", "aao"),
        "milte" to listOf("hain", "hai", "kal", "shaam"),
        "kal" to listOf("milte", "aana", "karte", "chalenge"),
        "aaj" to listOf("nahi", "kya", "aao", "karte"),
        "nahi" to listOf("pata", "yaar", "bhai", "hoga"),
        "nhi" to listOf("pata", "yaar", "bhai", "hoga"),
        "pata" to listOf("nahi", "h", "hai", "chal"),
        "bahut" to listOf("badhiya", "accha", "mast", "sahi"),
        "bhot" to listOf("badhiya", "accha", "mast", "sahi"),
        "mast" to listOf("hai", "yaar", "bhai"),
        "badhiya" to listOf("hai", "h", "bhai"),
        "sahi" to listOf("hai", "baat", "h"),
        "shukriya" to listOf("bhai", "aapka", "yaar"),
        "dhanyawad" to listOf("aapka", "bhai"),
        "namaste" to listOf("ji", "aapko"),
        "kuch" to listOf("nahi", "bhi", "karo", "bolo"),
        "ab" to listOf("kya", "chal", "niklo", "aao"),
        "ghar" to listOf("par", "aao", "ja", "chalo"),
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

        // Hinglish Vocabulary Words
        "kya", "kaise", "kaha", "kahan", "kab", "kyu", "kyun", "main", "mai", "hum", "aap", "tum", "tu",
        "haan", "ha", "nahi", "nhi", "na", "theek", "thik", "achha", "acha", "badhiya", "mast", "bhai",
        "yaar", "dost", "bolo", "suno", "sun", "dekho", "chalo", "chal", "milte", "kal", "aaj", "ab",
        "abhi", "raat", "subah", "shaam", "ghar", "kaam", "pata", "hoga", "hona", "karo", "karna",
        "rahe", "raha", "rahi", "hai", "hain", "hoon", "hu", "tha", "thi", "the", "bhi", "bahut", "bhot",
        "kam", "jyada", "zyada", "sahi", "galat", "shukriya", "dhanyawad", "namaste", "alvida", "waise",
        "lekin", "magar", "par", "pe", "se", "ko", "ke", "ki", "ka", "aur", "ya", "toh", "to", "matlab",
        "kuch", "sab", "apna", "apni", "apne", "mera", "meri", "mere", "tera", "teri", "tere", "unka",
        "unki", "unke", "iska", "iski", "iske"
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
        autoCorrectMode: AutoCorrectMode = AutoCorrectMode.CONSERVATIVE
    ): SuggestionResult {
        val learnedWords = userDb.getLearnedWords()

        if (prefix.isEmpty()) {
            // Context-based next word prediction
            val context1 = previousWords.takeLast(2).joinToString(" ").lowercase()
            val context0 = previousWords.lastOrNull()?.lowercase() ?: ""

            val nextPredictions = N_GRAM_MAP[context1] ?: N_GRAM_MAP[context0] ?: listOf("the", "you", "to")

            val c0 = nextPredictions.getOrNull(0)
            val c1 = nextPredictions.getOrNull(1)
            val c2 = nextPredictions.getOrNull(2)

            return SuggestionResult(
                left = c1?.let { formatCasing(it, previousWords) },
                center = c0?.let { formatCasing(it, previousWords) },
                right = c2?.let { formatCasing(it, previousWords) }
            )
        }

        val cleanPrefix = prefix.lowercase()

        // 1. Check auto-correction
        val autoCorrectMatch = AutoCorrectEngine.getCorrection(prefix, autoCorrectMode)

        // 2. Find prefix matches in learned words + dictionary
        val allMatches = mutableListOf<String>()

        // Learned words first
        learnedWords.keys.filter { it.startsWith(cleanPrefix) }.sortedByDescending { learnedWords[it] ?: 0 }.forEach {
            if (!allMatches.contains(it)) allMatches.add(it)
        }

        // Common dictionary matches
        COMMON_DICTIONARY.filter { it.startsWith(cleanPrefix) }.forEach {
            if (!allMatches.contains(it)) allMatches.add(it)
        }

        var center = autoCorrectMatch ?: allMatches.firstOrNull() ?: prefix
        var left: String? = null
        var right: String? = null

        if (autoCorrectMatch != null) {
            // If autocorrect matched: Center = Autocorrect, Left = Exact raw input, Right = Alternative
            left = prefix
            right = allMatches.firstOrNull { it != autoCorrectMatch.lowercase() }
        } else {
            // Normal prefix matches
            val otherMatches = allMatches.filter { it != center.lowercase() }
            left = if (prefix != center) prefix else otherMatches.getOrNull(0)
            right = otherMatches.getOrNull(if (prefix != center) 0 else 1)

            // If we have context, try adding a context candidate to right
            if (right == null && previousWords.isNotEmpty()) {
                val context = previousWords.last().lowercase()
                right = N_GRAM_MAP[context]?.firstOrNull()
            }
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
}

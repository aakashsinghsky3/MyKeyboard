package com.example.mykeyboard.engine

enum class AutoCorrectMode(val id: String, val displayName: String) {
    OFF("off", "Off"),
    CONSERVATIVE("conservative", "Conservative"),
    AGGRESSIVE("aggressive", "Aggressive");

    companion object {
        fun fromId(id: String?): AutoCorrectMode {
            return values().firstOrNull { it.id == id } ?: CONSERVATIVE
        }
    }
}

object AutoCorrectEngine {

    private val TYPO_MAP = mapOf(
        "teh" to "the",
        "recieve" to "receive",
        "becuase" to "because",
        "dont" to "don't",
        "im" to "I'm",
        "wont" to "won't",
        "cant" to "can't",
        "isnt" to "isn't",
        "arent" to "aren't",
        "didnt" to "didn't",
        "doesnt" to "doesn't",
        "havent" to "haven't",
        "hasnt" to "hasn't",
        "hadnt" to "hadn't",
        "wouldnt" to "wouldn't",
        "couldnt" to "couldn't",
        "shouldnt" to "shouldn't",
        "youre" to "you're",
        "theyre" to "they're",
        "weve" to "we've",
        "youve" to "you've",
        "theyve" to "they've",
        "ive" to "I've",
        "ill" to "I'll",
        "youll" to "you'll",
        "theyll" to "they'll",
        "shell" to "she'll",
        "thier" to "their",
        "waht" to "what",
        "adn" to "and",
        "alot" to "a lot",
        "definately" to "definitely",
        "definitly" to "definitely",
        "seperate" to "separate",
        "untill" to "until",
        "occured" to "occurred",
        "truely" to "truly",
        "wierd" to "weird",
        "tommorow" to "tomorrow",
        "tommorrow" to "tomorrow",
        "tomorow" to "tomorrow",
        "goverment" to "government",
        "realy" to "really",
        "beleive" to "believe",
        "acheive" to "achieve",
        "accross" to "across",
        "neccessary" to "necessary",
        "dissapoint" to "disappoint",
        "begining" to "beginning",
        "pronounciation" to "pronunciation",
        "suprise" to "surprise",
        "freind" to "friend",
        "untill" to "until",
        "calender" to "calendar",
        "catagory" to "category",
        "cemetary" to "cemetery",
        "collegue" to "colleague",
        "concious" to "conscious",
        "definately" to "definitely",
        "embarass" to "embarrass",
        "enviroment" to "environment",
        "existance" to "existence",
        "foriegn" to "foreign",
        "guarentee" to "guarantee",
        "happenning" to "happening",
        "harasment" to "harassment",
        "heirarchy" to "hierarchy",
        "humerous" to "humorous",
        "ignorence" to "ignorance",
        "immediatly" to "immediately",
        "independant" to "independent",
        "intelligance" to "intelligence",
        "judgement" to "judgment",
        "knowlege" to "knowledge",
        "liason" to "liaison",
        "libary" to "library",
        "lisence" to "license",
        "maintenence" to "maintenance",
        "mischevious" to "mischievous",
        "millenium" to "millennium",
        "miniture" to "miniature",
        "necesary" to "necessary",
        "noticable" to "noticeable",
        "ocasion" to "occasion",
        "occurance" to "occurrence",
        "paralell" to "parallel",
        "pastime" to "pastime",
        "persistant" to "persistent",
        "posession" to "possession",
        "prefered" to "preferred",
        "presance" to "presence",
        "privelege" to "privilege",
        "probaly" to "probably",
        "prolly" to "probably",
        "publically" to "publicly",
        "questionaire" to "questionnaire",
        "recommand" to "recommend",
        "refered" to "referred",
        "relavent" to "relevant",
        "religous" to "religious",
        "rememberance" to "remembrance",
        "resistence" to "resistance",
        "rythm" to "rhythm",
        "schedule" to "schedule",
        "sieze" to "seize",
        "sensable" to "sensible",
        "seperate" to "separate",
        "succesful" to "successful",
        "suceed" to "succeed",
        "supercede" to "supersede",
        "tendancy" to "tendency",
        "therefor" to "therefore",
        "threshhold" to "threshold",
        "tomatos" to "tomatoes",
        "tounge" to "tongue",
        "truely" to "truly",
        "unforseen" to "unforeseen",
        "unfortunatly" to "unfortunately",
        "uninteligable" to "unintelligible",
        "usefull" to "useful",
        "vaccum" to "vacuum",
        "vehical" to "vehicle",
        "visable" to "visible",
        "wearable" to "wearable",
        "weather" to "whether",
        "wensday" to "Wednesday",
        "wich" to "which",
        "yeild" to "yield",
        "woudl" to "would",
        "hsould" to "should",
        "coudl" to "could",
        "taht" to "that",
        "yuo" to "you",
        "mya" to "may",
        "whcih" to "which",
        "peopel" to "people",
        "knwo" to "know",
        "jsut" to "just",
        "hve" to "have",
        "grea" to "great",
        "somethin" to "something",
        "beuatiful" to "beautiful",
        "diffrent" to "different",
        "againt" to "against",
        "probaly" to "probably",
        "rhi" to "rahi",
        "kr" to "kar",
        "aple" to "apple",
        "aplel" to "apple",
        "laptoop" to "laptop",
        "laptp" to "laptop",
        "exampel" to "example",
        "exampal" to "example",
        "watc" to "watch",
        "watcd" to "watched",
        "comming" to "coming",
        "goind" to "going",
        "runing" to "running",
        "swiming" to "swimming",
        "planing" to "planning",
        "toda" to "today",
        "yestarday" to "yesterday",
        "sould" to "should",
        "woud" to "would",
        "coud" to "could",
        "hloo" to "hello",
        "helo" to "hello",
        "hlo" to "hello",
        "pls" to "please",
        "plz" to "please",
        "bcz" to "because",
        "abt" to "about"
    )

    val QWERTY_ADJACENT_MAP: Map<Char, List<Char>> = mapOf(
        'q' to listOf('w', 'a', 's'),
        'w' to listOf('q', 'e', 'a', 's', 'd'),
        'e' to listOf('w', 'r', 's', 'd', 'f'),
        'r' to listOf('e', 't', 'd', 'f', 'g'),
        't' to listOf('r', 'y', 'f', 'g', 'h'),
        'y' to listOf('t', 'u', 'g', 'h', 'j'),
        'u' to listOf('y', 'i', 'h', 'j', 'k'),
        'i' to listOf('u', 'o', 'j', 'k', 'l'),
        'o' to listOf('i', 'p', 'k', 'l'),
        'p' to listOf('o', 'l'),
        'a' to listOf('q', 'w', 's', 'z'),
        's' to listOf('a', 'd', 'w', 'e', 'z', 'x'),
        'd' to listOf('s', 'f', 'e', 'r', 'x', 'c'),
        'f' to listOf('d', 'g', 'r', 't', 'c', 'v'),
        'g' to listOf('f', 'h', 't', 'y', 'v', 'b'),
        'h' to listOf('g', 'j', 'y', 'u', 'b', 'n'),
        'j' to listOf('h', 'k', 'u', 'i', 'n', 'm'),
        'k' to listOf('j', 'l', 'i', 'o', 'm'),
        'l' to listOf('k', 'o', 'p'),
        'z' to listOf('a', 's', 'x'),
        'x' to listOf('z', 'c', 's', 'd'),
        'c' to listOf('x', 'v', 'd', 'f'),
        'v' to listOf('c', 'b', 'f', 'g'),
        'b' to listOf('v', 'n', 'g', 'h'),
        'n' to listOf('b', 'm', 'h', 'j'),
        'm' to listOf('n', 'j', 'k')
    )

    fun getCorrection(word: String, mode: AutoCorrectMode, wordValidator: ((String) -> Int?)? = null): String? {
        if (mode == AutoCorrectMode.OFF || word.length < 2) return null

        val lower = word.lowercase()
        val direct = TYPO_MAP[lower]
        if (direct != null) {
            return matchCasing(word, direct)
        }

        if (wordValidator != null) {
            // If the word itself is already in the dictionary with high frequency, don't autocorrect in conservative mode
            val exactFreq = wordValidator(lower)
            if (mode == AutoCorrectMode.CONSERVATIVE && exactFreq != null && exactFreq > 500) {
                return null
            }

            val candidates = mutableMapOf<String, Long>()

            // 1. Proximity: Adjacent key substitutions (e.g. wjen -> when, trh -> try, wlrds -> words, worda -> words)
            for (i in 0 until lower.length) {
                val adjList = QWERTY_ADJACENT_MAP[lower[i]] ?: emptyList()
                for (adj in adjList) {
                    val candidate = lower.substring(0, i) + adj + lower.substring(i + 1)
                    wordValidator(candidate)?.let { freq ->
                        candidates[candidate] = maxOf(candidates[candidate] ?: 0L, freq.toLong() * 2L + 15_000_000L)
                    }
                }
            }

            // 2. Extra tap: Single-character deletion (e.g. ftast -> fast, miising -> missing)
            if (lower.length >= 3) {
                for (i in 0 until lower.length) {
                    val candidate = lower.removeRange(i, i + 1)
                    if (candidate.length >= 2) {
                        wordValidator(candidate)?.let { freq ->
                            candidates[candidate] = maxOf(candidates[candidate] ?: 0L, freq.toLong() + 10_000_000L)
                        }
                    }
                }
            }

            // 3. Transposition: Swap adjacent characters (e.g. adn -> and, wrod -> word)
            for (i in 0 until lower.length - 1) {
                val candidate = lower.substring(0, i) + lower[i + 1] + lower[i] + lower.substring(i + 2)
                wordValidator(candidate)?.let { freq ->
                    candidates[candidate] = maxOf(candidates[candidate] ?: 0L, freq.toLong() * 2L + 12_000_000L)
                }
            }

            // 4. Missed tap: Single-character insertion (e.g. nd -> and, re -> are)
            if (lower.length in 2..6) {
                val commonInserts = charArrayOf('a', 'e', 'i', 'o', 'u', 's', 't', 'r', 'n', 'l', 'h')
                for (i in 0..lower.length) {
                    for (c in commonInserts) {
                        val candidate = lower.substring(0, i) + c + lower.substring(i)
                        wordValidator(candidate)?.let { freq ->
                            candidates[candidate] = maxOf(candidates[candidate] ?: 0L, freq.toLong())
                        }
                    }
                }
            }

            if (candidates.isNotEmpty()) {
                val best = candidates.maxByOrNull { it.value }?.key
                if (best != null && best != lower) {
                    return matchCasing(word, best)
                }
            }
        }

        if (mode == AutoCorrectMode.AGGRESSIVE) {
            // Check for close edit distance 1 with typo dictionary
            for ((typo, correct) in TYPO_MAP) {
                if (levenshteinDistance(lower, typo) <= 1 && abs(lower.length - typo.length) <= 1) {
                    return matchCasing(word, correct)
                }
            }
        }

        return null
    }

    private fun matchCasing(original: String, target: String): String {
        return when {
            original.all { it.isUpperCase() } -> target.uppercase()
            original.firstOrNull()?.isUpperCase() == true -> target.replaceFirstChar { it.uppercase() }
            else -> target
        }
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

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
        "yeild" to "yield"
    )

    fun getCorrection(word: String, mode: AutoCorrectMode): String? {
        if (mode == AutoCorrectMode.OFF || word.length < 2) return null

        val lower = word.lowercase()
        val direct = TYPO_MAP[lower]
        if (direct != null) {
            return matchCasing(word, direct)
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

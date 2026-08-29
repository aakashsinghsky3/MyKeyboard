package com.example.mykeyboard.engine

object ProfessionalToneEngine {

    private data class PatternRule(
        val keywords: List<String>,
        val professionalPhrases: List<String>
    )

    private val RULES = listOf(
        // 1. Sick & Leave Requests
        PatternRule(
            keywords = listOf("sick", "not well", "unwell", "fever", "cant come", "cannot come", "not coming", "taking leave"),
            professionalPhrases = listOf(
                "I am unable to attend work today due to health reasons.",
                "I am currently feeling unwell and will be taking sick leave today.",
                "Please accept this note regarding my absence today due to illness."
            )
        ),
        // 2. Urgent & Document Requests
        PatternRule(
            keywords = listOf("send me", "give me", "asap", "fast", "urgent", "report", "file"),
            professionalPhrases = listOf(
                "Could you please share the requested details at your earliest convenience?",
                "Kindly provide the relevant update when you have a moment.",
                "I would appreciate it if you could forward the files at your convenience."
            )
        ),
        // 3. Postponing & Scheduling
        PatternRule(
            keywords = listOf("do it later", "will do later", "later", "not now", "busy right now", "after some time"),
            professionalPhrases = listOf(
                "I will address this matter at the earliest opportunity.",
                "I am currently prioritizing an urgent task and will revert shortly.",
                "I will follow up on this item as soon as possible."
            )
        ),
        // 4. Apologies & Late Responses
        PatternRule(
            keywords = listOf("sorry", "sorry for delay", "late reply", "sorry late", "delay"),
            professionalPhrases = listOf(
                "Apologies for the delayed response.",
                "Thank you for your patience; regarding your query...",
                "I apologize for the delay in getting back to you."
            )
        ),
        // 5. Thanks & Appreciation
        PatternRule(
            keywords = listOf("thanks", "thx", "thanks bro", "thank you bro", "thx bro"),
            professionalPhrases = listOf(
                "Thank you very much for your assistance. Best regards.",
                "Much appreciated. Thank you for your support.",
                "Thank you for your prompt help with this."
            )
        ),
        // 6. Inquiries & Progress Checks
        PatternRule(
            keywords = listOf("kya kar rahe ho", "kya hua", "what are you doing", "status", "update", "progress"),
            professionalPhrases = listOf(
                "Hope you are doing well. Could you please provide a status update?",
                "I am writing to inquire about the progress of this task.",
                "Please let me know if there are any updates regarding this matter."
            )
        ),
        // 7. Meetings & Calls
        PatternRule(
            keywords = listOf("call me", "ping me", "meet tomorrow", "sync", "talk", "free"),
            professionalPhrases = listOf(
                "Please let me know a suitable time for a brief call.",
                "Would tomorrow be convenient for a quick sync?",
                "Feel free to connect at your earliest convenience."
            )
        )
    )

    fun getProfessionalRephrasings(rawInput: String): List<String> {
        val clean = rawInput.trim()
        if (clean.isEmpty()) return emptyList()

        val lower = clean.lowercase()
        val results = mutableListOf<String>()

        // 1. Check matching pattern rules
        for (rule in RULES) {
            if (rule.keywords.any { lower.contains(it) }) {
                rule.professionalPhrases.forEach { phrase ->
                    if (!results.contains(phrase)) {
                        results.add(phrase)
                    }
                }
            }
        }

        // 2. Add an auto-formatted polished version of the original input
        val polishedInput = polishRawSentence(clean)
        if (!results.contains(polishedInput) && polishedInput.isNotBlank()) {
            results.add(0, polishedInput)
        }

        // 3. Fallback corporate greetings/phrases if no specific match
        if (results.isEmpty()) {
            results.add("Please let me know if any further information is needed.")
            results.add("Looking forward to your response. Best regards.")
        }

        return results.take(3)
    }

    private fun polishRawSentence(text: String): String {
        var formatted = text

        // Replace casual slang & abbreviations
        val replacements = mapOf(
            "\\bim\\b" to "I am",
            "\\bcant\\b" to "cannot",
            "\\bwont\\b" to "will not",
            "\\bdont\\b" to "do not",
            "\\brn\\b" to "right now",
            "\\basap\\b" to "at your earliest convenience",
            "\\bpls\\b" to "please",
            "\\bplz\\b" to "please",
            "\\bthx\\b" to "thank you",
            "\\br\\b" to "are",
            "\\bu\\b" to "you",
            "\\bur\\b" to "your"
        )

        for ((key, value) in replacements) {
            formatted = formatted.replace(Regex(key, RegexOption.IGNORE_CASE), value)
        }

        // Clean up spacing & capitalization
        formatted = formatted.trim()
        if (formatted.isNotEmpty()) {
            formatted = formatted.replaceFirstChar { it.uppercase() }
            if (!formatted.endsWith(".") && !formatted.endsWith("?") && !formatted.endsWith("!")) {
                formatted += "."
            }
        }

        return formatted
    }
}

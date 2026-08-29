package com.example.mykeyboard.engine

object ProfessionalToneEngine {

    private data class PatternRule(
        val keywords: List<String>,
        val professionalPhrases: List<String>
    )

    private val RULES = listOf(
        // 1. Sick & Leave Requests
        PatternRule(
            keywords = listOf("sick", "ill", "fever", "unwell", "cant come", "cannot come", "not coming", "taking leave", "absent", "off today", "doctor"),
            professionalPhrases = listOf(
                "I am unable to attend work today due to health reasons.",
                "I am currently feeling unwell and will be taking sick leave today.",
                "Please accept this notification regarding my absence today due to illness."
            )
        ),
        // 2. Document & File Requests
        PatternRule(
            keywords = listOf("send", "give", "asap", "fast", "urgent", "report", "file", "doc", "pdf", "share", "need this", "attachment"),
            professionalPhrases = listOf(
                "Could you please share the requested details at your earliest convenience?",
                "Kindly provide the relevant file or report when you have a moment.",
                "I would appreciate it if you could forward the relevant files."
            )
        ),
        // 3. Postponing & Busy State
        PatternRule(
            keywords = listOf("later", "not now", "busy", "doing something", "after some time", "will do later", "too busy", "free later"),
            professionalPhrases = listOf(
                "I am currently occupied with a pressing task and will revert shortly.",
                "I will address this matter at the earliest opportunity.",
                "I am currently prioritizing another item and will follow up soon."
            )
        ),
        // 4. Apologies & Delays
        PatternRule(
            keywords = listOf("sorry", "late", "delay", "delayed", "stuck", "traffic", "sorry for delay", "late reply"),
            professionalPhrases = listOf(
                "Apologies for the delayed response.",
                "Thank you for your patience; regarding your query...",
                "I apologize for the delay in getting back to you."
            )
        ),
        // 5. Thanks & Gratitude
        PatternRule(
            keywords = listOf("thanks", "thx", "thank", "thank you", "appreciate", "bro"),
            professionalPhrases = listOf(
                "Thank you very much for your assistance. Best regards.",
                "Much appreciated. Thank you for your valuable support.",
                "Thank you for your prompt help with this item."
            )
        ),
        // 6. Status & Progress Checks
        PatternRule(
            keywords = listOf("status", "update", "progress", "what happened", "kya hua", "kya kar", "where is", "any news"),
            professionalPhrases = listOf(
                "Hope you are well. Could you please provide a status update on this?",
                "I am writing to inquire about the progress of this task.",
                "Please let me know if there are any updates regarding this matter."
            )
        ),
        // 7. Meetings & Calls
        PatternRule(
            keywords = listOf("call", "ping", "talk", "meet", "sync", "discussion", "free for call"),
            professionalPhrases = listOf(
                "Please let me know a suitable time for a brief call.",
                "Would tomorrow be convenient for a quick sync?",
                "Feel free to connect at your earliest convenience."
            )
        ),
        // 8. Confirmation & Acceptance
        PatternRule(
            keywords = listOf("ok", "okay", "fine", "sure", "done", "got it", "agreed", "cool"),
            professionalPhrases = listOf(
                "Acknowledged. I will proceed accordingly.",
                "Thank you for the update. Confirmed.",
                "That sounds appropriate. I will move forward with this."
            )
        ),
        // 9. Inability & Rejection
        PatternRule(
            keywords = listOf("cant", "cannot", "impossible", "no way", "wont", "unable", "not possible"),
            professionalPhrases = listOf(
                "Regrettably, I am unable to proceed with this request at present.",
                "Due to prior commitments, I will be unable to accommodate this.",
                "I apologize, but this falls outside our current capacity."
            )
        ),
        // 10. Hinglish / Casual Phrases
        PatternRule(
            keywords = listOf("kya", "kaise", "kaha", "kab", "batao", "suno", "bhai", "yaar", "ghar", "kaam"),
            professionalPhrases = listOf(
                "Hope you are doing well. Please let me know your current status.",
                "Could you please clarify this requirement when convenient?",
                "I am following up regarding our previous discussion."
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
        if (results.size < 3) {
            val fallbacks = listOf(
                "Please let me know if any further information is needed.",
                "Looking forward to your response. Best regards.",
                "Thank you for your attention to this matter."
            )
            fallbacks.forEach {
                if (!results.contains(it)) results.add(it)
            }
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

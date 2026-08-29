package com.example.mykeyboard.utils

object EmojiVariantsHelper {

    val SKIN_TONES = listOf("🏻", "🏼", "🏽", "🏾", "🏿")

    private val GENDER_PERSONS = mapOf(
        "👮" to Pair("👮‍♀️", "👮‍♂️"),
        "🏃" to Pair("🏃‍♀️", "🏃‍♂️"),
        "🚶" to Pair("🚶‍♀️", "🚶‍♂️"),
        "🤷" to Pair("🤷‍♀️", "🤷‍♂️"),
        "🤦" to Pair("🤦‍♀️", "🤦‍♂️"),
        "🙋" to Pair("🙋‍♀️", "🙋‍♂️"),
        "💁" to Pair("💁‍♀️", "💁‍♂️"),
        "🙇" to Pair("🙇‍♀️", "🙇‍♂️"),
        "🙅" to Pair("🙅‍♀️", "🙅‍♂️"),
        "🙆" to Pair("🙆‍♀️", "🙆‍♂️"),
        "🙍" to Pair("🙍‍♀️", "🙍‍♂️"),
        "🙎" to Pair("🙎‍♀️", "🙎‍♂️"),
        "👷" to Pair("👷‍♀️", "👷‍♂️"),
        "🕵️" to Pair("🕵️‍♀️", "🕵️‍♂️"),
        "🧑‍💻" to Pair("👩‍💻", "👨‍💻"),
        "🧑‍🍳" to Pair("👩‍🍳", "👨‍🍳"),
        "🧑‍⚕️" to Pair("👩‍⚕️", "👨‍⚕️"),
        "🧑‍🎓" to Pair("👩‍🎓", "👨‍🎓"),
        "🧑‍🏫" to Pair("👩‍🏫", "👨‍🏫"),
        "🧑‍🔬" to Pair("👩‍🔬", "👨‍🔬"),
        "🧑‍🎨" to Pair("👩‍🎨", "👨‍🎨"),
        "🧑‍✈️" to Pair("👩‍✈️", "👨‍✈️"),
        "🧑‍🚒" to Pair("👩‍🚒", "👨‍🚒"),
        "🧘" to Pair("🧘‍♀️", "🧘‍♂️"),
        "🏋️" to Pair("🏋️‍♀️", "🏋️‍♂️"),
        "🚴" to Pair("🚴‍♀️", "🚴‍♂️")
    )

    fun getVariants(emoji: String): List<String> {
        val list = mutableListOf<String>()

        // Strip any existing skin tone
        val base = emoji.replace(Regex("[\uD83C\uDFFB-\uD83C\uDFFF]"), "")

        // Check if genderable
        val pair = GENDER_PERSONS[base]
        if (pair != null) {
            val female = pair.first
            val male = pair.second

            list.add(female)
            SKIN_TONES.forEach { tone -> list.add(applyTone(female, tone)) }

            list.add(male)
            SKIN_TONES.forEach { tone -> list.add(applyTone(male, tone)) }

            list.add(base)
            SKIN_TONES.forEach { tone -> list.add(applyTone(base, tone)) }
        } else {
            list.add(base)
            SKIN_TONES.forEach { tone -> list.add(applyTone(base, tone)) }
        }

        return list.distinct()
    }

    private fun applyTone(baseEmoji: String, skinTone: String): String {
        if (baseEmoji.isEmpty()) return baseEmoji
        val firstCodepointLength = if (Character.isHighSurrogate(baseEmoji[0]) && baseEmoji.length > 1) 2 else 1
        val head = baseEmoji.substring(0, firstCodepointLength)
        val tail = baseEmoji.substring(firstCodepointLength)
        return head + skinTone + tail
    }
}

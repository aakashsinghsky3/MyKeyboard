package com.example.mykeyboard.utils

object EmojiVariantsHelper {

    val SKIN_TONES = listOf("🏻", "🏼", "🏽", "🏾", "🏿")

    private val HUMAN_GESTURES_AND_PEOPLE = setOf(
        "👋", "🤚", "🖐️", "✋", "🖖", "🫱", "🫲", "🫳", "🫴", "👌",
        "🤌", "🤏", "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉",
        "👆", "🖕", "👇", "☝️", "🫵", "👍", "👎", "✊", "👊", "🤛",
        "🤜", "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🙏", "✍️", "💅",
        "🤳", "💪", "🦵", "🦶", "👂", "🦻", "👃", "👶", "🧒", "👦",
        "👧", "🧑", "👱", "👨", "👩", "🧓", "👴", "👵", "🚶", "🏃",
        "🕺", "💃", "🕴️", "🧘", "🏄", "🏊", "🏋️", "🚴", "👮", "🕵️",
        "💂", "🥷", "👷", "🤴", "👸", "👳", "👲", "🧕", "🤵", "👰",
        "🤱", "👼", "🦸", "🦹", "🧙", "🧚", "🧛", "🧜", "🧝"
    )

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

        // Strip existing skin tone modifiers
        val base = emoji.replace(Regex("[\uD83C\uDFFB-\uD83C\uDFFF]"), "")

        val genderPair = GENDER_PERSONS[base]
        if (genderPair != null) {
            val female = genderPair.first
            val male = genderPair.second

            list.add(female)
            SKIN_TONES.forEach { tone -> list.add(applyTone(female, tone)) }

            list.add(male)
            SKIN_TONES.forEach { tone -> list.add(applyTone(male, tone)) }

            list.add(base)
            SKIN_TONES.forEach { tone -> list.add(applyTone(base, tone)) }
        } else if (HUMAN_GESTURES_AND_PEOPLE.contains(base)) {
            list.add(base)
            SKIN_TONES.forEach { tone -> list.add(applyTone(base, tone)) }
        } else {
            // Non-human emojis (smileys, hearts, objects, flags, food) do NOT get skin tones!
            return emptyList()
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

package com.example.mykeyboard.model

import android.graphics.Color

enum class KeyboardTheme(
    val id: String,
    val displayName: String,
    val backgroundColor: Int,
    val keyNormalColor: Int,
    val keySpecialColor: Int,
    val keyActionColor: Int,
    val keySpaceColor: Int,
    val textColorPrimary: Int,
    val textColorSecondary: Int,
    val actionTextColor: Int,
    val suggestionBgColor: Int,
    val suggestionTextColor: Int,
    val popupBgColor: Int,
    val popupTextColor: Int,
    val rippleColor: Int,
    val isDark: Boolean
) {
    AMOLED_MIDNIGHT(
        id = "amoled_midnight",
        displayName = "AMOLED Pitch Black (OLED)",
        backgroundColor = Color.parseColor("#000000"),
        keyNormalColor = Color.parseColor("#141414"),
        keySpecialColor = Color.parseColor("#222222"),
        keyActionColor = Color.parseColor("#007AFF"),
        keySpaceColor = Color.parseColor("#141414"),
        textColorPrimary = Color.parseColor("#FFFFFF"),
        textColorSecondary = Color.parseColor("#8E8E93"),
        actionTextColor = Color.parseColor("#FFFFFF"),
        suggestionBgColor = Color.parseColor("#000000"),
        suggestionTextColor = Color.parseColor("#FFFFFF"),
        popupBgColor = Color.parseColor("#2C2C2E"),
        popupTextColor = Color.parseColor("#FFFFFF"),
        rippleColor = Color.parseColor("#3A3A3C"),
        isDark = true
    ),
    MATTE_DARK(
        id = "material_dark",
        displayName = "Gboard Dark Slate",
        backgroundColor = Color.parseColor("#18181B"),
        keyNormalColor = Color.parseColor("#27272A"),
        keySpecialColor = Color.parseColor("#3F3F46"),
        keyActionColor = Color.parseColor("#3B82F6"),
        keySpaceColor = Color.parseColor("#27272A"),
        textColorPrimary = Color.parseColor("#F4F4F5"),
        textColorSecondary = Color.parseColor("#A1A1AA"),
        actionTextColor = Color.parseColor("#FFFFFF"),
        suggestionBgColor = Color.parseColor("#18181B"),
        suggestionTextColor = Color.parseColor("#F4F4F5"),
        popupBgColor = Color.parseColor("#27272A"),
        popupTextColor = Color.parseColor("#F4F4F5"),
        rippleColor = Color.parseColor("#52525B"),
        isDark = true
    ),
    PASTEL_PINK_BLUSH(
        id = "gboard_peach",
        displayName = "Pastel Pink Blush",
        backgroundColor = Color.parseColor("#FDF2F4"),
        keyNormalColor = Color.parseColor("#FFFFFF"),
        keySpecialColor = Color.parseColor("#FCE7F3"),
        keyActionColor = Color.parseColor("#F43F5E"),
        keySpaceColor = Color.parseColor("#FFFFFF"),
        textColorPrimary = Color.parseColor("#1E293B"),
        textColorSecondary = Color.parseColor("#94A3B8"),
        actionTextColor = Color.parseColor("#FFFFFF"),
        suggestionBgColor = Color.parseColor("#FDF2F4"),
        suggestionTextColor = Color.parseColor("#1E293B"),
        popupBgColor = Color.parseColor("#FFFFFF"),
        popupTextColor = Color.parseColor("#1E293B"),
        rippleColor = Color.parseColor("#FBCFE8"),
        isDark = false
    ),
    CYBER_NEON_NIGHT(
        id = "cyber_neon",
        displayName = "Cyber Neon Cyan",
        backgroundColor = Color.parseColor("#0F172A"),
        keyNormalColor = Color.parseColor("#1E293B"),
        keySpecialColor = Color.parseColor("#334155"),
        keyActionColor = Color.parseColor("#06B6D4"),
        keySpaceColor = Color.parseColor("#1E293B"),
        textColorPrimary = Color.parseColor("#F8FAFC"),
        textColorSecondary = Color.parseColor("#38BDF8"),
        actionTextColor = Color.parseColor("#0F172A"),
        suggestionBgColor = Color.parseColor("#0F172A"),
        suggestionTextColor = Color.parseColor("#F8FAFC"),
        popupBgColor = Color.parseColor("#1E293B"),
        popupTextColor = Color.parseColor("#F8FAFC"),
        rippleColor = Color.parseColor("#475569"),
        isDark = true
    ),
    FOREST_EMERALD(
        id = "emerald_green",
        displayName = "Forest Emerald",
        backgroundColor = Color.parseColor("#064E3B"),
        keyNormalColor = Color.parseColor("#047857"),
        keySpecialColor = Color.parseColor("#065F46"),
        keyActionColor = Color.parseColor("#10B981"),
        keySpaceColor = Color.parseColor("#047857"),
        textColorPrimary = Color.parseColor("#ECFDF5"),
        textColorSecondary = Color.parseColor("#A7F3D0"),
        actionTextColor = Color.parseColor("#064E3B"),
        suggestionBgColor = Color.parseColor("#064E3B"),
        suggestionTextColor = Color.parseColor("#ECFDF5"),
        popupBgColor = Color.parseColor("#047857"),
        popupTextColor = Color.parseColor("#ECFDF5"),
        rippleColor = Color.parseColor("#059669"),
        isDark = true
    ),
    ROYAL_PURPLE(
        id = "royal_purple",
        displayName = "Royal Purple Midnight",
        backgroundColor = Color.parseColor("#2E1065"),
        keyNormalColor = Color.parseColor("#3B0764"),
        keySpecialColor = Color.parseColor("#581C87"),
        keyActionColor = Color.parseColor("#A855F7"),
        keySpaceColor = Color.parseColor("#3B0764"),
        textColorPrimary = Color.parseColor("#F3E8FF"),
        textColorSecondary = Color.parseColor("#C084FC"),
        actionTextColor = Color.parseColor("#FFFFFF"),
        suggestionBgColor = Color.parseColor("#2E1065"),
        suggestionTextColor = Color.parseColor("#F3E8FF"),
        popupBgColor = Color.parseColor("#3B0764"),
        popupTextColor = Color.parseColor("#F3E8FF"),
        rippleColor = Color.parseColor("#6B21A8"),
        isDark = true
    );

    companion object {
        fun fromId(id: String?): KeyboardTheme {
            return values().firstOrNull { it.id == id } ?: AMOLED_MIDNIGHT
        }
    }
}

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
    MATERIAL_DARK(
        id = "material_dark",
        displayName = "Material Dark",
        backgroundColor = Color.parseColor("#18191C"),
        keyNormalColor = Color.parseColor("#2B2D31"),
        keySpecialColor = Color.parseColor("#232428"),
        keyActionColor = Color.parseColor("#4F46E5"),
        keySpaceColor = Color.parseColor("#2B2D31"),
        textColorPrimary = Color.parseColor("#FFFFFF"),
        textColorSecondary = Color.parseColor("#9CA3AF"),
        actionTextColor = Color.parseColor("#FFFFFF"),
        suggestionBgColor = Color.parseColor("#1E1F22"),
        suggestionTextColor = Color.parseColor("#E5E7EB"),
        popupBgColor = Color.parseColor("#313338"),
        popupTextColor = Color.parseColor("#FFFFFF"),
        rippleColor = Color.parseColor("#383A40"),
        isDark = true
    ),
    MATERIAL_LIGHT(
        id = "material_light",
        displayName = "Material Light",
        backgroundColor = Color.parseColor("#ECEFF1"),
        keyNormalColor = Color.parseColor("#FFFFFF"),
        keySpecialColor = Color.parseColor("#CFD8DC"),
        keyActionColor = Color.parseColor("#1976D2"),
        keySpaceColor = Color.parseColor("#FFFFFF"),
        textColorPrimary = Color.parseColor("#263238"),
        textColorSecondary = Color.parseColor("#78909C"),
        actionTextColor = Color.parseColor("#FFFFFF"),
        suggestionBgColor = Color.parseColor("#E0E0E0"),
        suggestionTextColor = Color.parseColor("#37474F"),
        popupBgColor = Color.parseColor("#FFFFFF"),
        popupTextColor = Color.parseColor("#263238"),
        rippleColor = Color.parseColor("#B0BEC5"),
        isDark = false
    ),
    AMOLED_MIDNIGHT(
        id = "amoled_midnight",
        displayName = "AMOLED Black",
        backgroundColor = Color.parseColor("#000000"),
        keyNormalColor = Color.parseColor("#161616"),
        keySpecialColor = Color.parseColor("#0D0D0D"),
        keyActionColor = Color.parseColor("#00B0FF"),
        keySpaceColor = Color.parseColor("#161616"),
        textColorPrimary = Color.parseColor("#FFFFFF"),
        textColorSecondary = Color.parseColor("#808080"),
        actionTextColor = Color.parseColor("#000000"),
        suggestionBgColor = Color.parseColor("#0A0A0A"),
        suggestionTextColor = Color.parseColor("#00B0FF"),
        popupBgColor = Color.parseColor("#222222"),
        popupTextColor = Color.parseColor("#00B0FF"),
        rippleColor = Color.parseColor("#333333"),
        isDark = true
    ),
    SUNSET_VIOLET(
        id = "sunset_violet",
        displayName = "Sunset Violet",
        backgroundColor = Color.parseColor("#1A102F"),
        keyNormalColor = Color.parseColor("#2D1B4E"),
        keySpecialColor = Color.parseColor("#21133B"),
        keyActionColor = Color.parseColor("#9333EA"),
        keySpaceColor = Color.parseColor("#2D1B4E"),
        textColorPrimary = Color.parseColor("#F3E8FF"),
        textColorSecondary = Color.parseColor("#C084FC"),
        actionTextColor = Color.parseColor("#FFFFFF"),
        suggestionBgColor = Color.parseColor("#24143D"),
        suggestionTextColor = Color.parseColor("#F3E8FF"),
        popupBgColor = Color.parseColor("#3B2264"),
        popupTextColor = Color.parseColor("#F3E8FF"),
        rippleColor = Color.parseColor("#4C2882"),
        isDark = true
    ),
    EMERALD_GREEN(
        id = "emerald_green",
        displayName = "Forest Emerald",
        backgroundColor = Color.parseColor("#0A1C16"),
        keyNormalColor = Color.parseColor("#133328"),
        keySpecialColor = Color.parseColor("#0E261E"),
        keyActionColor = Color.parseColor("#059669"),
        keySpaceColor = Color.parseColor("#133328"),
        textColorPrimary = Color.parseColor("#ECFDF5"),
        textColorSecondary = Color.parseColor("#6EE7B7"),
        actionTextColor = Color.parseColor("#FFFFFF"),
        suggestionBgColor = Color.parseColor("#0F2820"),
        suggestionTextColor = Color.parseColor("#ECFDF5"),
        popupBgColor = Color.parseColor("#1A4738"),
        popupTextColor = Color.parseColor("#ECFDF5"),
        rippleColor = Color.parseColor("#225C49"),
        isDark = true
    ),
    CYBER_NEON(
        id = "cyber_neon",
        displayName = "Cyber Neon",
        backgroundColor = Color.parseColor("#121820"),
        keyNormalColor = Color.parseColor("#1E2736"),
        keySpecialColor = Color.parseColor("#161E2B"),
        keyActionColor = Color.parseColor("#F59E0B"),
        keySpaceColor = Color.parseColor("#1E2736"),
        textColorPrimary = Color.parseColor("#F8FAFC"),
        textColorSecondary = Color.parseColor("#FBBF24"),
        actionTextColor = Color.parseColor("#0F172A"),
        suggestionBgColor = Color.parseColor("#18202E"),
        suggestionTextColor = Color.parseColor("#FBBF24"),
        popupBgColor = Color.parseColor("#263447"),
        popupTextColor = Color.parseColor("#F8FAFC"),
        rippleColor = Color.parseColor("#33445C"),
        isDark = true
    );

    companion object {
        fun fromId(id: String?): KeyboardTheme {
            return values().firstOrNull { it.id == id } ?: MATERIAL_DARK
        }
    }
}

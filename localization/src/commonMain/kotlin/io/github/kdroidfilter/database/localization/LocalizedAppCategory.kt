package io.github.kdroidfilter.database.localization

import io.github.kdroidfilter.database.core.AppCategory

/**
 * Extension class for AppCategory that provides localized names for categories
 * in English, Hebrew, and French, with English as the default.
 */
object LocalizedAppCategory {
    /**
     * Get the localized name of an AppCategory based on the specified language.
     * 
     * @param category The AppCategory to get the localized name for
     * @param language The language to get the name in (en, he, fr)
     * @return The localized name of the category
     */
    fun getLocalizedName(category: AppCategory, language: String): String {
        return when (language.lowercase()) {
            "he", "iw" -> getHebrewName(category)
            "fr" -> getFrenchName(category)
            else -> getEnglishName(category) // Default to English
        }
    }

    /**
     * Get the English name of an AppCategory.
     * 
     * @param category The AppCategory to get the English name for
     * @return The English name of the category
     */
    fun getEnglishName(category: AppCategory): String {
        return when (category) {
            AppCategory.TORAH -> "Torah"
            AppCategory.COMMUNICATION -> "Communication"
            AppCategory.PRODUCTIVITY -> "Productivity"
            AppCategory.TOOLS -> "Tools"
            AppCategory.NAVIGATION -> "Navigation"
            AppCategory.SHOPPING -> "Shopping"
            AppCategory.FINANCE -> "Finance"
            AppCategory.NEWS -> "News"
            AppCategory.EDUCATION -> "Education"
            AppCategory.HEALTH_FITNESS -> "Health & Fitness"
            AppCategory.MUSIC_AUDIO -> "Music & Audio"
            AppCategory.VIDEO -> "Video"
            AppCategory.PHOTOGRAPHY -> "Photography"
            AppCategory.ENTERTAINMENT -> "Entertainment"
            AppCategory.HOME -> "Home"
            AppCategory.LIFESTYLE -> "Lifestyle"
            AppCategory.TRAVEL -> "Travel"
            AppCategory.BUSINESS -> "Business"
            AppCategory.CUSTOMIZATION -> "Customization"
            AppCategory.MAIL -> "Mail"
            AppCategory.SYSTEM -> "System"
            AppCategory.GOVERNMENT -> "Government"
        }
    }

    /**
     * Get the Hebrew name of an AppCategory.
     * 
     * @param category The AppCategory to get the Hebrew name for
     * @return The Hebrew name of the category
     */
    fun getHebrewName(category: AppCategory): String {
        return when (category) {
            AppCategory.TORAH -> "תורה"
            AppCategory.COMMUNICATION -> "תקשורת"
            AppCategory.PRODUCTIVITY -> "פרודוקטיביות"
            AppCategory.TOOLS -> "כלים"
            AppCategory.NAVIGATION -> "ניווט"
            AppCategory.SHOPPING -> "קניות"
            AppCategory.FINANCE -> "פיננסים"
            AppCategory.NEWS -> "חדשות"
            AppCategory.EDUCATION -> "חינוך"
            AppCategory.HEALTH_FITNESS -> "בריאות וכושר"
            AppCategory.MUSIC_AUDIO -> "מוזיקה ואודיו"
            AppCategory.VIDEO -> "וידאו"
            AppCategory.PHOTOGRAPHY -> "צילום"
            AppCategory.ENTERTAINMENT -> "בידור"
            AppCategory.HOME -> "בית"
            AppCategory.LIFESTYLE -> "סגנון חיים"
            AppCategory.TRAVEL -> "נסיעות"
            AppCategory.BUSINESS -> "עסקים"
            AppCategory.CUSTOMIZATION -> "התאמה אישית"
            AppCategory.MAIL -> "דואר"
            AppCategory.SYSTEM -> "מערכת"
            AppCategory.GOVERNMENT -> "ממשלה"
        }
    }

    /**
     * Get the French name of an AppCategory.
     * 
     * @param category The AppCategory to get the French name for
     * @return The French name of the category
     */
    fun getFrenchName(category: AppCategory): String {
        return when (category) {
            AppCategory.TORAH -> "Torah"
            AppCategory.COMMUNICATION -> "Communication"
            AppCategory.PRODUCTIVITY -> "Productivité"
            AppCategory.TOOLS -> "Outils"
            AppCategory.NAVIGATION -> "Navigation"
            AppCategory.SHOPPING -> "Shopping"
            AppCategory.FINANCE -> "Finance"
            AppCategory.NEWS -> "Actualités"
            AppCategory.EDUCATION -> "Éducation"
            AppCategory.HEALTH_FITNESS -> "Santé et Fitness"
            AppCategory.MUSIC_AUDIO -> "Musique et Audio"
            AppCategory.VIDEO -> "Vidéo"
            AppCategory.PHOTOGRAPHY -> "Photographie"
            AppCategory.ENTERTAINMENT -> "Divertissement"
            AppCategory.HOME -> "Maison"
            AppCategory.LIFESTYLE -> "Style de vie"
            AppCategory.TRAVEL -> "Voyage"
            AppCategory.BUSINESS -> "Affaires"
            AppCategory.CUSTOMIZATION -> "Personnalisation"
            AppCategory.MAIL -> "Courrier"
            AppCategory.SYSTEM -> "Système"
            AppCategory.GOVERNMENT -> "Gouvernement"
        }
    }
}
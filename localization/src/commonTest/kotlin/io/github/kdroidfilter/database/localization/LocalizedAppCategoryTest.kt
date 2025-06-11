package io.github.kdroidfilter.database.localization

import io.github.kdroidfilter.database.core.AppCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalizedAppCategoryTest {
    
    @Test
    fun testEnglishLocalization() {
        // Test English localization (default)
        assertEquals("Torah", LocalizedAppCategory.getLocalizedName(AppCategory.TORAH, "en"))
        assertEquals("Communication", LocalizedAppCategory.getLocalizedName(AppCategory.COMMUNICATION, "en"))
        assertEquals("Productivity", LocalizedAppCategory.getLocalizedName(AppCategory.PRODUCTIVITY, "en"))
        assertEquals("Health & Fitness", LocalizedAppCategory.getLocalizedName(AppCategory.HEALTH_FITNESS, "en"))
    }
    
    @Test
    fun testHebrewLocalization() {
        // Test Hebrew localization
        assertEquals("תורה", LocalizedAppCategory.getLocalizedName(AppCategory.TORAH, "he"))
        assertEquals("תקשורת", LocalizedAppCategory.getLocalizedName(AppCategory.COMMUNICATION, "he"))
        assertEquals("פרודוקטיביות", LocalizedAppCategory.getLocalizedName(AppCategory.PRODUCTIVITY, "he"))
        assertEquals("בריאות וכושר", LocalizedAppCategory.getLocalizedName(AppCategory.HEALTH_FITNESS, "he"))
    }
    
    @Test
    fun testFrenchLocalization() {
        // Test French localization
        assertEquals("Torah", LocalizedAppCategory.getLocalizedName(AppCategory.TORAH, "fr"))
        assertEquals("Communication", LocalizedAppCategory.getLocalizedName(AppCategory.COMMUNICATION, "fr"))
        assertEquals("Productivité", LocalizedAppCategory.getLocalizedName(AppCategory.PRODUCTIVITY, "fr"))
        assertEquals("Santé et Fitness", LocalizedAppCategory.getLocalizedName(AppCategory.HEALTH_FITNESS, "fr"))
    }
    
    @Test
    fun testDefaultLocalization() {
        // Test that unknown language codes default to English
        assertEquals("Torah", LocalizedAppCategory.getLocalizedName(AppCategory.TORAH, "unknown"))
        assertEquals("Communication", LocalizedAppCategory.getLocalizedName(AppCategory.COMMUNICATION, "xx"))
        assertEquals("Productivity", LocalizedAppCategory.getLocalizedName(AppCategory.PRODUCTIVITY, ""))
    }
    
    @Test
    fun testCaseInsensitivity() {
        // Test that language codes are case-insensitive
        assertEquals("תורה", LocalizedAppCategory.getLocalizedName(AppCategory.TORAH, "HE"))
        assertEquals("תורה", LocalizedAppCategory.getLocalizedName(AppCategory.TORAH, "He"))
        assertEquals("Productivité", LocalizedAppCategory.getLocalizedName(AppCategory.PRODUCTIVITY, "FR"))
        assertEquals("Productivité", LocalizedAppCategory.getLocalizedName(AppCategory.PRODUCTIVITY, "Fr"))
    }
}
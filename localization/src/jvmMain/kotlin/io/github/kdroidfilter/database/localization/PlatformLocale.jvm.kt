package io.github.kdroidfilter.database.localization

import io.github.kdroidfilter.database.core.AppCategory
import java.util.Locale

actual fun AppCategory.getLocalizedName(): String {
    return LocalizedAppCategory.getLocalizedName(this, getSystemLanguage())
}

/**
 * Get the system language code for JVM platform.
 *
 * @return The language code (e.g., "en", "fr", "he")
 */
private fun getSystemLanguage(): String {
    return Locale.getDefault().language
}
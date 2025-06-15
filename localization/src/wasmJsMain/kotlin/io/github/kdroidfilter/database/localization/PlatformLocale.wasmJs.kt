package io.github.kdroidfilter.database.localization

import kotlinx.browser.window

actual fun io.github.kdroidfilter.database.core.AppCategory.getLocalizedName(): String {
    val language = window.navigator.language
    return LocalizedAppCategory.getLocalizedName(this, language)
}
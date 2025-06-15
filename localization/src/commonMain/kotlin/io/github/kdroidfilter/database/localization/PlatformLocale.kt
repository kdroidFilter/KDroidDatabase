package io.github.kdroidfilter.database.localization

import io.github.kdroidfilter.database.core.AppCategory




/**
 * Extension function to get the localized name of an AppCategory based on the system language.
 * 
 * @return The localized name of the category
 */
expect fun AppCategory.getLocalizedName(): String
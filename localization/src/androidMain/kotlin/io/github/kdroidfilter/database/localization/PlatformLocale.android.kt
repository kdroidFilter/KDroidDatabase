package io.github.kdroidfilter.database.localization

import android.content.Context
import com.kdroid.androidcontextprovider.ContextProvider
import io.github.kdroidfilter.database.core.AppCategory

actual fun AppCategory.getLocalizedName(): String {
    val context = ContextProvider.getContext()
    val language = getDeviceLanguage(context)
    return LocalizedAppCategory.getLocalizedName(this, language)
}

/**
 * Get the device language using the appropriate method based on API level.
 *
 * @param context The Android context
 * @return The language code
 */
private fun getDeviceLanguage(context: Context): String = context.resources.configuration.locales[0].language
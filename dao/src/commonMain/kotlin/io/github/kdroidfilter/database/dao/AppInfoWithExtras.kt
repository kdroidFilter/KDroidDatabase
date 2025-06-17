package io.github.kdroidfilter.database.dao

import io.github.kdroidfilter.androidappstorekit.gplay.core.model.GooglePlayApplicationInfo

/**
 * Extended class to add additional information to the GooglePlayApplicationInfo model
 */
data class AppInfoWithExtras(
    val id: Long,
    val categoryLocalizedName: String,
    val app: GooglePlayApplicationInfo
)

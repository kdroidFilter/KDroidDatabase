package io.github.kdroidfilter.database.dao

import com.kdroid.gplayscrapper.core.model.GooglePlayApplicationInfo

/**
 * Extended class to add additional information to the GooglePlayApplicationInfo model
 */
data class AppInfoWithExtras(
    val id: Long,
    val categoryLocalizedName: String,
    val app: GooglePlayApplicationInfo
)

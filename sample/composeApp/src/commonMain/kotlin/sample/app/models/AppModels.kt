package sample.app.models

import com.kdroid.gplayscrapper.core.model.GooglePlayApplicationInfo

// Navigation destinations
enum class Screen {
    Home,
    Search
}

// Extended class to add additional information to the GooglePlayApplicationInfo model
data class AppInfoWithExtras(
    val id: Long,
    val categoryLocalizedName: String,
    val app: GooglePlayApplicationInfo
)

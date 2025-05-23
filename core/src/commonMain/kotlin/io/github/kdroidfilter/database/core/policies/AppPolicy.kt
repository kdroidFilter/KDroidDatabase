package io.github.kdroidfilter.database.core.policies

import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.core.DetectionRule
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppPolicy {
    val packageName: String
    val category: AppCategory
    val minimumVersionCode: Int

    val requiresPlayStoreInstallation: Boolean
    val hasUnmodestImage : Boolean
    val isPotentiallyDangerous : Boolean

    val detectionRules: List<DetectionRule>
}



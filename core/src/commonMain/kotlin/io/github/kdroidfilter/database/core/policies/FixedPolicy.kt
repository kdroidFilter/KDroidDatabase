package io.github.kdroidfilter.database.core.policies

import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.core.DetectionRule
import io.github.kdroidfilter.database.core.NetworkPolicy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Simple case: same rule in all modes
@Serializable
@SerialName("Fixed")
data class FixedPolicy(
    override val packageName: String,
    override val category: AppCategory,
    val networkPolicy: NetworkPolicy,
    override val minimumVersionCode: Int,
    override val requiresPlayStoreInstallation: Boolean = false,
    override val hasUnmodestImage: Boolean = false,
    override val isPotentiallyDangerous: Boolean = false,
    override val isRecommendedInStore: Boolean = false,
    override val sha1: String = "",
    override val detectionRules: List<DetectionRule> = emptyList()
) : AppPolicy

package io.github.kdroidfilter.database.core.policies

import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.core.DetectionRule
import io.github.kdroidfilter.database.core.NetworkPolicy
import io.github.kdroidfilter.database.core.UserMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Free identifier for the variant; used as a display key in the UI. */
@Serializable
data class PolicyVariant(
    val id: String,            // e.g. "strict", "balanced", "relaxed"
    val label: String,         // e.g. "Strict (no external hosts)"
    val policy: NetworkPolicy,  // associated filtering rule
    val detectionRules: List<DetectionRule> = emptyList(),
    val overrideDefaultRules: Boolean = true,
    val configurationRequired: Boolean = false,
    val configurationKey: String? = null
)

/** Group of variants available for a given UserMode. */
@Serializable
data class ModeVariants(
    val userMode: UserMode,
    val variants: List<PolicyVariant>,
    val defaultVariantId: String = variants.first().id
)

@Serializable
data class ResolvedPolicy(
    val networkPolicy: NetworkPolicy,
    val detectionRules: List<DetectionRule>
)

/** Complete policy: several user modes, each with multiple variants. */
@Serializable @SerialName("MultiMode")
data class MultiModePolicy(
    override val packageName: String,
    override val category: AppCategory,
    val modeVariants: List<ModeVariants>,
    override val minimumVersionCode: Int,
    override val requiresPlayStoreInstallation: Boolean = false,
    override val hasUnmodestImage: Boolean = false,
    override val isPotentiallyDangerous: Boolean = false,
    override val isRecommendedInStore: Boolean = false,
    override val detectionRules: List<DetectionRule> = emptyList()
) : AppPolicy {

    /** Returns the effective NetworkPolicy for the specified mode and variant. */
    fun resolveDetailed(mode: UserMode, chosenId: String? = null): ResolvedPolicy {
        val mv = modeVariants.first { it.userMode == mode }
        val id = chosenId ?: mv.defaultVariantId
        val variant = mv.variants.first { it.id == id }
        // merges global rules + variant-specific rules
        val rules = if (variant.overrideDefaultRules) {
            variant.detectionRules
        } else {
            this.detectionRules + variant.detectionRules
        }
        return ResolvedPolicy(variant.policy, rules)
    }
}

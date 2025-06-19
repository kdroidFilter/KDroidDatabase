import co.touchlab.kermit.Logger
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.core.policies.AppPolicy
import io.github.kdroidfilter.storekit.aptoide.api.extensions.toFormattedSha1
import io.github.kdroidfilter.storekit.aptoide.api.services.AptoideService
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * Utility to check policies for missing version codes and signatures
 */
object PolicyChecker {
    private val logger = Logger.withTag("PolicyChecker")

    /**
     * Checks all policies for missing version codes and signatures
     * @param policiesDir The directory containing policy files
     * @return true if all checks pass, false otherwise
     */
    fun checkPolicies(policiesDir: Path): Boolean {
        logger.i { "🔍 Checking policies in: $policiesDir" }

        val aptoideService = AptoideService()
        val policies = PolicyRepository.loadAll(policiesDir)

        val versionCodeIssues = mutableListOf<String>()
        val signatureIssues = mutableListOf<String>()

        policies.forEach { policy ->
            runBlocking {
                // Check version code
                if (policy.minimumVersionCode == 0) {
                    try {
                        val appMeta = aptoideService.getAppMetaByPackageName(policy.packageName)
                        logger.i { "✅ Successfully retrieved version code for ${policy.packageName}" }
                    } catch (e: Exception) {
                        // If it's not a system app, add to issues list
                        if (policy.category != AppCategory.SYSTEM) {
                            logger.w { "❌ Failed to retrieve version code for ${policy.packageName}: ${e.message}" }
                            versionCodeIssues.add(policy.packageName)
                        } else {
                            logger.i { "⚠️ System app ${policy.packageName} has version code 0 but is exempt from check" }
                        }
                    }
                }

                // Check signature
                if (policy.sha1.isEmpty()) {
                    try {
                        val appMeta = aptoideService.getAppMetaByPackageName(policy.packageName)
                        val signature = appMeta.file.signature.toFormattedSha1()
                        logger.i { "✅ Successfully retrieved signature for ${policy.packageName}" }
                    } catch (e: Exception) {
                        // If it's not a system app, add to issues list
                        if (policy.category != AppCategory.SYSTEM) {
                            logger.w { "❌ Failed to retrieve signature for ${policy.packageName}: ${e.message}" }
                            signatureIssues.add(policy.packageName)
                        } else {
                            logger.i { "⚠️ System app ${policy.packageName} has empty signature but is exempt from check" }
                        }
                    }
                }
            }
        }

        // Print summary
        logger.i { "📊 Check complete: ${policies.size} policies checked" }

        val hasIssues = versionCodeIssues.isNotEmpty() || signatureIssues.isNotEmpty()

        if (versionCodeIssues.isNotEmpty()) {
            logger.e { "❌ Found ${versionCodeIssues.size} policies with version code 0 that cannot be retrieved from API:" }
            versionCodeIssues.forEach { packageName ->
                logger.e { "  - $packageName" }
            }
        }

        if (signatureIssues.isNotEmpty()) {
            logger.e { "❌ Found ${signatureIssues.size} policies with empty signature that cannot be retrieved from API:" }
            signatureIssues.forEach { packageName ->
                logger.e { "  - $packageName" }
            }
        }

        return !hasIssues
    }
}

/**
 * Main function to run the policy checker
 */
fun main() {
    val logger = Logger.withTag("PolicyChecker")
    val projectDir = java.nio.file.Paths.get("").toAbsolutePath()
    val root = projectDir.parent.resolve("../app-policies")

    val isValid = PolicyChecker.checkPolicies(root)

    if (!isValid) {
        logger.e { "❌ Policy checks failed. Please fix the issues above." }
        exitProcess(1)
    } else {
        logger.i { "✅ All policy checks passed!" }
    }
}

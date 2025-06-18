import co.touchlab.kermit.Logger
import io.github.kdroidfilter.database.core.policies.AppPolicy
import io.github.kdroidfilter.database.core.policies.FixedPolicy
import io.github.kdroidfilter.database.core.policies.ModeBasedPolicy
import io.github.kdroidfilter.database.core.policies.MultiModePolicy
import io.github.kdroidfilter.storekit.aptoide.api.extensions.toFormattedSha1
import io.github.kdroidfilter.storekit.aptoide.api.services.AptoideService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SqlitePolicyBuilder {
    private val logger = Logger.withTag("SqlitePolicyBuilder")
    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = false
        serializersModule = PolicyRepository.json.serializersModule
    }

    fun buildDatabase(policiesDir: Path, outputDbPath: Path) {
        // Always create the database from scratch
        logger.i { "🔄 Creating database from scratch..." }

        // Delete existing database file if it exists
        if (Files.exists(outputDbPath)) {
            logger.i { "🗑️ Deleting existing database file..." }
            Files.delete(outputDbPath)
        }

        // Get release name from environment variable or generate timestamp
        val releaseName = System.getenv("RELEASE_NAME") ?: LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
        logger.i { "🏷️ Using release name: $releaseName" }

        Class.forName("org.sqlite.JDBC")
        Files.createDirectories(outputDbPath.parent)
        val url = "jdbc:sqlite:${outputDbPath.toAbsolutePath()}"
        DriverManager.getConnection(url).use { conn ->
            conn.autoCommit = false
            createTables(conn)
            insertVersion(conn, releaseName)
            insertPolicies(conn, policiesDir)
            conn.commit()
        }
        logger.i { "✅ SQLite database created at $outputDbPath" }
    }

    private fun createTables(conn: Connection) = with(conn.createStatement()) {
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS policies (
              package_name TEXT PRIMARY KEY,
              data         TEXT NOT NULL
            )
        """.trimIndent())

        executeUpdate("""
            CREATE TABLE IF NOT EXISTS version (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              release_name TEXT NOT NULL
            )
        """.trimIndent())
        close()
    }

    private fun insertVersion(conn: Connection, releaseName: String) {
        // Clear existing entries
        conn.createStatement().use { stmt ->
            stmt.executeUpdate("DELETE FROM version")
        }

        // Insert new release name
        conn.prepareStatement("INSERT INTO version (release_name) VALUES (?)").use { ps ->
            ps.setString(1, releaseName)
            ps.executeUpdate()
        }
        logger.i { "✅ Inserted version info: $releaseName" }
    }

    private fun insertPolicies(conn: Connection, policiesDir: Path) {
        val insertSql = """
            INSERT OR REPLACE INTO policies(package_name, data) 
            VALUES(?, ?)
        """.trimIndent()

        val aptoideService = AptoideService()
        val failedApps = mutableListOf<String>()
        conn.prepareStatement(insertSql).use { ps ->
            val policies = PolicyRepository.loadAll(policiesDir)
            policies.forEach { policy ->
                runBlocking {
                    // Try to get the app signature, but use an empty string if it fails
                    val appSignature = try {
                        aptoideService.getAppMetaByPackageName(policy.packageName)
                            .file.signature.toFormattedSha1()
                    } catch (e: Exception) {
                        logger.w { "Failed to get signature for ${policy.packageName}: ${e.message}" }
                        failedApps.add(policy.packageName)
                        ""
                    }
                    // Create a new policy with the updated sha1 value
                    val policyWithSignature = when (policy) {
                        is FixedPolicy -> FixedPolicy(
                            packageName = policy.packageName,
                            category = policy.category,
                            networkPolicy = policy.networkPolicy,
                            minimumVersionCode = policy.minimumVersionCode,
                            requiresPlayStoreInstallation = policy.requiresPlayStoreInstallation,
                            hasUnmodestImage = policy.hasUnmodestImage,
                            isPotentiallyDangerous = policy.isPotentiallyDangerous,
                            isRecommendedInStore = policy.isRecommendedInStore,
                            sha1 = appSignature,
                            detectionRules = policy.detectionRules
                        )
                        is ModeBasedPolicy -> ModeBasedPolicy(
                            packageName = policy.packageName,
                            category = policy.category,
                            modePolicies = policy.modePolicies,
                            minimumVersionCode = policy.minimumVersionCode,
                            requiresPlayStoreInstallation = policy.requiresPlayStoreInstallation,
                            hasUnmodestImage = policy.hasUnmodestImage,
                            isPotentiallyDangerous = policy.isPotentiallyDangerous,
                            isRecommendedInStore = policy.isRecommendedInStore,
                            sha1 = appSignature,
                            detectionRules = policy.detectionRules
                        )
                        is MultiModePolicy -> MultiModePolicy(
                            packageName = policy.packageName,
                            category = policy.category,
                            modeVariants = policy.modeVariants,
                            minimumVersionCode = policy.minimumVersionCode,
                            requiresPlayStoreInstallation = policy.requiresPlayStoreInstallation,
                            hasUnmodestImage = policy.hasUnmodestImage,
                            isPotentiallyDangerous = policy.isPotentiallyDangerous,
                            isRecommendedInStore = policy.isRecommendedInStore,
                            sha1 = appSignature,
                            detectionRules = policy.detectionRules
                        )
                        else -> throw IllegalArgumentException("Unknown policy type: ${policy::class.simpleName}")
                    }
                    val jsonStr = json.encodeToString(AppPolicy.serializer(), policyWithSignature)
                    ps.setString(1, policy.packageName)
                    ps.setString(2, jsonStr)
                    ps.addBatch()
                }
            }
            ps.executeBatch()
            logger.i { "✅ Inserted ${policies.size} policies" }

            // Log summary of apps with failed signature retrieval
            if (failedApps.isNotEmpty()) {
                logger.w { "⚠️ Failed to get signatures for ${failedApps.size} apps:" }
                failedApps.forEach { packageName ->
                    logger.w { "  - $packageName" }
                }
            }
        }
    }
}

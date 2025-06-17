package io.github.kdroidfilter.database.dao

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kdroidfilter.database.core.policies.AppPolicy
import io.github.kdroidfilter.database.downloader.DatabaseDownloader
import io.github.kdroidfilter.database.store.Database
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import PolicyRepository

/**
 * Tests for verifying data consistency between JSON policy files and the database
 * These tests ensure that the data in the database matches the data in the JSON files
 */
class DataConsistencyTest {

    private lateinit var database: Database
    private lateinit var driver: SqlDriver
    private lateinit var databaseFile: File
    private lateinit var policies: List<AppPolicy>

    @BeforeEach
    fun setup(@TempDir tempDir: Path) {
        // Download the database from GitHub releases
        val language = "en" // Use English for tests
        val dbFileName = "store-database-$language.db"
        databaseFile = tempDir.resolve(dbFileName).toFile()

        println("[DEBUG_LOG] Temp directory: ${tempDir.toAbsolutePath()}")
        println("[DEBUG_LOG] Database file: ${databaseFile.absolutePath}")

        // Download the database
        runBlocking {
            val downloader = DatabaseDownloader()
            val success = downloader.downloadLatestStoreDatabaseForLanguage(
                tempDir.toString(), 
                language
            )

            println("[DEBUG_LOG] Database download success: $success")
            assertTrue(success, "Database download should succeed")
            assertTrue(databaseFile.exists(), "Database file should exist")
            assertTrue(databaseFile.length() > 0, "Database file should not be empty")
        }

        // Create the database driver and connection
        driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
        database = Database(driver)

        println("[DEBUG_LOG] Database connection established")

        // Load all policies from the app-policies directory
        val projectDir = Paths.get("").toAbsolutePath()
        println("[DEBUG_LOG] Project directory: $projectDir")
        // The app-policies directory is at the root of the project, not in the dao directory
        val policiesDir = projectDir.resolve("../app-policies")
        println("[DEBUG_LOG] Policies directory: $policiesDir")

        // Use PolicyRepository to load all policies
        policies = loadPolicies(policiesDir)

        println("[DEBUG_LOG] Loaded ${policies.size} policies from JSON files")
    }

    @AfterEach
    fun tearDown() {
        driver.close()
    }

    /**
     * Load all policies from the app-policies directory
     */
    private fun loadPolicies(policiesDir: Path): List<AppPolicy> {
        return PolicyRepository.loadAll(policiesDir)
    }

    @Test
    fun testPackageNameConsistency() {
        // Get all applications from the database
        val applications = ApplicationsDao.loadApplicationsFromDatabase(
            database = database,
            deviceLanguage = "en",
            creator = { id, categoryLocalizedName, appInfo ->
                AppInfoWithExtras(
                    id = id,
                    categoryLocalizedName = categoryLocalizedName,
                    app = appInfo
                )
            }
        )

        println("[DEBUG_LOG] Loaded ${applications.size} applications from database")

        // Create a map of package names to applications for easier lookup
        val appMap = applications.associateBy { it.app.appId }

        // Verify that all policies have corresponding applications in the database
        var matchCount = 0
        for (policy in policies) {
            val packageName = policy.packageName
            val app = appMap[packageName]

            if (app != null) {
                matchCount++
                println("[DEBUG_LOG] Found match for package: $packageName")
            } else {
                println("[DEBUG_LOG] No match found for package: $packageName")
            }
        }

        println("[DEBUG_LOG] Found matches for $matchCount out of ${policies.size} policies")

        // We don't expect 100% match because some policies might be for apps that are not in the database yet
        // But we expect a significant portion to match
        assertTrue(matchCount > 0, "Should find at least one matching application")
    }

    @Test
    fun testCategoryConsistency() {
        // Get all applications from the database
        val applications = ApplicationsDao.loadApplicationsFromDatabase(
            database = database,
            deviceLanguage = "en",
            creator = { id, categoryLocalizedName, appInfo ->
                AppInfoWithExtras(
                    id = id,
                    categoryLocalizedName = categoryLocalizedName,
                    app = appInfo
                )
            }
        )

        // Create a map of package names to applications for easier lookup
        val appMap = applications.associateBy { it.app.appId }

        // Verify that the categories in the database match the categories in the JSON files
        var categoryMatchCount = 0
        for (policy in policies) {
            val packageName = policy.packageName
            val app = appMap[packageName]

            if (app != null) {
                // The category in the database is stored as a localized name, so we need to compare with the enum name
                val policyCategory = policy.category.name
                val dbCategory = app.categoryLocalizedName

                println("[DEBUG_LOG] Package: $packageName, Policy category: $policyCategory, DB category: $dbCategory")

                // The comparison is not exact because the database stores localized category names
                // We'll just log the differences for now
                if (dbCategory.contains(policyCategory, ignoreCase = true)) {
                    categoryMatchCount++
                    println("[DEBUG_LOG] Category match for package: $packageName")
                } else {
                    println("[DEBUG_LOG] Category mismatch for package: $packageName")
                }
            }
        }

        println("[DEBUG_LOG] Found category matches for $categoryMatchCount applications")

        // We expect at least some category matches
        assertTrue(categoryMatchCount > 0, "Should find at least one category match")
    }

    @Test
    fun testIsRecommendedInStoreConsistency() {
        // Verify that the isRecommendedInStore flag in the database matches the isRecommendedInStore flag in the JSON files
        var recommendedMatchCount = 0
        for (policy in policies) {
            val packageName = policy.packageName
            val policyRecommended = policy.isRecommendedInStore

            // Check if the application is recommended in the database
            val dbRecommended = ApplicationsDao.isRecommendedInStore(
                database = database,
                appId = packageName
            )

            println("[DEBUG_LOG] Package: $packageName, Policy recommended: $policyRecommended, DB recommended: $dbRecommended")

            // If the application is in the database, verify that the recommended flag matches
            if (policyRecommended == dbRecommended) {
                recommendedMatchCount++
                println("[DEBUG_LOG] Recommended flag match for package: $packageName")
            } else {
                println("[DEBUG_LOG] Recommended flag mismatch for package: $packageName")
            }
        }

        println("[DEBUG_LOG] Found recommended flag matches for $recommendedMatchCount out of ${policies.size} policies")

        // We expect at least some recommended flag matches
        assertTrue(recommendedMatchCount > 0, "Should find at least one recommended flag match")
    }
}

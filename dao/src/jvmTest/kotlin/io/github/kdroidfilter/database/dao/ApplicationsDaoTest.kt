package io.github.kdroidfilter.database.dao

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kdroidfilter.database.downloader.DatabaseDownloader
import io.github.kdroidfilter.database.store.Database
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the ApplicationsDao
 * These tests download the database from GitHub releases and use it to test the DAO functionality
 */
class ApplicationsDaoTest {

    private lateinit var database: Database
    private lateinit var driver: SqlDriver
    private lateinit var databaseFile: File

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
    }

    @AfterEach
    fun tearDown() {
        driver.close()
    }

    @Test
    fun testLoadApplicationsFromDatabase() {
        // Test loading all applications
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

        println("[DEBUG_LOG] Loaded ${applications.size} applications")
        assertTrue(applications.isNotEmpty(), "Should load at least one application")

        // Verify the first application has valid data
        val firstApp = applications.firstOrNull()
        assertNotNull(firstApp, "First application should not be null")
        assertNotNull(firstApp.app.appId, "App ID should not be null")
        assertNotNull(firstApp.app.title, "App title should not be null")

        println("[DEBUG_LOG] First app: ${firstApp.app.appId} - ${firstApp.app.title}")
    }

    @Test
    fun testSearchApplicationsInDatabase() {
        // First get all applications to find a valid search term
        val allApps = ApplicationsDao.loadApplicationsFromDatabase(
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

        // Make sure we have applications to search
        assertTrue(allApps.isNotEmpty(), "Should have applications to search")

        // Use the first app's title as a search term
        val searchTerm = allApps.first().app.title.split(" ").first()
        println("[DEBUG_LOG] Searching for: $searchTerm")

        // Search for applications
        val searchResults = ApplicationsDao.searchApplicationsInDatabase(
            database = database,
            query = searchTerm,
            deviceLanguage = "en",
            creator = { id, categoryLocalizedName, appInfo ->
                AppInfoWithExtras(
                    id = id,
                    categoryLocalizedName = categoryLocalizedName,
                    app = appInfo
                )
            }
        )

        println("[DEBUG_LOG] Found ${searchResults.size} results for '$searchTerm'")
        assertTrue(searchResults.isNotEmpty(), "Should find at least one result")
    }

    @Test
    fun testIsRecommendedInStore() {
        // First get all applications
        val allApps = ApplicationsDao.loadApplicationsFromDatabase(
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

        // Make sure we have applications to test
        assertTrue(allApps.isNotEmpty(), "Should have applications to test")

        // Get the first app's ID
        val appId = allApps.first().app.appId
        println("[DEBUG_LOG] Testing isRecommendedInStore for: $appId")

        // Check if the app is recommended
        val isRecommended = ApplicationsDao.isRecommendedInStore(
            database = database,
            appId = appId
        )

        println("[DEBUG_LOG] App $appId is recommended: $isRecommended")
        // We don't assert true or false here because we don't know if the app is recommended
        // Just verify the function runs without errors
    }

    @Test
    fun testGetRecommendedApplications() {
        // Get recommended applications
        val recommendedApps = ApplicationsDao.getRecommendedApplications(
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

        println("[DEBUG_LOG] Found ${recommendedApps.size} recommended applications")

        // Verify each app in the list is actually recommended
        recommendedApps.forEach { app ->
            val isRecommended = ApplicationsDao.isRecommendedInStore(
                database = database,
                appId = app.app.appId
            )
            println("[DEBUG_LOG] Verifying app ${app.app.appId} is recommended: $isRecommended")
            assertTrue(isRecommended, "App ${app.app.appId} should be recommended")
        }
    }
}

package io.github.kdroidfilter.database.dao

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.downloader.DatabaseDownloader
import io.github.kdroidfilter.database.store.Database
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the CategoriesDao
 * These tests download the database from GitHub releases and use it to test the DAO functionality
 */
class CategoriesDaoTest {

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
    fun testGetAllCategories() {
        // Test getting all categories
        val categories = CategoriesDao.getAllCategories(
            database = database,
            deviceLanguage = "en"
        )

        println("[DEBUG_LOG] Loaded ${categories.size} categories")
        assertTrue(categories.isNotEmpty(), "Should load at least one category")

        // Verify the categories contain valid data
        categories.forEach { (category, localizedName) ->
            println("[DEBUG_LOG] Category: ${category.name} - $localizedName")
            assertNotNull(category, "Category should not be null")
            assertNotNull(localizedName, "Localized name should not be null")
        }
    }

    @Test
    fun testGetApplicationsByCategoryId() {
        // First get all categories to find a valid category ID
        val categories = database.app_categoriesQueries.getAllCategories().executeAsList()
        assertTrue(categories.isNotEmpty(), "Should have categories to test")

        // Get the first category's ID
        val categoryId = categories.first().id
        println("[DEBUG_LOG] Testing getApplicationsByCategoryId for category ID: $categoryId")

        // Get applications by category ID
        val applications = CategoriesDao.getApplicationsByCategoryId(
            database = database,
            categoryId = categoryId,
            deviceLanguage = "en",
            creator = { id, categoryLocalizedName, appInfo ->
                AppInfoWithExtras(
                    id = id,
                    categoryLocalizedName = categoryLocalizedName,
                    app = appInfo
                )
            }
        )

        println("[DEBUG_LOG] Found ${applications.size} applications for category ID $categoryId")
        
        // Verify all applications have the correct category ID
        applications.forEach { app ->
            val appDetails = database.applicationsQueries.getApplicationById(app.id).executeAsOne()
            assertEquals(categoryId, appDetails.app_category_id, "Application should have the correct category ID")
        }
    }

    @Test
    fun testGetApplicationsByCategoryName() {
        // First get all categories to find a valid category name
        val categories = database.app_categoriesQueries.getAllCategories().executeAsList()
        assertTrue(categories.isNotEmpty(), "Should have categories to test")

        // Get the first category's name
        val categoryName = categories.first().category_name
        println("[DEBUG_LOG] Testing getApplicationsByCategoryName for category name: $categoryName")

        // Get applications by category name
        val applications = CategoriesDao.getApplicationsByCategoryName(
            database = database,
            categoryName = categoryName,
            deviceLanguage = "en",
            creator = { id, categoryLocalizedName, appInfo ->
                AppInfoWithExtras(
                    id = id,
                    categoryLocalizedName = categoryLocalizedName,
                    app = appInfo
                )
            }
        )

        println("[DEBUG_LOG] Found ${applications.size} applications for category name $categoryName")
        
        // Verify all applications have the correct category name
        applications.forEach { app ->
            val appDetails = database.applicationsQueries.getApplicationById(app.id).executeAsOne()
            val appCategory = database.app_categoriesQueries.getCategoryById(appDetails.app_category_id).executeAsOne()
            assertEquals(categoryName, appCategory.category_name, "Application should have the correct category name")
        }
    }

    @Test
    fun testGetApplicationsByCategory() {
        // First get all categories to find a valid category
        val dbCategories = database.app_categoriesQueries.getAllCategories().executeAsList()
        assertTrue(dbCategories.isNotEmpty(), "Should have categories to test")

        // Get the first category's name and convert to enum
        val categoryName = dbCategories.first().category_name
        val category = AppCategory.valueOf(categoryName)
        println("[DEBUG_LOG] Testing getApplicationsByCategory for category: $category")

        // Get applications by category enum
        val applications = CategoriesDao.getApplicationsByCategory(
            database = database,
            category = category,
            deviceLanguage = "en",
            creator = { id, categoryLocalizedName, appInfo ->
                AppInfoWithExtras(
                    id = id,
                    categoryLocalizedName = categoryLocalizedName,
                    app = appInfo
                )
            }
        )

        println("[DEBUG_LOG] Found ${applications.size} applications for category $category")
        
        // Verify all applications have the correct category
        applications.forEach { app ->
            val appDetails = database.applicationsQueries.getApplicationById(app.id).executeAsOne()
            val appCategory = database.app_categoriesQueries.getCategoryById(appDetails.app_category_id).executeAsOne()
            assertEquals(category.name, appCategory.category_name, "Application should have the correct category")
        }
    }
}
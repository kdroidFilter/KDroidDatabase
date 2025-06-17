import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.core.policies.FixedPolicy
import io.github.kdroidfilter.database.core.NetworkPolicy
import io.github.kdroidfilter.database.core.NetworkMode
import io.github.kdroidfilter.database.core.ModeSpec
import kotlinx.serialization.json.Json
import java.nio.file.StandardOpenOption

class SqliteStoreBuilderTest {

    @Test
    fun `buildDatabase should create a valid database file`() {
        // Arrange
        val tempDir = Files.createTempDirectory("test-policies")
        val outputDbPath = Files.createTempFile("test-db", ".db")
        Files.deleteIfExists(outputDbPath) // Delete so buildDatabase can create it

        // Create a test policy file
        createTestPolicyFile(tempDir)

        // Act
        SqliteStoreBuilder.buildDatabase(tempDir, outputDbPath)

        // Assert
        assertTrue(Files.exists(outputDbPath), "Database file should be created")

        // Verify database has expected tables
        val connection = DriverManager.getConnection("jdbc:sqlite:${outputDbPath.toAbsolutePath()}")
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('app_categories', 'version', 'applications', 'developers')"
        )

        val tables = mutableListOf<String>()
        while (resultSet.next()) {
            tables.add(resultSet.getString("name"))
        }

        assertTrue(tables.contains("app_categories"), "app_categories table should exist")
        assertTrue(tables.contains("version"), "version table should exist")
        assertTrue(tables.contains("applications"), "applications table should exist")
        assertTrue(tables.contains("developers"), "developers table should exist")

        // Cleanup
        resultSet.close()
        statement.close()
        connection.close()
        Files.deleteIfExists(outputDbPath)
        deleteDirectory(tempDir)
    }

    @Test
    fun `insertCategories should add all AppCategory entries to database`() {
        // Arrange
        val outputDbPath = Files.createTempFile("test-db", ".db")
        val driver = JdbcSqliteDriver("jdbc:sqlite:${outputDbPath.toAbsolutePath()}")
        io.github.kdroidfilter.database.store.Database.Schema.create(driver)

        // Use reflection to access the private method
        val method = SqliteStoreBuilder::class.java.getDeclaredMethod("insertCategories", Path::class.java)
        method.isAccessible = true

        // Act
        method.invoke(SqliteStoreBuilder, outputDbPath)

        // Assert
        val connection = DriverManager.getConnection("jdbc:sqlite:${outputDbPath.toAbsolutePath()}")
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery("SELECT COUNT(*) as count FROM app_categories")

        assertTrue(resultSet.next(), "Result set should have at least one row")
        val count = resultSet.getInt("count")
        assertEquals(AppCategory.entries.size, count, "All categories should be inserted")

        // Cleanup
        resultSet.close()
        statement.close()
        connection.close()
        driver.close()
        Files.deleteIfExists(outputDbPath)
    }

    @Test
    fun `insertVersion should add version information to database`() {
        // Arrange
        val outputDbPath = Files.createTempFile("test-db", ".db")
        val driver = JdbcSqliteDriver("jdbc:sqlite:${outputDbPath.toAbsolutePath()}")
        io.github.kdroidfilter.database.store.Database.Schema.create(driver)

        val releaseName = "test-release-123"

        // Use reflection to access the private method
        val method = SqliteStoreBuilder::class.java.getDeclaredMethod(
            "insertVersion", 
            String::class.java,
            Path::class.java
        )
        method.isAccessible = true

        // Act
        method.invoke(SqliteStoreBuilder, releaseName, outputDbPath)

        // Assert
        val connection = DriverManager.getConnection("jdbc:sqlite:${outputDbPath.toAbsolutePath()}")
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery("SELECT release_name FROM version")

        assertTrue(resultSet.next(), "Result set should have at least one row")
        assertEquals(releaseName, resultSet.getString("release_name"), "Release name should match")

        // Cleanup
        resultSet.close()
        statement.close()
        connection.close()
        driver.close()
        Files.deleteIfExists(outputDbPath)
    }

    // Helper method to create a test policy file
    private fun createTestPolicyFile(directory: Path) {
        val categoryDir = directory.resolve(AppCategory.COMMUNICATION.name.lowercase())
        Files.createDirectories(categoryDir)

        val policy = FixedPolicy(
            packageName = "com.example.test",
            category = AppCategory.COMMUNICATION,
            networkPolicy = NetworkPolicy(NetworkMode.WHITELIST, ModeSpec.HostList(setOf("example.com"))),
            minimumVersionCode = 1
        )

        val json = Json {
            classDiscriminator = "type"
            ignoreUnknownKeys = true
            encodeDefaults = false
            prettyPrint = true
        }

        val policyJson = json.encodeToString(io.github.kdroidfilter.database.core.policies.AppPolicy.serializer(), policy)
        val policyFile = categoryDir.resolve("com.example.test.json")

        Files.write(policyFile, policyJson.toByteArray(), StandardOpenOption.CREATE)
    }

    // Helper method to recursively delete a directory
    private fun deleteDirectory(directory: Path) {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }
}

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class UtilsTest {

    @Test
    fun `getDatabasePath should return provided path when not null`() {
        // Arrange
        val testPath = Files.createTempFile("test-db", ".db")

        // Act
        val result = getDatabasePath(testPath)

        // Assert
        assertEquals(testPath, result, "Should return the provided path")

        // Cleanup
        Files.deleteIfExists(testPath)
    }

    @Test
    fun `getDatabasePath should return default path when input is null`() {
        // Act
        val result = getDatabasePath(null)

        // Assert
        val userHome = System.getProperty("user.home")
        val expectedPath = Paths.get(userHome, ".kdroidfilterdb", "store-database.db")
        assertEquals(expectedPath, result, "Should return the default path")

        // Verify directory was created
        assertTrue(Files.exists(result.parent), "Parent directory should be created")
    }

    @Test
    fun `createSqlDriver should create a valid SQL driver`() {
        // Arrange
        val tempDbPath = Files.createTempFile("test-db", ".db")

        // Act
        val driver = createSqlDriver(tempDbPath)

        // Assert
        assertTrue(driver is app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver, "Driver should be a SQLite JDBC driver")

        // Cleanup
        driver.close()
        Files.deleteIfExists(tempDbPath)
    }
}

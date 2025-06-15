import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kdroidfilter.database.store.Database
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.synchronized

/**
 * Gets the database path, either from the provided path or using a default location.
 * 
 * @param outputDbPath Optional path to use for the database. If null, a default path is used.
 * @return The path where the database should be stored.
 */
@Synchronized
fun getDatabasePath(outputDbPath: Path? = null): Path {
    // If outputDbPath is provided, use it
    if (outputDbPath != null) {
        return outputDbPath
    }

    // Otherwise use a path in the user's home directory
    val userHome = System.getProperty("user.home")
    val dbDir = Paths.get(userHome, ".kdroidfilterdb")

    // Create directory if it doesn't exist
    if (!dbDir.toFile().exists()) {
        dbDir.toFile().mkdirs()
    }

    return dbDir.resolve("store-database.db")
}

/**
 * Creates a SQL driver for the database.
 * 
 * @param outputDbPath Optional path to use for the database. If null, a default path is used.
 * @return A configured SqlDriver instance.
 */
fun createSqlDriver(outputDbPath: Path? = null): SqlDriver {
    val dbPath = getDatabasePath(outputDbPath)
    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbPath.toAbsolutePath()}")

    // Create tables if they don't exist
    Database.Schema.create(driver)

    return driver
}

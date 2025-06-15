package sample.app

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import sample.app.utils.isDatabaseExists
import sample.app.utils.downloadDatabaseIfNotExists

private val logger = Logger.withTag("SqlDriverFactory")

actual fun createSqlDriver(): SqlDriver {
    val dbPath = getDatabasePath()
    val language = getDeviceLanguage()

    // Check if database exists with content and download it if it doesn't or is empty
    val dbExists = isDatabaseExists(dbPath)
    logger.i { "Database exists with content check: $dbExists for path: $dbPath" }

    if (!dbExists) {
        logger.i { "Attempting to download database for language: $language" }
        val downloadSuccess = downloadDatabaseIfNotExists(dbPath, language)
        if (!downloadSuccess) {
            logger.e { "Failed to download database. Creating a new empty database." }
        }
    }

    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbPath.toAbsolutePath()}")
    return driver
}



actual fun getDatabasePath(): Path {
    // Use a production-ready directory
    val userHome = System.getProperty("user.home")
    val appDataDir = Paths.get(userHome, ".kdroid-database")

    // Create directory if it doesn't exist
    Files.createDirectories(appDataDir)

    // Use language-specific database file
    val language = getDeviceLanguage()
    return appDataDir.resolve("store-database-${language}.db")
}

actual fun getDeviceLanguage(): String {
    // Get the default locale's language
    val language = Locale.getDefault().language

    // Return the language code (en, fr, he, etc.)
    return when (language) {
        "fr" -> "fr"
        "he", "iw" -> "he" // iw is the old code for Hebrew
        else -> "en" // Default to English for any other language
    }
}

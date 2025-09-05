package io.github.kdroidfilter.database.driver

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import io.github.kdroidfilter.database.utils.DatabaseUtils
import java.io.File
import java.util.Locale

private val logger = Logger.withTag("SqlDriverFactory")

actual fun createSqlDriver(): SqlDriver {
    val dbPath = getDatabasePath()
    val language = getDeviceLanguage()

    val dbExists = DatabaseUtils.isDatabaseExists(dbPath)
    logger.i { "Database exists with content check: $dbExists for path: $dbPath" }

    if (!dbExists) {
        logger.i { "Attempting to download database for language: $language" }
        val downloadSuccess = DatabaseUtils.downloadDatabaseIfNotExists(dbPath, language)
        if (!downloadSuccess) {
            logger.e { "Failed to download database. Creating a new empty database." }
        }
    }

    val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath")
    return driver
}

actual fun getDatabasePath(): String {
    val userHome = System.getProperty("user.home")
    val appDataDir = File(userHome, ".kdroid-database")
    if (!appDataDir.exists()) appDataDir.mkdirs()
    val language = getDeviceLanguage()
    return File(appDataDir, "store-database-${language}.db").absolutePath
}

actual fun getDeviceLanguage(): String {
    val language = Locale.getDefault().language
    return when (language) {
        "fr" -> "fr"
        "he", "iw" -> "he"
        else -> "en"
    }
}

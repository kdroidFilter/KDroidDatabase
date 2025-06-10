package sample.app

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kdroidfilter.database.sample.Database
import java.nio.file.Paths
import java.util.Locale

actual fun createSqlDriver(): SqlDriver {
    val dbPath = Paths.get(getDatabasePath())
    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbPath.toAbsolutePath()}")

    // Create tables if they don't exist
    Database.Schema.create(driver)

    return driver
}

actual fun getDatabasePath(): String {
    // Use a path in the user's home directory
    val userHome = System.getProperty("user.home")
    val dbDir = Paths.get(userHome, ".kdroidfilter")

    // Create directory if it doesn't exist
    if (!dbDir.toFile().exists()) {
        dbDir.toFile().mkdirs()
    }

    return dbDir.resolve("store-database.db").toString()
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

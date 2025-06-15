package sample.app

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

actual fun createSqlDriver(): SqlDriver {
    val dbPath = getDatabasePath()
    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbPath.toAbsolutePath()}")

    return driver
}

actual fun getDatabasePath(): Path {
    // Utiliser le chemin du projet
    val projectRoot = System.getProperty("user.dir")

    // Remonter au répertoire parent si nous sommes dans sample/
    val rootDir = if (projectRoot.endsWith("sample")) {
        Paths.get(projectRoot).parent
    } else {
        Paths.get(projectRoot)
    }

    // Chemin vers la base de données générée
    val language = getDeviceLanguage()
    return rootDir.resolve("../../generators/store/build/store-database-${language}.db")
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

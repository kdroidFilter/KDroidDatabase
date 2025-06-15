package sample.app

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import co.touchlab.kermit.Logger
import io.github.kdroidfilter.database.store.Database
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import sample.app.utils.isDatabaseExists
import sample.app.utils.downloadDatabaseIfNotExists

private lateinit var applicationContext: Context
private val logger = Logger.withTag("SqlDriverFactory")

fun initializeContext(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createSqlDriver(): SqlDriver {
    val language = getDeviceLanguage()
    val dbName = "store-database-${language}.db"

    // Check if database exists with content and download it if it doesn't or is empty
    val dbPath = getDatabasePath()
    val dbExists = isDatabaseExists(dbPath)
    logger.i { "Database exists with content check: $dbExists for path: $dbPath" }

    if (!dbExists) {
        logger.i { "Attempting to download database for language: $language" }
        // Use runBlocking but with withContext to move the network operation to IO dispatcher
        runBlocking {
            try {
                val downloadSuccess = withContext(Dispatchers.IO) {
                    downloadDatabaseIfNotExists(dbPath, language)
                }
                if (!downloadSuccess) {
                    logger.e { "Failed to download database. Creating a new empty database." }
                }
            } catch (e: Exception) {
                logger.e(e) { "❌ Failed to download store database for $language: ${e.message}" }
            }
        }
    }

    val driver = AndroidSqliteDriver(
        schema = Database.Schema,
        context = applicationContext,
        name = dbName
    )
    return driver
}




actual fun getDatabasePath(): Path {
    val language = getDeviceLanguage()
    val databaseFile = applicationContext.getDatabasePath("store-database-${language}.db")
    return Paths.get(databaseFile.absolutePath)
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

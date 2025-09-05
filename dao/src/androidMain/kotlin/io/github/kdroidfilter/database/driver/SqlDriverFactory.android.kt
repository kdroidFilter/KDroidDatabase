package io.github.kdroidfilter.database.driver

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import co.touchlab.kermit.Logger
import io.github.kdroidfilter.database.store.Database
import io.github.kdroidfilter.database.utils.DatabaseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Locale

private lateinit var applicationContext: Context
private val logger = Logger.withTag("SqlDriverFactory")

fun initializeContext(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createSqlDriver(): SqlDriver {
    val language = getDeviceLanguage()
    val dbName = "store-database-${language}.db"

    val dbPath = getDatabasePath()
    val dbExists = DatabaseUtils.isDatabaseExists(dbPath)
    logger.i { "Database exists with content check: $dbExists for path: $dbPath" }

    if (!dbExists) {
        logger.i { "Attempting to download database for language: $language" }
        runBlocking {
            try {
                val downloadSuccess = withContext(Dispatchers.IO) {
                    DatabaseUtils.downloadDatabaseIfNotExists(dbPath, language)
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

actual fun getDatabasePath(): String {
    val language = getDeviceLanguage()
    val databaseFile = applicationContext.getDatabasePath("store-database-${language}.db")
    return databaseFile.absolutePath
}

actual fun getDeviceLanguage(): String {
    val language = Locale.getDefault().language
    return when (language) {
        "fr" -> "fr"
        "he", "iw" -> "he"
        else -> "en"
    }
}

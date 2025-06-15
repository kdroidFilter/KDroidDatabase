package sample.app.utils

import co.touchlab.kermit.Logger
import io.github.kdroidfilter.database.dao.VersionDao
import io.github.kdroidfilter.database.downloader.DatabaseDownloader
import io.github.kdroidfilter.database.downloader.DatabaseVersionChecker
import io.github.kdroidfilter.database.store.Database
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path

/**
 * Checks if the database file exists at the specified path and has a non-zero size.
 * @param databasePath The path to the database file
 * @return Boolean indicating whether the database file exists and has content
 */
fun isDatabaseExists(databasePath: Path): Boolean {
    val logger = Logger.withTag("DatabaseUtils")
    try {
        val exists = Files.exists(databasePath)
        if (!exists) {
            logger.w { "⚠️ Database file does not exist at: $databasePath" }
            return false
        }

        val size = Files.size(databasePath)
        val isValid = size > 60000

        if (isValid) {
            logger.i { "✅ Database file exists and has content (${size} bytes) at: $databasePath" }
        } else {
            logger.w { "⚠️ Database file exists but is empty (0 bytes) at: $databasePath" }
        }

        return isValid
    } catch (e: Exception) {
        logger.e(e) { "❌ Failed to check if database exists or has content: ${e.message}" }
        return false
    }
}

/**
 * Downloads the database if it doesn't exist at the specified path or has zero size.
 * @param databasePath The path where the database should be located
 * @param language The language code (en, fr, he) for which to download the database
 * @return Boolean indicating whether the database exists with content or was successfully downloaded
 */
fun downloadDatabaseIfNotExists(databasePath: Path, language: String): Boolean {
    val logger = Logger.withTag("DatabaseUtils")

    // Check if database exists and has content
    if (isDatabaseExists(databasePath)) {
        logger.i { "✅ Database already exists with content at: $databasePath" }
        return true
    }

    logger.i { "🔄 Database does not exist or is empty. Attempting to download..." }

    // Get the parent directory of the database file
    val parentDir = databasePath.parent.toString()

    // Download the database
    return runBlocking {
        val downloader = DatabaseDownloader()
        val success = downloader.downloadLatestStoreDatabaseForLanguage(parentDir, language)

        if (success) {
            logger.i { "✅ Database downloaded successfully to: $databasePath" }
        } else {
            logger.e { "❌ Failed to download database to: $databasePath" }
        }

        success
    }
}

/**
 * Checks if the database version is up to date using the version stored in the database.
 * @param database The database instance
 * @return Boolean indicating whether the database version is up to date
 */
suspend fun isDatabaseVersionUpToDate(database: Database): Boolean {
    val logger = Logger.withTag("DatabaseUtils")
    try {
        // Get the current version from the database
        val currentVersion = VersionDao.getCurrentVersion(database)

        if (currentVersion == null) {
            logger.w { "⚠️ No version information found in the database" }
            return false
        }

        logger.i { "🔄 Checking if database version $currentVersion is up to date using DAO..." }

        return DatabaseVersionChecker().isDatabaseVersionUpToDate(currentVersion)
    } catch (e: Exception) {
        logger.e(e) { "❌ Failed to check database version using DAO: ${e.message}" }
        return false
    }
}

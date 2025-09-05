package io.github.kdroidfilter.database.utils

import co.touchlab.kermit.Logger
import io.github.kdroidfilter.database.dao.VersionDao
import io.github.kdroidfilter.database.downloader.DatabaseDownloader
import io.github.kdroidfilter.database.downloader.DatabaseVersionChecker
import io.github.kdroidfilter.database.store.Database
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Utilities to help initialize and maintain the KDroid database.
 * Integrated into the library to simplify usage for applications.
 */
object DatabaseUtils {
    private val logger = Logger.withTag("DatabaseUtils")

    /**
     * Checks if the database file exists at the specified path and has a minimal size.
     * @param databasePath The path to the database file (absolute path string)
     * @return true if the file exists and likely contains valid content
     */
    fun isDatabaseExists(databasePath: String): Boolean {
        return try {
            val dbFile = File(databasePath)
            val exists = dbFile.exists()
            if (!exists) {
                logger.w { "⚠️ Database file does not exist at: $databasePath" }
                false
            } else {
                val size = dbFile.length()
                val isValid = size > 60000
                if (isValid) {
                    logger.i { "✅ Database file exists and has content (${size} bytes) at: $databasePath" }
                } else {
                    logger.w { "⚠️ Database file exists but is empty or too small (${size} bytes) at: $databasePath" }
                }
                isValid
            }
        } catch (e: Exception) {
            logger.e(e) { "❌ Failed to check if database exists or has content: ${e.message}" }
            false
        }
    }

    /**
     * Downloads the database if it doesn't exist at the specified path or has insufficient size.
     * @param databasePath The path where the database should be located (absolute path string)
     * @param language The language code (en, fr, he) for which to download the database
     * @return true if the database exists with content or was successfully downloaded
     */
    fun downloadDatabaseIfNotExists(databasePath: String, language: String): Boolean {
        // Check first
        if (isDatabaseExists(databasePath)) {
            logger.i { "✅ Database already exists with content at: $databasePath" }
            return true
        }

        logger.i { "🔄 Database does not exist or is empty. Attempting to download..." }

        // Get the parent directory of the database file
        val parentDir = File(databasePath).parentFile?.absolutePath ?: "."

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
     */
    suspend fun isDatabaseVersionUpToDate(database: Database): Boolean {
        return try {
            val currentVersion = VersionDao.getCurrentVersion(database)
            if (currentVersion == null) {
                logger.w { "⚠️ No version information found in the database" }
                false
            } else {
                DatabaseVersionChecker().isDatabaseVersionUpToDate(currentVersion)
            }
        } catch (e: Exception) {
            logger.e(e) { "❌ Failed to check database version using DAO: ${e.message}" }
            false
        }
    }
}

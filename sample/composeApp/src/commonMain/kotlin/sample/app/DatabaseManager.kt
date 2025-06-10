package sample.app

import co.touchlab.kermit.Logger
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Manages database operations, including downloading and refreshing the database.
 */
object DatabaseManager {
    private val logger = Logger.withTag("DatabaseManager")

    /**
     * Downloads the latest database from GitHub releases if it doesn't exist locally.
     * @return true if download was successful or database already exists, false otherwise
     */
    suspend fun downloadLatestDatabaseIfNotExists(): Boolean {
        val dbPath = getDatabasePath()

        // Check if database already exists and has data
        val dbFile = dbPath.toFile()
        val dbExists = dbFile.exists() && dbFile.length() > 30000

        if (dbExists) {
            logger.i { "✅ Database already exists at $dbPath" }
            return true
        }

        // Database doesn't exist or is too small (likely empty), download it
        return refreshDatabase()
    }

    /**
     * Downloads the latest database from GitHub releases, replacing any existing database.
     * @return true if download was successful, false otherwise
     */
    suspend fun refreshDatabase(): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbPath = getDatabasePath()
            logger.i { "🔄 Attempting to download the latest database..." }

            val fetcher = GitHubReleaseFetcher(owner = "kdroidFilter", repo = "KDroidDatabase")
            val latestRelease = fetcher.getLatestRelease()

            if (latestRelease != null && latestRelease.assets.size > 1) {
                // Find the store-database.db asset
                val downloadUrl = latestRelease.assets[1].browser_download_url

                logger.i { "📥 Downloading database from: $downloadUrl" }

                Files.createDirectories(dbPath.parent)

                // Download the file
                URL(downloadUrl).openStream().use { input ->
                    Files.copy(input, dbPath, StandardCopyOption.REPLACE_EXISTING)
                }

                // Verify the file was downloaded successfully
                val dbFile = dbPath.toFile()
                if (dbFile.exists() && dbFile.length() > 1024) { // Ensure it's at least 1KB
                    logger.i { "✅ Database downloaded successfully to $dbPath" }
                    return@withContext true
                } else {
                    logger.w { "⚠️ Downloaded file is empty, too small, or does not exist" }
                    return@withContext false
                }
            } else {
                logger.w { "⚠️ No database assets found in the latest release" }
                return@withContext false
            }
        } catch (e: Exception) {
            logger.e(e) { "❌ Failed to download the latest database: ${e.message}" }
            return@withContext false
        }
    }
}

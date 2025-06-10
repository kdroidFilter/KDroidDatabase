package sample.app

import co.touchlab.kermit.Logger
import io.github.kdroidfilter.database.sample.Database
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Android implementation of [DatabaseManager].
 */
actual object DatabaseManager {
    private val logger = Logger.withTag("DatabaseManager")

    actual suspend fun downloadLatestDatabaseIfNotExists(): Boolean {
        val dbPath = Paths.get(getDatabasePath())

        val dbFile = dbPath.toFile()
        val dbExists = dbFile.exists() && dbFile.length() > 30000

        if (dbExists) {
            logger.i { "✅ Database already exists at $dbPath" }
            return true
        }

        return refreshDatabase()
    }

    actual suspend fun refreshDatabase(): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbPath = Paths.get(getDatabasePath())
            logger.i { "🔄 Checking for database updates..." }

            val fetcher = GitHubReleaseFetcher(owner = "kdroidFilter", repo = "KDroidDatabase")
            val latestRelease = fetcher.getLatestRelease()

            if (latestRelease != null && latestRelease.assets.size > 1) {
                val dbFile = dbPath.toFile()
                val dbExists = dbFile.exists() && dbFile.length() > 30000

                if (dbExists) {
                    try {
                        val database = Database(createSqlDriver())
                        val currentVersion = database.storeQueries.getCurrentVersion().executeAsOneOrNull()
                        if (currentVersion != null && currentVersion.release_name == latestRelease.name) {
                            logger.i { "✅ Database is already the most recent version (${latestRelease.name})" }
                            return@withContext true
                        }
                    } catch (e: Exception) {
                        logger.w(e) { "⚠️ Could not check current database version: ${'$'}{e.message}" }
                    }
                }

                val downloadUrl = latestRelease.assets[1].browser_download_url

                logger.i { "📥 Downloading database version ${'$'}{latestRelease.name} from: ${'$'}downloadUrl" }

                Files.createDirectories(dbPath.parent)

                URL(downloadUrl).openStream().use { input ->
                    Files.copy(input, dbPath, StandardCopyOption.REPLACE_EXISTING)
                }

                if (dbFile.exists() && dbFile.length() > 30000) {
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
            logger.e(e) { "❌ Failed to download the latest database: ${'$'}{e.message}" }
            return@withContext false
        }
    }
}

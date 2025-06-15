package io.github.kdroidfilter.database.downloader

import co.touchlab.kermit.Logger
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher

/**
 * Class responsible for checking if the KDroid database version is up to date.
 */
class DatabaseVersionChecker {
    private val logger = Logger.withTag("DatabaseVersionChecker")

    /**
     * Checks if the database version is up to date compared to the latest release using the release name.
     * @param version The version string to check against the latest release name
     * @return Boolean indicating whether the provided version is up to date
     */
    suspend fun isDatabaseVersionUpToDate(version: String): Boolean {
        try {
            logger.i { "🔄 Checking if database version $version is up to date using release name..." }
            val fetcher = GitHubReleaseFetcher(owner = GitHubConstants.OWNER, repo = GitHubConstants.REPO)
            val latestRelease = fetcher.getLatestRelease()

            if (latestRelease != null) {
                val latestVersion = latestRelease.name

                val isUpToDate = version == latestVersion

                if (isUpToDate) {
                    logger.i { "✅ Database version $version is up to date with latest release $latestVersion" }
                } else {
                    logger.i { "⚠️ Database version $version is outdated. Latest release is $latestVersion" }
                }

                return isUpToDate
            } else {
                logger.w { "⚠️ Could not fetch latest release information" }
                return false
            }
        } catch (e: Exception) {
            logger.e(e) { "❌ Failed to check database version by release name: ${e.message}" }
            return false
        }
    }
}

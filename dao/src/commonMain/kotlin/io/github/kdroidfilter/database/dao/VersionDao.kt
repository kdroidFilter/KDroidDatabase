package io.github.kdroidfilter.database.dao

import co.touchlab.kermit.Logger
import io.github.kdroidfilter.database.store.Database

/**
 * Data Access Object for Version
 * Contains functions for database operations related to version information
 */
object VersionDao {
    private val logger = Logger.withTag("VersionDao")

    /**
     * Gets the current version from the database
     * @param database The database instance
     * @return The version string or null if no version is found
     */
    fun getCurrentVersion(database: Database): String? {
        try {
            val versionQueries = database.versionQueries
            val version = versionQueries.getVersion().executeAsOneOrNull()
            return version?.release_name
        } catch (e: Exception) {
            logger.e(e) { "Failed to get current version: ${e.message}" }
            return null
        }
    }

    /**
     * Updates the version in the database
     * @param database The database instance
     * @param versionName The new version name
     * @return Boolean indicating whether the update was successful
     */
    fun updateVersion(database: Database, versionName: String): Boolean {
        return try {
            val versionQueries = database.versionQueries
            
            // Clear existing versions
            versionQueries.clearVersions()
            
            // Insert new version
            versionQueries.insertVersion(versionName)
            
            logger.i { "Version updated to $versionName" }
            true
        } catch (e: Exception) {
            logger.e(e) { "Failed to update version: ${e.message}" }
            false
        }
    }
}
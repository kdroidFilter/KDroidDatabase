package sample.app

/**
 * Manages database operations for platforms with a filesystem.
 */
expect object DatabaseManager {
    /**
     * Downloads the latest database if it does not exist locally.
     * @return true if the database is available after the call, false otherwise.
     */
    suspend fun downloadLatestDatabaseIfNotExists(): Boolean

    /**
     * Refreshes the local database if a newer version is available.
     * @return true if the database was refreshed, false otherwise.
     */
    suspend fun refreshDatabase(): Boolean
}

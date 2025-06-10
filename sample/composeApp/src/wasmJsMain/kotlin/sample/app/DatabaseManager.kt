package sample.app

/**
 * WASM implementation of [DatabaseManager].
 * WASM downloads the database from the network using the custom worker.
 */
actual object DatabaseManager {
    actual suspend fun downloadLatestDatabaseIfNotExists(): Boolean {
        // The WebWorker fetches the database on first access.
        return true
    }

    actual suspend fun refreshDatabase(): Boolean {
        // Reload the page to fetch the latest database.
        js("location.reload()");
        return true
    }
}

import kotlinx.coroutines.runBlocking
import java.nio.file.Path

/**
 * Main function to extract and build store databases
 * 
 * Usage:
 * - Run without arguments to build or update databases normally (download if available)
 * - Run with argument "force-scratch" to force building from scratch
 */
fun main(args: Array<String>) {
    // Check if force-scratch argument is provided
    val forceFromScratch = args.isNotEmpty() && args[0] == "force-scratch"
    val projectDir = Path.of("").toAbsolutePath()
    val policiesDir = projectDir.resolve("../../app-policies")

    // This path is used only to determine the output directory (its parent directory)
    // The actual database files will be created as:
    // - build/store-database-en.db (English)
    // - build/store-database-fr.db (French)
    // - build/store-database-he.db (Hebrew)
    val baseDbPath = projectDir.resolve("build/store-database.db")

    // Build databases in multiple languages (English, French, Hebrew)
    runBlocking {
        if (forceFromScratch) {
            println("Building databases from scratch (forced)...")
        } else {
            println("Building or updating databases (downloading existing ones if available)...")
        }
        SqliteStoreBuilder.buildOrUpdateMultiLanguageDatabases(policiesDir, baseDbPath, forceFromScratch)
    }
}

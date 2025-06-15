import java.nio.file.Path

fun main() {
    val projectDir = Path.of("").toAbsolutePath()
    val policiesDir = projectDir.resolve("../../app-policies")

    // This path is used only to determine the output directory (its parent directory)
    // The actual database files will be created as:
    // - build/store-database-en.db (English)
    // - build/store-database-fr.db (French)
    // - build/store-database-he.db (Hebrew)
    val baseDbPath = projectDir.resolve("build/store-database.db")

    // Build databases in multiple languages (English, French, Hebrew)
    SqliteStoreBuilder.buildMultiLanguageDatabases(policiesDir, baseDbPath)
}

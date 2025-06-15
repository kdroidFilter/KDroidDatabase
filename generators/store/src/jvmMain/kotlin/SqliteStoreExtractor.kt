import java.nio.file.Path

fun main() {
    val projectDir = Path.of("").toAbsolutePath()
    val policiesDir = projectDir.resolve("../../app-policies")
    val outputDb = projectDir.resolve("build/store-database.db")
    
    SqliteStoreBuilder.buildDatabase(policiesDir, outputDb)
}
import java.nio.file.Path

fun main() {
    val projectDir = Path.of("").toAbsolutePath()
    val policiesDir = projectDir.parent.resolve("../app-policies")
    val outputDb = projectDir.resolve("build/policies-database.db")
    
    SqlitePolicyBuilder.buildDatabase(policiesDir, outputDb)
}
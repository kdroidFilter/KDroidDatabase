import co.touchlab.kermit.Logger
import io.github.kdroidfilter.database.core.policies.AppPolicy
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SqlitePolicyBuilder {
    private val logger = Logger.withTag("SqlitePolicyBuilder")
    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = false
        serializersModule = PolicyRepository.json.serializersModule
    }

    fun buildDatabase(policiesDir: Path, outputDbPath: Path) {
        // Always create the database from scratch
        logger.i { "🔄 Creating database from scratch..." }

        // Delete existing database file if it exists
        if (Files.exists(outputDbPath)) {
            logger.i { "🗑️ Deleting existing database file..." }
            Files.delete(outputDbPath)
        }

        // Get release name from environment variable or generate timestamp
        val releaseName = System.getenv("RELEASE_NAME") ?: LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
        logger.i { "🏷️ Using release name: $releaseName" }

        Class.forName("org.sqlite.JDBC")
        Files.createDirectories(outputDbPath.parent)
        val url = "jdbc:sqlite:${outputDbPath.toAbsolutePath()}"
        DriverManager.getConnection(url).use { conn ->
            conn.autoCommit = false
            createTables(conn)
            insertVersion(conn, releaseName)
            insertPolicies(conn, policiesDir)
            conn.commit()
        }
        logger.i { "✅ SQLite database created at $outputDbPath" }
    }

    private fun createTables(conn: Connection) = with(conn.createStatement()) {
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS policies (
              package_name TEXT PRIMARY KEY,
              data         TEXT NOT NULL
            )
        """.trimIndent())

        executeUpdate("""
            CREATE TABLE IF NOT EXISTS version (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              release_name TEXT NOT NULL
            )
        """.trimIndent())
        close()
    }

    private fun insertVersion(conn: Connection, releaseName: String) {
        // Clear existing entries
        conn.createStatement().use { stmt ->
            stmt.executeUpdate("DELETE FROM version")
        }

        // Insert new release name
        conn.prepareStatement("INSERT INTO version (release_name) VALUES (?)").use { ps ->
            ps.setString(1, releaseName)
            ps.executeUpdate()
        }
        logger.i { "✅ Inserted version info: $releaseName" }
    }

    private fun insertPolicies(conn: Connection, policiesDir: Path) {
        val insertSql = """
            INSERT OR REPLACE INTO policies(package_name, data) 
            VALUES(?, ?)
        """.trimIndent()

        conn.prepareStatement(insertSql).use { ps ->
            val policies = PolicyRepository.loadAll(policiesDir)
            policies.forEach { policy ->
                val jsonStr = json.encodeToString(AppPolicy.serializer(), policy)
                ps.setString(1, policy.packageName)
                ps.setString(2, jsonStr)
                ps.addBatch()
            }
            ps.executeBatch()
            logger.i { "✅ Inserted ${policies.size} policies" }
        }
    }
}

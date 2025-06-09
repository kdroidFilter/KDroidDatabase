package io.github.kdroidfilter.database.generator

import io.github.kdroidfilter.database.core.policies.AppPolicy
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SQLitePolicyExporter {
    // Reuse the same JSON configuration as PolicyRepository
    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = false
        serializersModule = PolicyRepository.json.serializersModule
    }

    fun exportAll(root: Path, outputDb: Path) {
        // Load the SQLite driver
        Class.forName("org.sqlite.JDBC")

        // Get release name from environment variable or generate timestamp
        val releaseName = System.getenv("RELEASE_NAME") ?: LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
        println("🏷️ Using release name: $releaseName")

        // Creation / opening of the database
        val url = "jdbc:sqlite:${outputDb.toAbsolutePath()}"
        DriverManager.getConnection(url).use { conn ->
            conn.autoCommit = false
            createTable(conn)
            insertVersion(conn, releaseName)

            val insertSql = """
                INSERT OR REPLACE INTO policies(package_name, data) 
                VALUES(?, ?)
            """.trimIndent()

            conn.prepareStatement(insertSql).use { ps ->
                PolicyRepository.loadAll(root).forEach { policy ->
                    val jsonStr = json.encodeToString(AppPolicy.serializer(), policy)
                    ps.setString(1, policy.packageName)
                    ps.setString(2, jsonStr)
                    ps.addBatch()
                }
                ps.executeBatch()
            }

            conn.commit()
            println("✅ Export ${PolicyRepository.loadAll(root).size} policies in $outputDb")
        }
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
        println("✅ Inserted version info: $releaseName")
    }

    private fun createTable(conn: Connection) {
        conn.createStatement().use { stmt ->
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS policies (
                  package_name TEXT PRIMARY KEY,
                  data         TEXT NOT NULL
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS version (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  release_name TEXT NOT NULL
                )
            """.trimIndent())
        }
    }
}

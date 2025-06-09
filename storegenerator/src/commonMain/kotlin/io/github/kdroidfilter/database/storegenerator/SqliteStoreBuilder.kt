package io.github.kdroidfilter.database.storegenerator

import co.touchlab.kermit.Logger
import com.kdroid.gplayscrapper.services.getGooglePlayApplicationInfo
import io.github.kdroidfilter.database.core.AppCategory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

object SqliteStoreBuilder {
    private val logger = Logger.withTag("SqliteStoreBuilder")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun buildDatabase(appPoliciesDir: Path, outputDbPath: Path) {
        Class.forName("org.sqlite.JDBC")
        Files.createDirectories(outputDbPath.parent)
        val url = "jdbc:sqlite:${outputDbPath.toAbsolutePath()}"
        DriverManager.getConnection(url).use { conn ->
            conn.autoCommit = false
            createTables(conn)
            insertCategories(conn)
            upsertPackages(conn, appPoliciesDir)
            conn.commit()
        }
        logger.i { "✅ SQLite database created at $outputDbPath" }
    }

    private fun createTables(conn: Connection) = with(conn.createStatement()) {
        // Replace OLD TABLE with the new definition
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE
            )
        """.trimIndent())
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS packages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category_id INTEGER NOT NULL,
                package_name TEXT NOT NULL UNIQUE,
                store_info_en TEXT,
                store_info_fr TEXT,
                store_info_he TEXT,
                FOREIGN KEY (category_id) REFERENCES categories(id)
            )
        """.trimIndent())
        close()
    }

    private fun insertCategories(conn: Connection) {
        conn.prepareStatement("INSERT OR IGNORE INTO categories (name) VALUES (?)").use { ps ->
            AppCategory.entries.forEach { cat ->
                ps.setString(1, cat.name)
                ps.addBatch()
            }
            ps.executeBatch()
        }
        logger.i { "✅ Inserted ${AppCategory.entries.size} categories" }
    }

    private fun upsertPackages(conn: Connection, dir: Path) {
        // Load existing information to avoid re-fetching if already present
        data class Infos(val en: String?, val fr: String?, val he: String?)
        val existing = mutableMapOf<String, Infos>()
        conn.createStatement().use { stmt ->
            stmt.executeQuery(
                "SELECT package_name, store_info_en, store_info_fr, store_info_he FROM packages"
            ).use { rs ->
                while (rs.next()) {
                    existing[rs.getString("package_name")] = Infos(
                        rs.getString("store_info_en"),
                        rs.getString("store_info_fr"),
                        rs.getString("store_info_he")
                    )
                }
            }
        }

        val sql = """
            INSERT INTO packages
              (package_name, category_id, store_info_en, store_info_fr, store_info_he)
            VALUES (?, (SELECT id FROM categories WHERE name = ?), ?, ?, ?)
            ON CONFLICT(package_name) DO UPDATE SET
              category_id    = excluded.category_id,
              store_info_en  = COALESCE(excluded.store_info_en, store_info_en),
              store_info_fr  = COALESCE(excluded.store_info_fr, store_info_fr),
              store_info_he  = COALESCE(excluded.store_info_he, store_info_he)
        """.trimIndent()

        conn.prepareStatement(sql).use { ps ->
            var count = 0
            Files.walk(dir)
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                .forEach { file ->
                    val pkg = file.fileName.toString().substringBeforeLast(".")
                    val catName = file.parent.fileName.toString()
                        .uppercase().replace('-', '_')

                    val prev = existing[pkg]
                    // For each language, only fetch if empty
                    val infos = runBlocking {
                        listOf("en" to prev?.en, "fr" to prev?.fr, "he" to prev?.he)
                            .map { (lang, previous) ->
                                previous ?: runCatching {
                                    getGooglePlayApplicationInfo(pkg, lang, "us")
                                }
                                    .map { json.encodeToString(it) }
                                    .onFailure { e ->
                                        logger.w { "⚠️ Failed to fetch [$lang] for $pkg: ${e.message}" }
                                    }
                                    .getOrNull()
                            }
                    }

                    ps.setString(1, pkg)
                    ps.setString(2, catName)
                    ps.setString(3, infos[0]) // en
                    ps.setString(4, infos[1]) // fr
                    ps.setString(5, infos[2]) // he
                    ps.addBatch()
                    count++
                }

            ps.executeBatch()
            logger.i { "✅ Processed $count packages" }
        }
    }
}

fun main() {
    val root = Path.of("").toAbsolutePath()
    val policies = root.resolve("../app-policies")
    val output = root.resolve("build/store-database.db")
    SqliteStoreBuilder.buildDatabase(policies, output)
}

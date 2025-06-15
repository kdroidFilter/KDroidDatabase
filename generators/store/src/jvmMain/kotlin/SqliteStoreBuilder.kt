import co.touchlab.kermit.Logger
import com.kdroid.gplayscrapper.services.getGooglePlayApplicationInfo
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SqliteStoreBuilder {
    private val logger = Logger.withTag("SqliteStoreBuilder")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    /**
     * Attempts to download the latest database from GitHub releases.
     * @return true if download was successful, false otherwise
     */
    private fun downloadLatestDatabase(outputDbPath: Path): Boolean = runBlocking {
        try {
            logger.i { "🔄 Attempting to download the latest database..." }
            val fetcher = GitHubReleaseFetcher(owner = "kdroidFilter", repo = "KDroidDatabase")
            val latestRelease = fetcher.getLatestRelease()

            if (latestRelease != null && latestRelease.assets.size > 1) {
                // Find the store-database.db asset
                val downloadUrl = latestRelease.assets[1].browser_download_url

                logger.i { "📥 Downloading database from: $downloadUrl" }

                Files.createDirectories(outputDbPath.parent)

                // Download the file
                URL(downloadUrl).openStream().use { input ->
                    Files.copy(input, outputDbPath, StandardCopyOption.REPLACE_EXISTING)
                }

                // Verify the file was downloaded successfully
                if (Files.exists(outputDbPath) && Files.size(outputDbPath) > 0) {
                    logger.i { "✅ Database downloaded successfully to $outputDbPath" }
                    return@runBlocking true
                } else {
                    logger.w { "⚠️ Downloaded file is empty or does not exist" }
                    return@runBlocking false
                }
            } else {
                logger.w { "⚠️ No database assets found in the latest release" }
                return@runBlocking false
            }
        } catch (e: Exception) {
            logger.e(e) { "❌ Failed to download the latest database: ${e.message}" }
            return@runBlocking false
        }
    }

    fun buildDatabase(appPoliciesDir: Path, outputDbPath: Path) {
        // First try to download the latest database
        if (downloadLatestDatabase(outputDbPath)) {
            logger.i { "✅ Using downloaded database at $outputDbPath" }
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
            insertCategories(conn)
            insertVersion(conn, releaseName)
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
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS version (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                release_name TEXT NOT NULL
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

        val insertSql = """
        INSERT INTO packages
          (package_name, category_id, store_info_en, store_info_fr, store_info_he)
        VALUES (?, (SELECT id FROM categories WHERE name = ?), ?, ?, ?)
    """.trimIndent()

        val updateSql = """
        UPDATE packages SET
          category_id = (SELECT id FROM categories WHERE name = ?),
          store_info_en = COALESCE(?, store_info_en),
          store_info_fr = COALESCE(?, store_info_fr),
          store_info_he = COALESCE(?, store_info_he)
        WHERE package_name = ?
    """.trimIndent()

        conn.prepareStatement(insertSql).use { insertPs ->
            conn.prepareStatement(updateSql).use { updatePs ->
                var count = 0
                Files.walk(dir)
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                    .forEach { file ->
                        val pkg = file.fileName.toString().substringBeforeLast(".")
                        val catName = file.parent.fileName.toString()
                            .uppercase().replace('-', '_')

                        val prev = existing[pkg]
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

                        if (existing.containsKey(pkg)) {
                            // UPDATE existing package
                            updatePs.setString(1, catName)
                            updatePs.setString(2, infos[0]) // en
                            updatePs.setString(3, infos[1]) // fr
                            updatePs.setString(4, infos[2]) // he
                            updatePs.setString(5, pkg)
                            updatePs.addBatch()
                        } else {
                            // INSERT new package
                            insertPs.setString(1, pkg)
                            insertPs.setString(2, catName)
                            insertPs.setString(3, infos[0]) // en
                            insertPs.setString(4, infos[1]) // fr
                            insertPs.setString(5, infos[2]) // he
                            insertPs.addBatch()
                        }
                        count++
                    }

                insertPs.executeBatch()
                updatePs.executeBatch()
                logger.i { "✅ Processed $count packages" }
            }
        }
    }
}

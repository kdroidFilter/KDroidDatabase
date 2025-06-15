import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import createSqlDriver
import co.touchlab.kermit.Logger
import com.kdroid.gplayscrapper.core.model.GooglePlayApplicationInfo
import com.kdroid.gplayscrapper.services.getGooglePlayApplicationInfo
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.store.App_categories
import io.github.kdroidfilter.database.store.App_categoriesQueries
import io.github.kdroidfilter.database.store.Applications
import io.github.kdroidfilter.database.store.ApplicationsQueries
import io.github.kdroidfilter.database.store.Database
import io.github.kdroidfilter.database.store.Developers
import io.github.kdroidfilter.database.store.DevelopersQueries
import io.github.kdroidfilter.database.store.VersionQueries
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
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
//        if (downloadLatestDatabase(outputDbPath)) {
//            logger.i { "✅ Using downloaded database at $outputDbPath" }
//            return // Use the downloaded database
//        }
        //Disable for testing

        // Get release name from environment variable or generate timestamp
        val releaseName = System.getenv("RELEASE_NAME") ?: LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
        logger.i { "🏷️ Using release name: $releaseName" }

        // Ensure the directory exists
        Files.createDirectories(outputDbPath.parent)

        // Create a SqlDriver for the database
        val driver = JdbcSqliteDriver("jdbc:sqlite:${outputDbPath.toAbsolutePath()}")

        // Create the database schema
        Database.Schema.create(driver)

        // Insert categories
        insertCategories()

        // Insert version information
        insertVersion(releaseName)

        // Insert packages
        upsertPackages(appPoliciesDir)

        logger.i { "✅ SQLite database created at $outputDbPath" }
    }


    private fun insertCategories() {
        val appCategoriesQueries = App_categoriesQueries(createSqlDriver())
        var count = 0

        AppCategory.entries.forEach { cat ->
            try {
                appCategoriesQueries.insertCategory(
                    category_name = cat.name,
                    description = null
                )
                count++
            } catch (e: Exception) {
                // Category might already exist, ignore
                logger.d { "Category ${cat.name} already exists or could not be inserted: ${e.message}" }
            }
        }

        logger.i { "✅ Inserted $count categories" }
    }

    private fun insertVersion(releaseName: String) {
        val versionQueries = VersionQueries(createSqlDriver())

        // Clear existing entries
        versionQueries.clearVersions()

        // Insert new release name
        versionQueries.insertVersion(releaseName)

        logger.i { "✅ Inserted version info: $releaseName" }
    }

    private fun upsertPackages(dir: Path) {
        // Get existing applications to avoid re-fetching
        val existingApps = mutableMapOf<String, GooglePlayApplicationInfo?>()

        var count = 0
        Files.walk(dir)
            .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
            .forEach { file ->
                val packageName = file.fileName.toString().substringBeforeLast(".")
                val categoryName = file.parent.fileName.toString()
                    .uppercase().replace('-', '_')

                // Get or create the category
                val appCategoriesQueries = App_categoriesQueries(createSqlDriver())
                val category = appCategoriesQueries
                    .getCategoryByName(categoryName)
                    .executeAsOneOrNull() ?: run {
                        // Insert the category if it doesn't exist
                        appCategoriesQueries.insertCategory(
                            category_name = categoryName,
                            description = null
                        )
                        appCategoriesQueries.getCategoryByName(categoryName).executeAsOne()
                    }

                // Fetch application info from Google Play (only English for now)
                val appInfo = runBlocking {
                    existingApps.getOrPut(packageName) {
                        runCatching {
                            getGooglePlayApplicationInfo(packageName, "en", "us")
                        }.getOrNull()
                    }
                }

                if (appInfo != null) {
                    // Create DevelopersQueries instance
                    val developersQueries = DevelopersQueries(createSqlDriver())

                    // Get or create the developer
                    val developer = developersQueries
                        .getDeveloperByDeveloperId(appInfo.developerId)
                        .executeAsOneOrNull() ?: run {
                            // Insert the developer if it doesn't exist
                            developersQueries.insertDeveloper(
                                developer_id = appInfo.developerId,
                                name = appInfo.developer,
                                email = null,
                                website = appInfo.developerWebsite,
                                address = null
                            )
                            developersQueries.getDeveloperByDeveloperId(appInfo.developerId).executeAsOne()
                        }

                    // Create ApplicationsQueries instance
                    val applicationsQueries = ApplicationsQueries(createSqlDriver())

                    // Check if the application already exists
                    val existingApp = applicationsQueries
                        .getApplicationByAppId(packageName)
                        .executeAsOneOrNull()

                    if (existingApp == null) {
                        // Insert the application
                        applicationsQueries.insertApplication(
                            app_id = packageName,
                            title = appInfo.title,
                            description = appInfo.description,
                            description_html = appInfo.descriptionHTML,
                            summary = appInfo.summary,
                            installs = appInfo.installs,
                            min_installs = appInfo.minInstalls?.toLong(),
                            real_installs = appInfo.realInstalls?.toLong(),
                            score = appInfo.score,
                            ratings = appInfo.ratings?.toLong(),
                            reviews = appInfo.reviews?.toLong(),
                            histogram = appInfo.histogram?.toString(),
                            price = appInfo.price,
                            free = if (appInfo.free) 1 else 0,
                            currency = appInfo.currency,
                            sale = if (appInfo.sale) 1 else 0,
                            sale_time = null,
                            original_price = appInfo.originalPrice,
                            sale_text = appInfo.saleText,
                            offers_iap = if (appInfo.offersIAP) 1 else 0,
                            in_app_product_price = appInfo.inAppProductPrice,
                            developer_id = developer.id.toLong(),
                            privacy_policy = appInfo.privacyPolicy,
                            genre = appInfo.genre,
                            genre_id = appInfo.genreId,
                            icon = appInfo.icon,
                            header_image = appInfo.headerImage,
                            screenshots = appInfo.screenshots?.joinToString(","),
                            video = appInfo.video,
                            video_image = appInfo.videoImage,
                            content_rating = appInfo.contentRating,
                            content_rating_description = appInfo.contentRatingDescription,
                            ad_supported = if (appInfo.adSupported) 1 else 0,
                            contains_ads = if (appInfo.containsAds) 1 else 0,
                            released = appInfo.released,
                            updated = appInfo.updated?.toLong(),
                            version = appInfo.version,
                            comments = null,
                            url = appInfo.url,
                            app_category_id = category.id
                        )
                    }
                    count++
                } else {
                    logger.w { "⚠️ Failed to fetch info for $packageName" }
                }
            }

        logger.i { "✅ Processed $count applications using SQLDelight tables" }
    }
}

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import com.kdroid.gplayscrapper.core.model.GooglePlayApplicationInfo
import com.kdroid.gplayscrapper.services.getGooglePlayApplicationInfo
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.downloader.DatabaseDownloader
import io.github.kdroidfilter.database.store.App_categoriesQueries
import io.github.kdroidfilter.database.store.ApplicationsQueries
import io.github.kdroidfilter.database.store.Database
import io.github.kdroidfilter.database.store.DevelopersQueries
import io.github.kdroidfilter.database.store.VersionQueries
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SqliteStoreBuilder {
    private val logger = Logger.withTag("SqliteStoreBuilder")

    fun buildDatabase(appPoliciesDir: Path, outputDbPath: Path, language: String = "en", country: String = "us") {
        // Get release name from environment variable or generate timestamp
        val releaseName = System.getenv("RELEASE_NAME") ?: LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
        logger.i { "🏷️ Using release name: $releaseName for language: $language" }

        // Ensure the directory exists
        Files.createDirectories(outputDbPath.parent)

        // Create a SqlDriver for the database
        val driver = JdbcSqliteDriver("jdbc:sqlite:${outputDbPath.toAbsolutePath()}")

        // Create the database schema
        Database.Schema.create(driver)

        // Insert categories
        insertCategories(outputDbPath)

        // Insert version information
        insertVersion(releaseName, outputDbPath)

        // Insert packages
        upsertPackages(appPoliciesDir, outputDbPath, language, country)

        logger.i { "✅ SQLite database created at $outputDbPath" }
    }


    private fun insertCategories(outputDbPath: Path) {
        val appCategoriesQueries = App_categoriesQueries(createSqlDriver(outputDbPath))
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

    private fun insertVersion(releaseName: String, outputDbPath: Path) {
        val versionQueries = VersionQueries(createSqlDriver(outputDbPath))

        // Clear existing entries
        versionQueries.clearVersions()

        // Insert new release name
        versionQueries.insertVersion(releaseName)

        logger.i { "✅ Inserted version info: $releaseName" }
    }

    private fun upsertPackages(dir: Path, outputDbPath: Path, language: String = "en", country: String = "us") {
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
                val appCategoriesQueries = App_categoriesQueries(createSqlDriver(outputDbPath))
                val categoryId = getOrCreateCategory(categoryName, appCategoriesQueries)

                // Fetch application info from Google Play with specified language and country
                val appInfo = runBlocking {
                    existingApps.getOrPut(packageName) {
                        runCatching {
                            getGooglePlayApplicationInfo(packageName, language, country)
                        }.getOrNull()
                    }
                }

                if (appInfo != null) {
                    // Create queries instances
                    val developersQueries = DevelopersQueries(createSqlDriver(outputDbPath))
                    val applicationsQueries = ApplicationsQueries(createSqlDriver(outputDbPath))

                    // Insert application
                    insertApplicationFromAppInfo(
                        appInfo = appInfo,
                        packageName = packageName,
                        applicationsQueries = applicationsQueries,
                        developersQueries = developersQueries,
                        categoryId = categoryId
                    )

                    count++
                } else {
                    logger.w { "⚠️ Failed to fetch info for $packageName" }
                }
            }

        logger.i { "✅ Processed $count applications using SQLDelight tables" }
    }

    /**
     * Downloads the latest databases for all three languages (en, fr, he) from GitHub releases.
     * @param baseDbPath The base path used to determine the output directory
     * @return Map of language codes to download success status
     */
    private fun downloadLatestDatabases(baseDbPath: Path): Map<String, Boolean> {
        val outputDir = baseDbPath.parent.toString()

        // Use the DatabaseDownloader to download the latest databases
        val databaseDownloader = DatabaseDownloader()
        return databaseDownloader.downloadLatestStoreDatabases(outputDir)
    }

    /**
     * Updates packages for existing databases, only adding packages that don't exist in the table
     */
    private fun updatePackagesIfNotExists(dir: Path, outputDbPath: Path, language: String = "en", country: String = "us") {
        val existingApps = mutableMapOf<String, GooglePlayApplicationInfo?>()
        var processedCount = 0
        var addedCount = 0

        Files.walk(dir)
            .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
            .forEach { file ->
                val packageName = file.fileName.toString().substringBeforeLast(".")
                processedCount++

                // Check if the package already exists in the database
                val applicationsQueries = ApplicationsQueries(createSqlDriver(outputDbPath))
                val existingApp = applicationsQueries
                    .getApplicationByAppId(packageName)
                    .executeAsOneOrNull()

                // Only process if the package doesn't exist
                if (existingApp == null) {
                    val categoryName = file.parent.fileName.toString()
                        .uppercase().replace('-', '_')

                    // Get or create the category
                    val appCategoriesQueries = App_categoriesQueries(createSqlDriver(outputDbPath))
                    val categoryId = getOrCreateCategory(categoryName, appCategoriesQueries)

                    // Fetch application info from Google Play with specified language and country
                    val appInfo = runBlocking {
                        existingApps.getOrPut(packageName) {
                            runCatching {
                                getGooglePlayApplicationInfo(packageName, language, country)
                            }.getOrNull()
                        }
                    }

                    if (appInfo != null) {
                        // Create queries instances
                        val developersQueries = DevelopersQueries(createSqlDriver(outputDbPath))

                        // Insert application
                        insertApplicationFromAppInfo(
                            appInfo = appInfo,
                            packageName = packageName,
                            applicationsQueries = applicationsQueries,
                            developersQueries = developersQueries,
                            categoryId = categoryId
                        )

                        addedCount++
                        logger.d { "➕ Added new package: $packageName" }
                    } else {
                        logger.w { "⚠️ Failed to fetch info for new package $packageName" }
                    }
                } else {
                    logger.d { "⏭️ Package $packageName already exists, skipping" }
                }
            }

        logger.i { "✅ Processed $processedCount packages, added $addedCount new packages for language $language" }
    }

    /**
     * Gets or creates a category in the database
     * Returns the category ID
     */
    private fun getOrCreateCategory(
        categoryName: String,
        appCategoriesQueries: App_categoriesQueries
    ): Long {
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

        return category.id
    }

    /**
     * Inserts or updates an application in the database from GooglePlayApplicationInfo
     * Returns the inserted/updated application ID
     */
    private fun insertApplicationFromAppInfo(
        appInfo: GooglePlayApplicationInfo,
        packageName: String,
        applicationsQueries: ApplicationsQueries,
        developersQueries: DevelopersQueries,
        categoryId: Long
    ): Long {
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
                min_installs = appInfo.minInstalls,
                real_installs = appInfo.realInstalls,
                score = appInfo.score,
                ratings = appInfo.ratings,
                reviews = appInfo.reviews,
                histogram = appInfo.histogram.toString(),
                price = appInfo.price,
                free = if (appInfo.free) 1 else 0,
                currency = appInfo.currency,
                sale = if (appInfo.sale) 1 else 0,
                sale_time = null,
                original_price = appInfo.originalPrice,
                sale_text = appInfo.saleText,
                offers_iap = if (appInfo.offersIAP) 1 else 0,
                in_app_product_price = appInfo.inAppProductPrice,
                developer_id = developer.id,
                privacy_policy = appInfo.privacyPolicy,
                genre = appInfo.genre,
                genre_id = appInfo.genreId,
                icon = appInfo.icon,
                header_image = appInfo.headerImage,
                screenshots = appInfo.screenshots.joinToString(","),
                video = appInfo.video,
                video_image = appInfo.videoImage,
                content_rating = appInfo.contentRating,
                content_rating_description = appInfo.contentRatingDescription,
                ad_supported = if (appInfo.adSupported) 1 else 0,
                contains_ads = if (appInfo.containsAds) 1 else 0,
                released = appInfo.released,
                updated = appInfo.updated,
                version = appInfo.version,
                comments = null,
                url = appInfo.url,
                app_category_id = categoryId
            )

            // Return the ID of the newly inserted application
            return applicationsQueries.getApplicationByAppId(packageName).executeAsOne().id
        } else {
            // Return the ID of the existing application
            return existingApp.id
        }
    }

    /**
     * Builds or updates databases for all three languages, downloading existing ones first
     */
    fun buildOrUpdateMultiLanguageDatabases(appPoliciesDir: Path, baseDbPath: Path) {
        val outputDir = baseDbPath.parent
        val languages = mapOf(
            "en" to "us",
            "fr" to "fr", 
            "he" to "il"
        )

        logger.i { "🔄 Starting multi-language database build/update process..." }

        // First, try to download existing databases
        val downloadResults = downloadLatestDatabases(baseDbPath)

        languages.forEach { (language, country) ->
            val dbPath = outputDir.resolve("store-database-$language.db")

            if (downloadResults[language] == true) {
                logger.i { "📄 Using downloaded database for $language, updating with new packages only..." }
                // Update the downloaded database with only new packages
                updatePackagesIfNotExists(appPoliciesDir, dbPath, language, country)
            } else {
                logger.i { "🏗️ Building new database for $language from scratch..." }
                // Build database from scratch
                buildDatabase(appPoliciesDir, dbPath, language, country)
            }
        }

        logger.i { "✅ Completed multi-language database build/update process" }
    }
}

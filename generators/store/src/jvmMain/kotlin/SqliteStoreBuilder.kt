import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger

import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.core.policies.AppPolicy
import io.github.kdroidfilter.database.downloader.DatabaseDownloader
import io.github.kdroidfilter.database.store.*
import io.github.kdroidfilter.storekit.gplay.core.model.GooglePlayApplicationInfo
import io.github.kdroidfilter.storekit.gplay.scrapper.services.getGooglePlayApplicationInfo
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SqliteStoreBuilder {
    private val logger = Logger.withTag("SqliteStoreBuilder")

    // Extension function to convert Boolean to Long (1 for true, 0 for false)
    private fun Boolean.toSqliteInt(): Long = if (this) 1L else 0L

    // Extension function to convert nullable Boolean to Long (1 for true, 0 for false or null)
    private fun Boolean?.toSqliteInt(): Long = if (this == true) 1L else 0L

    fun buildDatabase(
        appPoliciesDir: Path,
        outputDbPath: Path,
        language: String = "en",
        country: String = "us"
    ) {
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

    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
    }

    private fun loadPolicies(appPoliciesDir: Path): Map<String, AppPolicy> {
        val policies = mutableMapOf<String, AppPolicy>()

        Files.walk(appPoliciesDir)
            .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
            .forEach { file ->
                try {
                    val content = Files.readString(file)
                    val policy = json.decodeFromString(AppPolicy.serializer(), content)
                    policies[policy.packageName] = policy
                    logger.d { "Loaded policy for ${policy.packageName}: isRecommendedInStore=${policy.isRecommendedInStore}" }
                } catch (e: Exception) {
                    logger.w { "Failed to load policy from $file: ${e.message}" }
                }
            }

        logger.i { "✅ Loaded ${policies.size} policies" }
        return policies
    }

    private fun upsertPackages(dir: Path, outputDbPath: Path, language: String = "en", country: String = "us") {
        // Get existing applications to avoid re-fetching
        val existingApps = mutableMapOf<String, GooglePlayApplicationInfo?>()

        // Load app policies to get isRecommendedInStore values
        val policies = loadPolicies(dir)

        var count = 0
        Files.walk(dir)
            .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
            .forEach { file ->
                // Get policy for this file to extract packageName and category
                val content = Files.readString(file)
                val policy = json.decodeFromString(AppPolicy.serializer(), content)
                val packageName = policy.packageName
                val categoryName = policy.category.name

                // Get or create the category
                val appCategoriesQueries = App_categoriesQueries(createSqlDriver(outputDbPath))
                val categoryId = getOrCreateCategory(categoryName, appCategoriesQueries)

                // Fetch application info from Google Play with specified language and country
                val appInfo = existingApps[packageName] ?: try {
                    runBlocking {
                        getGooglePlayApplicationInfo(packageName, language, country).also { 
                            existingApps[packageName] = it 
                        }
                    }
                } catch (e: Exception) {
                    logger.w { "⚠️ Failed to fetch info for $packageName: ${e.message}" }
                    null
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
                        categoryId = categoryId,
                        policies = policies
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
    private suspend fun downloadLatestDatabases(baseDbPath: Path): Map<String, Boolean> {
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

        // Load app policies to get isRecommendedInStore values
        val policies = loadPolicies(dir)

        Files.walk(dir)
            .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
            .forEach { file ->
                // Get policy for this file to extract packageName and category
                val content = Files.readString(file)
                val policy = json.decodeFromString(AppPolicy.serializer(), content)
                val packageName = policy.packageName
                processedCount++

                // Check if the package already exists in the database
                val applicationsQueries = ApplicationsQueries(createSqlDriver(outputDbPath))
                val existingApp = applicationsQueries
                    .getApplicationByAppId(packageName)
                    .executeAsOneOrNull()

                // Only process if the package doesn't exist
                if (existingApp == null) {
                    val categoryName = policy.category.name

                    // Get or create the category
                    val appCategoriesQueries = App_categoriesQueries(createSqlDriver(outputDbPath))
                    val categoryId = getOrCreateCategory(categoryName, appCategoriesQueries)

                    // Fetch application info from Google Play with specified language and country
                    val appInfo = existingApps.getOrPut(packageName) {
                        runBlocking {
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
                            categoryId = categoryId,
                            policies = policies
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
        categoryId: Long,
        policies: Map<String, AppPolicy> = emptyMap()
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
                free = appInfo.free.toSqliteInt(),
                currency = appInfo.currency,
                sale = appInfo.sale.toSqliteInt(),
                sale_time = null,
                original_price = appInfo.originalPrice,
                sale_text = appInfo.saleText,
                offers_iap = appInfo.offersIAP.toSqliteInt(),
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
                ad_supported = appInfo.adSupported.toSqliteInt(),
                contains_ads = appInfo.containsAds.toSqliteInt(),
                is_recommended_in_store = (policies[packageName]?.isRecommendedInStore ?: false).toSqliteInt(),
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
     * @param appPoliciesDir Directory containing app policies
     * @param baseDbPath Base path used to determine the output directory
     * @param forceFromScratch If true, forces building databases from scratch instead of downloading existing ones
     */
    suspend fun buildOrUpdateMultiLanguageDatabases(appPoliciesDir: Path, baseDbPath: Path, forceFromScratch: Boolean = false) {
        val outputDir = baseDbPath.parent
        val languages = mapOf(
            "en" to "us",
            "fr" to "fr", 
            "he" to "il"
        )

        logger.i { "🔄 Starting multi-language database build/update process..." }

        // Determine if we should try to download existing databases or force build from scratch
        val downloadResults = if (forceFromScratch) {
            logger.i { "🔨 Force building from scratch enabled, skipping download of existing databases" }
            emptyMap()
        } else {
            // Try to download existing databases
            downloadLatestDatabases(baseDbPath)
        }

        languages.forEach { (language, country) ->
            val dbPath = outputDir.resolve("store-database-$language.db")

            if (!forceFromScratch && downloadResults[language] == true) {
                logger.i { "📄 Using downloaded database for $language, updating with new packages only..." }
                // Get release name from environment variable or generate timestamp
                val releaseName = System.getenv("RELEASE_NAME") ?: LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
                logger.i { "🏷️ Updating version to: $releaseName for language: $language" }

                // Update version even for downloaded databases
                insertVersion(releaseName, dbPath)

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

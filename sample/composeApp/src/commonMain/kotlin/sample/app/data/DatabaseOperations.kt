package sample.app.data

import com.kdroid.gplayscrapper.core.model.GooglePlayApplicationInfo
import io.github.kdroidfilter.database.store.Database
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.localization.LocalizedAppCategory
import io.github.kdroidfilter.database.store.App_categories
import io.github.kdroidfilter.database.store.Applications
import io.github.kdroidfilter.database.store.Developers
import sample.app.models.AppInfoWithExtras
import sample.app.getDeviceLanguage

// Function to create GooglePlayApplicationInfo from database data
fun createAppInfoFromDatabaseData(
    app: Applications,
    developer: Developers,
    category: App_categories
): GooglePlayApplicationInfo {
    return GooglePlayApplicationInfo(
        appId = app.app_id,
        title = app.title,
        description = app.description ?: "",
        descriptionHTML = app.description_html ?: "",
        summary = app.summary ?: "",
        installs = app.installs ?: "",
        minInstalls = app.min_installs ?: 0L,
        realInstalls = app.real_installs ?: 0L,
        score = app.score ?: 0.0,
        ratings = app.ratings ?: 0L,
        reviews = app.reviews ?: 0L,
        histogram = app.histogram?.removeSurrounding("[", "]")?.split(", ")?.map { it.toLongOrNull() ?: 0L } ?: emptyList(),
        price = app.price ?: 0.0,
        free = app.free == 1L,
        currency = app.currency ?: "",
        sale = app.sale == 1L,
        saleTime = app.sale_time,
        originalPrice = app.original_price,
        saleText = app.sale_text,
        offersIAP = app.offers_iap == 1L,
        inAppProductPrice = app.in_app_product_price ?: "",
        developer = developer.name,
        developerId = developer.developer_id,
        developerEmail = developer.email ?: "",
        developerWebsite = developer.website ?: "",
        developerAddress = developer.address ?: "",
        privacyPolicy = app.privacy_policy ?: "",
        genre = app.genre ?: "",
        genreId = app.genre_id ?: "",
        icon = app.icon ?: "",
        headerImage = app.header_image ?: "",
        screenshots = app.screenshots?.split(",") ?: emptyList(),
        video = app.video ?: "",
        videoImage = app.video_image ?: "",
        contentRating = app.content_rating ?: "",
        contentRatingDescription = app.content_rating_description ?: "",
        adSupported = app.ad_supported == 1L,
        containsAds = app.contains_ads == 1L,
        released = app.released ?: "",
        updated = app.updated ?: 0L,
        version = app.version ?: "Varies with device",
        url = app.url ?: ""
    )
}

// Function to create AppInfoWithExtras from database data
fun createAppInfoWithExtras(
    app: Applications,
    developer: Developers,
    category: App_categories
): AppInfoWithExtras {
    val appInfo = createAppInfoFromDatabaseData(app, developer, category)

    val categoryEnum = AppCategory.valueOf(category.category_name)
    val localizedCategoryName = LocalizedAppCategory.getLocalizedName(categoryEnum, getDeviceLanguage())

    return AppInfoWithExtras(
        id = app.id,
        categoryLocalizedName = localizedCategoryName,
        app = appInfo
    )
}

// Function to load applications from the database with SQLDelight
fun loadApplicationsFromDatabase(database: Database): List<AppInfoWithExtras> {
    // Directly use the query objects provided by the database
    val applicationsQueries = database.applicationsQueries
    val developersQueries = database.developersQueries
    val categoriesQueries = database.app_categoriesQueries

    return applicationsQueries.getAllApplications().executeAsList().map { app ->
        val developer = developersQueries.getDeveloperById(app.developer_id).executeAsOne()
        val category = categoriesQueries.getCategoryById(app.app_category_id).executeAsOne()

        createAppInfoWithExtras(app, developer, category)
    }
}

// Function to search applications in the database
fun searchApplicationsInDatabase(database: Database, query: String): List<AppInfoWithExtras> {
    // Directly use the query objects provided by the database
    val applicationsQueries = database.applicationsQueries
    val developersQueries = database.developersQueries
    val categoriesQueries = database.app_categoriesQueries

    return applicationsQueries.searchApplications(query, query).executeAsList().map { app ->
        val developer = developersQueries.getDeveloperById(app.developer_id).executeAsOne()
        val category = categoriesQueries.getCategoryById(app.app_category_id).executeAsOne()

        createAppInfoWithExtras(app, developer, category)
    }
}

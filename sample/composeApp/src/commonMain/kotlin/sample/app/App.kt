package sample.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kdroid.gplayscrapper.core.model.GooglePlayApplicationInfo
import io.github.kdroidfilter.database.store.Database
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.localization.LocalizedAppCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path

expect fun createSqlDriver(): SqlDriver
expect fun getDatabasePath(): Path
expect fun getDeviceLanguage(): String

// Extended class to add additional information to the GooglePlayApplicationInfo model
data class AppInfoWithExtras(
    val id: Long,
    val categoryRawName: String,
    val categoryLocalizedName: String,
    val app: GooglePlayApplicationInfo
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val database = remember { Database(createSqlDriver()) }
    var isLoading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var applications by remember { mutableStateOf<List<AppInfoWithExtras>>(emptyList()) }
    var selectedAppDetails by remember { mutableStateOf<AppInfoWithExtras?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val logger = remember { Logger.withTag("AppList") }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val apps = loadApplicationsFromDatabase(database)
                applications = apps
            }
        } catch (e: Exception) {
            logger.e { "Loading error: ${e.message}" }
            message = "Error: ${e.message}\nMake sure you have run the 'runStoreExtractor' task to generate the database."
        } finally {
            isLoading = false
        }
    }

    // Display the snackbar when the message changes
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(message = it)
            message = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "KDroid Database",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Language: ${getDeviceLanguage()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "DB: ${getDatabasePath().fileName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp
                    )
                }
                IconButton(
                    onClick = {
                        if (!isLoading && !isRefreshing) {
                            isRefreshing = true
                            MainScope().launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        val apps = loadApplicationsFromDatabase(database)
                                        applications = apps
                                    }
                                    message = "Database refreshed successfully!"
                                } catch (e: Exception) {
                                    logger.e { "Refresh error: ${e.message}" }
                                    message = "Refresh error: ${e.message}"
                                } finally {
                                    isRefreshing = false
                                }
                            }
                        }
                    },
                    enabled = !isLoading && !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, "Refresh database")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(applications) { app ->
                        AppRow(app) { selectedAppDetails = app }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    selectedAppDetails?.let { info ->
        AppDetailDialog(info) { selectedAppDetails = null }
    }
}

// Function to load applications from the database with SQLDelight
private fun loadApplicationsFromDatabase(database: Database): List<AppInfoWithExtras> {
    // Directly use the query objects provided by the database
    val applicationsQueries = database.applicationsQueries
    val developersQueries = database.developersQueries
    val categoriesQueries = database.app_categoriesQueries

    return applicationsQueries.getAllApplications().executeAsList().map { app ->
        val developer = developersQueries.getDeveloperById(app.developer_id).executeAsOne()
        val category = categoriesQueries.getCategoryById(app.app_category_id).executeAsOne()

        // Create a GooglePlayApplicationInfo from the database data
        val appInfo = GooglePlayApplicationInfo(
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

        // Try to convert the category name to an AppCategory enum
        val categoryEnum = try {
            AppCategory.valueOf(category.category_name)
        } catch (e: IllegalArgumentException) {
            // If the category is not found in the enum, use null
            null
        }

        // Get the localized name of the category if possible
        val localizedCategoryName = if (categoryEnum != null) {
            LocalizedAppCategory.getLocalizedName(categoryEnum, getDeviceLanguage())
        } else {
            category.category_name // Fallback to raw name if not found in enum
        }

        AppInfoWithExtras(
            id = app.id,
            categoryRawName = category.category_name,
            categoryLocalizedName = localizedCategoryName,
            app = appInfo
        )
    }
}

@Composable
fun AppRow(appWithExtras: AppInfoWithExtras, onClick: () -> Unit) {
    val app = appWithExtras.app
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = createImageRequest(app.icon),
            contentDescription = "App icon",
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = app.title,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${app.score} ★ (${app.ratings} ratings)",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp
            )
            Text(
                text = "Category: ${appWithExtras.categoryLocalizedName}",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun AppDetailDialog(appWithExtras: AppInfoWithExtras, onDismiss: () -> Unit) {
    val info = appWithExtras.app
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = createImageRequest(info.icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = info.title, 
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header image
                info.headerImage?.takeIf { it.isNotBlank() }?.let { url ->
                    AsyncImage(
                        model = createImageRequest(url),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Screenshots carousel
                info.screenshots.takeIf { it.isNotEmpty() }?.let { list ->
                    Text(
                        text = "Screenshots:", 
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        list.forEach { shot ->
                            AsyncImage(
                                model = createImageRequest(shot),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(120.dp, 200.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )
                        }
                    }
                }

                // Application information
                Text(text = "Rating: ${info.score} ★ (${info.ratings} ratings)")
                Text(text = "Installations: ${info.installs.takeIf { it.isNotBlank() } ?: "Not specified"}")
                Text(
                    text = "Price: ${if (info.free) "Free" else "${info.price} ${info.currency}"}"
                )
                Text(text = "Category: ${appWithExtras.categoryLocalizedName}")
                Spacer(modifier = Modifier.height(8.dp))
                if (info.description.isNotBlank()) {
                    Text(text = info.description)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(text = "Version: ${info.version}")
                if (info.updated > 0) {
                    Text(text = "Updated: ${info.updated}")
                }
                Text(text = "Developer: ${info.developer}")
                if (info.developerWebsite.isNotBlank()) {
                    Text(text = "Website: ${info.developerWebsite}")
                }
                if (info.genre.isNotBlank()) {
                    Text(text = "Genre: ${info.genre}")
                }
                if (info.contentRating.isNotBlank()) {
                    Text(text = "Content Rating: ${info.contentRating}")
                }
            }
        },
        confirmButton = {
            Button(onClick = { /* install */ }) {
                Text(text = "Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        }
    )
}

@Composable
private fun createImageRequest(data: Any?): ImageRequest {
    return ImageRequest.Builder(LocalPlatformContext.current)
        .data(data)
        .crossfade(true)
        .build()
}

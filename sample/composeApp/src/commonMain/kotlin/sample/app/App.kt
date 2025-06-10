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
import io.github.kdroidfilter.database.sample.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

expect fun createSqlDriver(): SqlDriver
expect fun getDatabasePath(): String
expect fun getDeviceLanguage(): String

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val database = remember { Database(createSqlDriver()) }
    var isLoading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var packages by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }
    var selectedPkgInfo by remember { mutableStateOf<GooglePlayApplicationInfo?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val logger = remember { Logger.withTag("AppList") }

    LaunchedEffect(Unit) {
        try {
            // Download the latest database if it doesn't exist
            val dbDownloaded = DatabaseManager.downloadLatestDatabaseIfNotExists()
            if (!dbDownloaded) {
                logger.w { "⚠️ Could not download the database, will try to use existing one if available" }
            }

            withContext(Dispatchers.IO) {
                val pkgs = database.storeQueries.getAllPackages()
                    .executeAsList()
                    .map {
                        PackageInfo(
                            it.package_name,
                            it.store_info_en,
                            it.store_info_fr,
                            it.store_info_he
                        )
                    }
                packages = pkgs
            }
        } catch (e: Exception) {
            logger.e { "Loading error: ${e.message}" }
            message = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Show snackbar when message changes
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(message = it)
            // Clear the message after showing it
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
                Text(
                    text = "KDroid Database",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    onClick = {
                        if (!isLoading && !isRefreshing) {
                            isRefreshing = true
                            // Launch a coroutine to refresh the database
                            MainScope().launch {
                                try {
                                    val refreshed = DatabaseManager.refreshDatabase()
                                    if (refreshed) {
                                        message = "Database refreshed successfully!"
                                        // Reload the database
                                        val pkgs = database.storeQueries.getAllPackages()
                                            .executeAsList()
                                            .map {
                                                PackageInfo(
                                                    it.package_name,
                                                    it.store_info_en,
                                                    it.store_info_fr,
                                                    it.store_info_he
                                                )
                                            }
                                        packages = pkgs
                                    } else {
                                        message = "Failed to refresh database."
                                    }
                                } catch (e: Exception) {
                                    logger.e { "Refresh error: ${e.message}" }
                                    message = "Error refreshing: ${e.message}"
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
                    items(packages) { pkg ->
                        AppRow(pkg) { selectedPkgInfo = loadPkgDetails(pkg) }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    selectedPkgInfo?.let { info ->
        AppDetailDialog(info) { selectedPkgInfo = null }
    }
}

@Composable
fun AppRow(pkg: PackageInfo, onClick: () -> Unit) {
    val appDetails = remember { loadPkgDetails(pkg) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = createImageRequest(appDetails?.icon),
            contentDescription = "App icon",
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = appDetails?.title ?: pkg.packageName,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${appDetails?.score ?: 0.0} ★",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun AppDetailDialog(info: GooglePlayApplicationInfo, onDismiss: () -> Unit) {
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
                // Header Image
                info.headerImage.takeIf { it.isNotBlank() }?.let { url ->
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

                // Screenshots Carousel
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

                // App Info
                Text(text = "Rating: ${info.score} ★ (${info.ratings} ratings)")
                Text(text = "Installs: ${info.installs}")
                Text(
                    text = "Price: ${if (info.free) "Free" else "${info.price} ${info.currency}"}"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = info.description)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Version: ${info.version}")
                Text(text = "Updated: ${info.updated}")
                Text(text = "Developer: ${info.developer}")
                Text(text = "Email: ${info.developerEmail}")
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

private fun loadPkgDetails(pkg: PackageInfo): GooglePlayApplicationInfo? {
    val lang = getDeviceLanguage()
    val rawJson = when (lang) {
        "fr" -> pkg.storeInfoFr ?: pkg.storeInfoEn
        "he" -> pkg.storeInfoHe ?: pkg.storeInfoEn
        else -> pkg.storeInfoEn
    } ?: return null

    val json = Json { ignoreUnknownKeys = true }
    return try {
        json.decodeFromString(GooglePlayApplicationInfo.serializer(), rawJson)
    } catch (_: Exception) {
        null
    }
}

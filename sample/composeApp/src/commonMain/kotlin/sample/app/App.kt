package sample.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import io.github.kdroidfilter.database.store.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import io.github.kdroidfilter.database.dao.ApplicationsDao
import io.github.kdroidfilter.database.dao.AppInfoWithExtras
import io.github.kdroidfilter.database.downloader.DatabaseDownloader
import sample.app.ui.AppDetailDialog
import sample.app.ui.AppRow
import sample.app.ui.SearchScreen

expect fun createSqlDriver(): SqlDriver
expect fun getDatabasePath(): Path
expect fun getDeviceLanguage(): String

enum class Screen {
    Home,
    Search,
    Recommended
}

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

    // Navigation state
    var currentScreen by remember { mutableStateOf(Screen.Home) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AppInfoWithExtras>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Recommended apps state
    var recommendedApps by remember { mutableStateOf<List<AppInfoWithExtras>>(emptyList()) }
    var isLoadingRecommended by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val apps = ApplicationsDao.loadApplicationsFromDatabase(
                    database = database,
                    deviceLanguage = getDeviceLanguage(),
                    creator = { id, categoryLocalizedName, appInfo ->
                        AppInfoWithExtras(
                            id = id,
                            categoryLocalizedName = categoryLocalizedName,
                            app = appInfo
                        )
                    }
                )
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                                        // Check if database version is up to date
                                        val isUpToDate = sample.app.utils.isDatabaseVersionUpToDate(database)

                                        // If not up to date, download the new version
                                        if (!isUpToDate) {
                                            logger.i { "Database is not up to date. Downloading new version..." }
                                            val downloader = DatabaseDownloader()
                                            val success = downloader.downloadLatestStoreDatabaseForLanguage(
                                                getDatabasePath().parent.toString(), 
                                                getDeviceLanguage()
                                            )

                                            if (success) {
                                                message = "Database updated to the latest version!"
                                            } else {
                                                message = "Failed to download the latest database version."
                                                isRefreshing = false
                                                return@withContext
                                            }
                                        } else {
                                            logger.i { "Database is already up to date." }
                                        }

                                        // Load applications from the database
                                        val apps = ApplicationsDao.loadApplicationsFromDatabase(
                                            database = database,
                                            deviceLanguage = getDeviceLanguage(),
                                            creator = { id, categoryLocalizedName, appInfo ->
                                                AppInfoWithExtras(
                                                    id = id,
                                                    categoryLocalizedName = categoryLocalizedName,
                                                    app = appInfo
                                                )
                                            }
                                        )
                                        applications = apps
                                    }
                                    if (message == null) {
                                        message = "Database refreshed successfully!"
                                    }
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
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, "Refresh database")
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentScreen == Screen.Home,
                    onClick = { currentScreen = Screen.Home }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = "Recommended") },
                    label = { Text("Recommended") },
                    selected = currentScreen == Screen.Recommended,
                    onClick = { currentScreen = Screen.Recommended }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") },
                    selected = currentScreen == Screen.Search,
                    onClick = { currentScreen = Screen.Search }
                )
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
                when (currentScreen) {
                    Screen.Home -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(applications) { app ->
                                AppRow(app, database) { selectedAppDetails = app }
                                HorizontalDivider()
                            }
                        }
                    }
                    Screen.Search -> {
                        SearchScreen(
                            database = database,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { newQuery ->
                                searchQuery = newQuery
                                if (newQuery.isNotEmpty()) {
                                    isSearching = true
                                    MainScope().launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                val results = ApplicationsDao.searchApplicationsInDatabase(
                                                    database = database,
                                                    query = newQuery,
                                                    deviceLanguage = getDeviceLanguage(),
                                                    creator = { id, categoryLocalizedName, appInfo ->
                                                        AppInfoWithExtras(
                                                            id = id,
                                                            categoryLocalizedName = categoryLocalizedName,
                                                            app = appInfo
                                                        )
                                                    }
                                                )
                                                searchResults = results
                                            }
                                        } catch (e: Exception) {
                                            logger.e { "Search error: ${e.message}" }
                                            message = "Search error: ${e.message}"
                                        } finally {
                                            isSearching = false
                                        }
                                    }
                                } else {
                                    searchResults = emptyList()
                                }
                            },
                            searchResults = searchResults,
                            isSearching = isSearching,
                            onAppClick = { app -> selectedAppDetails = app }
                        )
                    }
                    Screen.Recommended -> {
                        // Load recommended apps when this screen is first shown
                        LaunchedEffect(Unit) {
                            if (recommendedApps.isEmpty() && !isLoadingRecommended) {
                                isLoadingRecommended = true
                                try {
                                    withContext(Dispatchers.IO) {
                                        val apps = ApplicationsDao.getRecommendedApplications(
                                            database = database,
                                            deviceLanguage = getDeviceLanguage(),
                                            creator = { id, categoryLocalizedName, appInfo ->
                                                AppInfoWithExtras(
                                                    id = id,
                                                    categoryLocalizedName = categoryLocalizedName,
                                                    app = appInfo
                                                )
                                            }
                                        )
                                        recommendedApps = apps
                                    }
                                } catch (e: Exception) {
                                    logger.e { "Loading recommended apps error: ${e.message}" }
                                    message = "Error loading recommended apps: ${e.message}"
                                } finally {
                                    isLoadingRecommended = false
                                }
                            }
                        }

                        if (isLoadingRecommended) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(recommendedApps) { app ->
                                    AppRow(app, database) { selectedAppDetails = app }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedAppDetails?.let { info ->
        AppDetailDialog(info) { selectedAppDetails = null }
    }
}

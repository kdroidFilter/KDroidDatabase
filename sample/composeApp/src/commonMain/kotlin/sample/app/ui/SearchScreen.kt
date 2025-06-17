package sample.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.database.store.Database
import io.github.kdroidfilter.database.dao.AppInfoWithExtras

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    database: Database,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<AppInfoWithExtras>,
    isSearching: Boolean,
    onAppClick: (AppInfoWithExtras) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SearchBar(
            inputField = {
                androidx.compose.material3.SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onSearch = { /* Already handled in onQueryChange */ },
                    expanded = false,
                    onExpandedChange = { /* Not needed for this implementation */ },
                    placeholder = { Text("Search applications") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    enabled = true
                )
            },
            expanded = false,
            onExpandedChange = { /* Not needed for this implementation */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Search suggestions would go here if needed
        }

        if (isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No results found for '$searchQuery'")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults) { app ->
                    AppRow(app, database) { onAppClick(app) }
                    HorizontalDivider()
                }
            }
        }
    }
}

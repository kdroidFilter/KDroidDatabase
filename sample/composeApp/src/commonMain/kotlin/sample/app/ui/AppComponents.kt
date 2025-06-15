package sample.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import sample.app.models.AppInfoWithExtras
import sample.app.utils.createImageRequest

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
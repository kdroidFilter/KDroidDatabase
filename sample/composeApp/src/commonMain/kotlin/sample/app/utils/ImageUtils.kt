package sample.app.utils

import androidx.compose.runtime.Composable
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun createImageRequest(data: Any?): ImageRequest {
    return ImageRequest.Builder(LocalPlatformContext.current)
        .data(data)
        .crossfade(true)
        .build()
}
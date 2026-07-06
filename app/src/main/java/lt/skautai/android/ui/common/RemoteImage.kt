package lt.skautai.android.ui.common

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import lt.skautai.android.util.Constants
import lt.skautai.android.util.TokenManager

@Composable
fun RemoteImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val tokenManager = remember(context) { TokenManager(context.applicationContext) }
    val token by tokenManager.token.collectAsState(initial = null)
    val tuntasId by tokenManager.activeTuntasId.collectAsState(initial = null)
    val resolvedUrl = imageUrl?.takeIf { it.isNotBlank() }?.toAbsoluteImageUrl()
    val model = remember(resolvedUrl, token, tuntasId) {
        val data = resolvedUrl?.let { url ->
            if (url.startsWith("content://")) Uri.parse(url) else url
        }
        ImageRequest.Builder(context)
            .data(data)
            .crossfade(true)
            .apply {
                if (
                    resolvedUrl?.startsWith(Constants.BASE_URL.trimEnd('/') + "/uploads/") == true &&
                    !token.isNullOrBlank()
                ) {
                    setHeader("Authorization", "Bearer $token")
                    tuntasId?.takeIf(String::isNotBlank)?.let { setHeader("X-Tuntas-Id", it) }
                }
            }
            .build()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            loading = {
                CircularProgressIndicator()
            },
            error = {
                Icon(
                    imageVector = Icons.Default.ImageIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

private fun String.toAbsoluteImageUrl(): String =
    if (startsWith("http://") || startsWith("https://")) {
        this
    } else {
        Constants.BASE_URL.trimEnd('/') + "/" + trimStart('/')
    }

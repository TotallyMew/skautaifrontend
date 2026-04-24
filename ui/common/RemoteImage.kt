package lt.skautai.android.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lt.skautai.android.util.Constants
import java.net.URL

@Composable
fun RemoteImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var bitmap by remember(imageUrl) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(imageUrl) { mutableStateOf(!imageUrl.isNullOrBlank()) }

    LaunchedEffect(imageUrl) {
        val url = imageUrl?.takeIf { it.isNotBlank() }
        if (url == null) {
            bitmap = null
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                if (url.startsWith("content://")) {
                    context.contentResolver.openInputStream(Uri.parse(url))?.use(BitmapFactory::decodeStream)
                } else {
                    URL(url.toAbsoluteImageUrl()).openStream().use(BitmapFactory::decodeStream)
                }
            }.getOrNull()
        }
        isLoading = false
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
            isLoading -> CircularProgressIndicator()
            else -> Icon(
                imageVector = Icons.Default.ImageIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun String.toAbsoluteImageUrl(): String =
    if (startsWith("http://") || startsWith("https://")) {
        this
    } else {
        Constants.BASE_URL.trimEnd('/') + "/" + trimStart('/')
    }

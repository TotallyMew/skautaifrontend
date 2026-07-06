package lt.skautai.android.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SkautaiSkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

@Composable
fun SkautaiListSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 6,
    showHeader: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showHeader) {
            SkautaiSkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    SkautaiSkeletonBlock(
                        modifier = Modifier
                            .width(92.dp)
                            .height(32.dp),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }
        repeat(rows) {
            SkautaiListSkeletonRow()
        }
    }
}

@Composable
fun SkautaiFormSkeleton(
    modifier: Modifier = Modifier,
    fields: Int = 3
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(fields) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkautaiSkeletonBlock(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                )
                SkautaiSkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SkautaiSkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun SkautaiListSkeletonRow(
    thumbnailSize: Dp = 48.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(skautaiSurfaceTone(SkautaiSurfaceRole.DenseList))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkautaiSkeletonBlock(
            modifier = Modifier.size(thumbnailSize),
            shape = CircleShape
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkautaiSkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(18.dp)
            )
            SkautaiSkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
            )
            SkautaiSkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(14.dp)
            )
        }
    }
}

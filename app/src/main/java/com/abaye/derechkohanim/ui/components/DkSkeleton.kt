package com.abaye.derechkohanim.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.abaye.derechkohanim.ui.theme.DkTheme

/** Placeholder block. The pulse duration comes from the motion tokens. */
@Composable
fun DkSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(DkTheme.radius.sm),
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = MIN_ALPHA,
        targetValue = MAX_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = DkTheme.motion.slow),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .alpha(alpha)
            .background(color = DkTheme.colors.surfaceMuted, shape = shape),
    )
}

/** The shape of a suggestion row, so the list does not jump when results arrive. */
@Composable
fun DkListItemSkeleton(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s3),
    ) {
        DkSkeleton(
            modifier = Modifier.size(DkTheme.touch.minTarget),
            shape = CircleShape,
        )
        Spacer(Modifier.width(DkTheme.spacing.s3))
        Column(modifier = Modifier.weight(1f)) {
            DkSkeleton(
                modifier = Modifier
                    .fillMaxWidth(TITLE_WIDTH_FRACTION)
                    .height(DkTheme.spacing.s4)
            )
            Spacer(Modifier.height(DkTheme.spacing.s2))
            DkSkeleton(
                modifier = Modifier
                    .fillMaxWidth(SUBTITLE_WIDTH_FRACTION)
                    .height(DkTheme.spacing.s3)
            )
        }
    }
}

private const val MIN_ALPHA = 0.4f
private const val MAX_ALPHA = 1f
private const val TITLE_WIDTH_FRACTION = 0.62f
private const val SUBTITLE_WIDTH_FRACTION = 0.38f

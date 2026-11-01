package com.abaye.derechkohanim.ui.components

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.ui.theme.DkTheme

enum class AppLoaderSize(val icon: Dp) { Small(40.dp), Medium(64.dp), Large(88.dp) }

/**
 * The loading state of the app: its icon hopping like a map pin settling, over a soft shadow
 * that shrinks and fades as the icon rises. A 1.15 s ease-in-out loop: squashed a little at
 * rest, lifted 38% of its size and slightly stretched at the top. With animations turned off
 * in the system settings the icon stands still. An optional label sits underneath.
 */
@Composable
fun AppLoader(
    modifier: Modifier = Modifier,
    size: AppLoaderSize = AppLoaderSize.Medium,
    label: String? = null,
) {
    val context = LocalContext.current
    val reducedMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    // 0 at rest, 1 at the top of the hop.
    val lift = if (reducedMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "appLoader")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = HALF_CYCLE_MS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "lift",
        )
        value
    }
    val icon = size.icon
    val description = label ?: androidx.compose.ui.res.stringResource(R.string.state_loading)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
        modifier = modifier.semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.size(width = icon, height = icon * 1.5f)) {
            // The shadow: 62% of the icon wide at rest, 55% and fainter at the top.
            Box(
                modifier = Modifier
                    .width(icon * (0.62f - 0.07f * lift))
                    .height(icon * 0.12f)
                    .graphicsLayer { alpha = 1f - 0.55f * lift }
                    .background(Color.Black.copy(alpha = 0.18f), RoundedCornerShape(percent = 50)),
            )
            Image(
                painter = painterResource(R.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = icon * 0.08f)
                    .size(icon)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        translationY = -0.38f * icon.toPx() * lift
                        // Squash at rest (1.04 x 0.96), a slight stretch at the top (0.97 x 1.03).
                        scaleX = 1.04f - 0.07f * lift
                        scaleY = 0.96f + 0.07f * lift
                    }
                    .clip(RoundedCornerShape(percent = 26)),
            )
        }
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = DkTheme.colors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A page that is still loading: the loader centred in the available space. */
@Composable
fun AppLoaderPage(modifier: Modifier = Modifier, label: String? = null, size: AppLoaderSize = AppLoaderSize.Large) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(DkTheme.spacing.s8)) {
        AppLoader(size = size, label = label)
    }
}

private const val HALF_CYCLE_MS = 575

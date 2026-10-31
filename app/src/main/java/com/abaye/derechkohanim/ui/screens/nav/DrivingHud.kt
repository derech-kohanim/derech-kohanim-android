package com.abaye.derechkohanim.ui.screens.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkNavTextStyles
import com.abaye.derechkohanim.ui.theme.DkTheme
import kotlin.math.roundToInt

/** The ring fills up to this speed (DirGo's speedometer uses the same 120 km/h scale). */
const val SPEEDOMETER_MAX_KMH = 120

/**
 * Current speed in a ring that fills with speed, beside the speed-limit sign when the limit is
 * known. Over the limit, the ring and its frame turn to the danger colour (spec 17.2: a glance,
 * no reading).
 */
@Composable
fun SpeedCluster(speedKmh: Int, limitKmh: Int?, speeding: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Speedometer(speedKmh = speedKmh, speeding = speeding)
        if (limitKmh != null) SpeedLimitSign(limitKmh = limitKmh)
    }
}

@Composable
fun Speedometer(speedKmh: Int, speeding: Boolean, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_speedometer, speedKmh)
    val accent = if (speeding) DkTheme.colors.danger else DkTheme.colors.primary
    Box(
        modifier = modifier
            .size(DkTheme.spacing.s16)
            .background(DkTheme.colors.surfaceOverlay, CircleShape)
            .border(
                width = if (speeding) DkTheme.spacing.s1 else DkTheme.spacing.s0,
                color = DkTheme.colors.danger,
                shape = CircleShape,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { (speedKmh.toFloat() / SPEEDOMETER_MAX_KMH).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxSize()
                .padding(DkTheme.spacing.s1),
            color = accent,
            trackColor = DkTheme.colors.surfaceMuted,
            strokeWidth = DkTheme.spacing.s1,
        )
        Text(
            text = speedKmh.toString(),
            style = DkNavTextStyles.stat,
            color = if (speeding) DkTheme.colors.danger else DkTheme.colors.text,
        )
    }
}

/** The round regulatory sign: a red ring around the limit, as on the road. */
@Composable
fun SpeedLimitSign(limitKmh: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_speed_limit, limitKmh)
    Box(
        modifier = modifier
            .size(DkTheme.spacing.s12)
            .background(DkTheme.colors.surface, CircleShape)
            .border(DkTheme.spacing.s1, DkTheme.colors.danger, CircleShape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = limitKmh.toString(),
            style = DkNavTextStyles.stat.copy(fontWeight = FontWeight.Bold),
            color = DkTheme.colors.text,
        )
    }
}

/** Metres per second to whole km/h, never negative. */
fun speedKmh(metersPerSecond: Double): Int = (metersPerSecond * 3.6).roundToInt().coerceAtLeast(0)

@Preview(name = "Speed, within the limit", showBackground = true, locale = "he")
@Composable
private fun SpeedClusterPreview() {
    DerechKohanimTheme { SpeedCluster(speedKmh = 72, limitKmh = 90, speeding = false) }
}

@Preview(name = "Speed, over the limit", showBackground = true, locale = "en")
@Composable
private fun SpeedClusterSpeedingPreview() {
    DerechKohanimTheme { SpeedCluster(speedKmh = 104, limitKmh = 90, speeding = true) }
}

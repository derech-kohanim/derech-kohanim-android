package com.abaye.derechkohanim.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.ui.theme.DkTheme

// Waze-style: a black bar with red text, readable at a glance and unmistakable in the corner of
// the eye while driving. Fixed colours by design - the meaning must not shift with the theme.
private val BannerBackground = Color(0xFF000000)
private val BannerText = Color(0xFFFF3B30)

/**
 * A compact status bar that surfaces the two conditions that break navigation: no internet and no
 * GPS fix. Shows one pill per active condition and nothing at all when both are fine. Reused by the
 * home screen (top) and the navigation screen (under the instruction card).
 */
@Composable
fun StatusBanner(
    offline: Boolean,
    noGps: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s1),
    ) {
        AnimatedVisibility(visible = offline) {
            StatusPill(
                icon = Icons.Filled.CloudOff,
                text = stringResource(R.string.banner_no_internet),
            )
        }
        AnimatedVisibility(visible = noGps) {
            StatusPill(
                icon = Icons.Filled.LocationOff,
                text = stringResource(R.string.banner_no_gps),
            )
        }
    }
}

@Composable
private fun StatusPill(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(DkTheme.radius.full),
        color = BannerBackground,
        contentColor = BannerText,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = DkTheme.spacing.s3,
                vertical = DkTheme.spacing.s2,
            ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BannerText,
                modifier = Modifier.size(DkTheme.spacing.s5),
            )
            Spacer(Modifier.width(DkTheme.spacing.s2))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = BannerText,
            )
        }
    }
}

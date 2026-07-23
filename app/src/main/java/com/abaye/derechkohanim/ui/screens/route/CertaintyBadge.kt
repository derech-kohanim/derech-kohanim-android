package com.abaye.derechkohanim.ui.screens.route

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.ui.components.DkBadge
import com.abaye.derechkohanim.ui.components.DkBadgeTone

/**
 * The certainty marker from spec 17.4 and 17.6. It always pairs a colour with an icon and a
 * word, so the state survives colour blindness and a single glance.
 */
data class CertaintyVisual(val tone: DkBadgeTone, val icon: ImageVector, val labelRes: Int, val bodyRes: Int)

fun certaintyVisual(certainty: Certainty): CertaintyVisual = when (certainty) {
    Certainty.VerifiedClean -> CertaintyVisual(
        tone = DkBadgeTone.Success,
        icon = Icons.Filled.CheckCircle,
        labelRes = R.string.route_verified_clean,
        bodyRes = R.string.route_verified_clean_body,
    )

    Certainty.ZoneNearby -> CertaintyVisual(
        tone = DkBadgeTone.Warning,
        icon = Icons.Filled.Warning,
        labelRes = R.string.route_zone_ahead,
        bodyRes = R.string.route_zone_ahead_body,
    )

    Certainty.Unverified, Certainty.Unknown -> CertaintyVisual(
        tone = DkBadgeTone.Danger,
        icon = Icons.Filled.Error,
        labelRes = R.string.route_unverified,
        bodyRes = R.string.route_unverified_body,
    )
}

@Composable
fun CertaintyBadge(certainty: Certainty) {
    val visual = certaintyVisual(certainty)
    DkBadge(
        text = stringResource(visual.labelRes),
        tone = visual.tone,
        icon = visual.icon,
    )
}

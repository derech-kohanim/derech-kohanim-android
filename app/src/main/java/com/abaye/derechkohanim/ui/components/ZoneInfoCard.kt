package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * Card shown when a zone or danger polygon is tapped: the server-supplied name, city and
 * street, the stringency layer in its own colour (or the danger label), and the full
 * description. Name/city/street/description are server DATA and shown as-is.
 */
@Composable
fun ZoneInfoCard(
    info: ZoneTapInfo,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** Opens the report form about this zone; the link is hidden when null. */
    onReport: (() -> Unit)? = null,
) {
    val colors = DkTheme.colors
    val layerColor = if (info.danger) colors.zoneDanger else zoneColor(info.stringency ?: Stringency.BASIC, colors)

    DkCard(modifier = modifier.fillMaxWidth(), color = colors.surfaceOverlay) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(DkTheme.spacing.s4)
                    .clip(RoundedCornerShape(DkTheme.radius.xs))
                    .background(layerColor),
            )
            Text(
                text = info.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.text,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = DkTheme.spacing.s3),
            )
            DkIconButton(
                icon = Icons.Filled.Close,
                contentDescription = stringResource(R.string.cd_zone_close),
                onClick = onClose,
                tint = colors.textMuted,
            )
        }

        // Layer / danger badge in the matching colour.
        Row(modifier = Modifier.padding(top = DkTheme.spacing.s1)) {
            DkBadge(
                text = layerLabel(info),
                tone = if (info.danger) DkBadgeTone.Danger else DkBadgeTone.Neutral,
            )
        }

        val locality = listOfNotNull(info.street, info.city).joinToString(", ")
        if (locality.isNotBlank()) {
            Text(
                text = locality,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                modifier = Modifier.padding(top = DkTheme.spacing.s2),
            )
        }
        if (!info.description.isNullOrBlank()) {
            Text(
                text = info.description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text,
                modifier = Modifier.padding(top = DkTheme.spacing.s2),
            )
        }
        if (onReport != null) {
            Text(
                text = stringResource(R.string.zone_report_link),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
                modifier = Modifier
                    .padding(top = DkTheme.spacing.s3)
                    .clickable(onClick = onReport)
                    .padding(vertical = DkTheme.spacing.s1),
            )
        }
    }
}

@Composable
private fun layerLabel(info: ZoneTapInfo): String = when {
    info.danger -> stringResource(R.string.zone_card_danger_title)
    info.stringency == Stringency.TAHARAT -> stringResource(R.string.zone_card_layer_taharat)
    info.stringency == Stringency.MACHMIRIM -> stringResource(R.string.zone_card_layer_machmirim)
    else -> stringResource(R.string.zone_card_layer_basic)
}

@Preview(name = "Zone info", showBackground = true, locale = "he")
@Composable
private fun ZoneInfoPreview() {
    DerechKohanimTheme {
        Box(modifier = Modifier.padding(DkTheme.spacing.s4)) {
            ZoneInfoCard(
                info = ZoneTapInfo(
                    name = "הר המנוחות",
                    city = "ירושלים",
                    street = "שדרות הרצל",
                    description = "בית הקברות הר המנוחות. קברי כהנים לאורך הכביש, איסור גמור לכל הדעות.",
                    stringency = Stringency.BASIC,
                    danger = false,
                ),
                onClose = {},
            )
        }
    }
}

@Preview(name = "Danger zone info", showBackground = true, locale = "he")
@Composable
private fun DangerZoneInfoPreview() {
    DerechKohanimTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(DkTheme.spacing.s4)) {
            ZoneInfoCard(
                info = ZoneTapInfo(
                    name = "שטח A - קלנדיה",
                    city = "קלנדיה",
                    street = null,
                    description = "שטח A בשליטה פלסטינית. כניסת ישראלים אסורה ומסוכנת.",
                    stringency = null,
                    danger = true,
                ),
                onClose = {},
            )
        }
    }
}

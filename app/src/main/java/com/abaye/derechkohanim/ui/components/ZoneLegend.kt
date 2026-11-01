package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Legend for the three stringency zone colours, driven entirely by the theme so it stays
 * correct in light and dark. The colours match exactly what DkMap paints per feature.
 */
@Composable
fun ZoneLegend(modifier: Modifier = Modifier, showDanger: Boolean = false) {
    val colors = DkTheme.colors
    DkCard(modifier = modifier, color = colors.surfaceOverlay) {
        Text(
            text = stringResource(R.string.zone_legend_title),
            style = MaterialTheme.typography.titleSmall,
            color = colors.text,
            modifier = Modifier.padding(bottom = DkTheme.spacing.s2),
        )
        Column(verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2)) {
            LegendRow(colors.zoneBasic, stringResource(R.string.zone_legend_basic))
            LegendRow(colors.zoneTaharat, stringResource(R.string.zone_legend_taharat))
            LegendRow(colors.zoneMachmirim, stringResource(R.string.zone_legend_machmirim))
            if (showDanger) {
                LegendRow(colors.zoneDanger, stringResource(R.string.zone_legend_danger))
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(DkTheme.spacing.s3)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = DkTheme.colors.textBody,
            modifier = Modifier.padding(start = DkTheme.spacing.s3),
        )
    }
}

/**
 * The legend folded into a small map button. Tapping it slides the legend up from the bottom
 * as a Civic sheet (surface, 16dp top corners, the scrim setting it off the map): the title, then one row per
 * layer with its colour swatch, split by hairlines. Drag, the scrim or back closes it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneLegendButton(modifier: Modifier = Modifier, showDanger: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    DkFloatingIconButton(
        icon = Icons.Outlined.Layers,
        contentDescription = stringResource(R.string.cd_zone_legend),
        onClick = { open = true },
        modifier = modifier,
    )
    if (open) {
        val colors = DkTheme.colors
        val shape = RoundedCornerShape(topStart = DkTheme.radius.lg, topEnd = DkTheme.radius.lg)
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
            contentColor = colors.text,
            shape = shape,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = DkTheme.spacing.s4, end = DkTheme.spacing.s4, bottom = DkTheme.spacing.s6),
            ) {
                Text(
                    text = stringResource(R.string.zone_legend_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text,
                    modifier = Modifier.padding(bottom = DkTheme.spacing.s2),
                )
                val rows = buildList {
                    add(colors.zoneBasic to R.string.zone_legend_basic)
                    add(colors.zoneTaharat to R.string.zone_legend_taharat)
                    add(colors.zoneMachmirim to R.string.zone_legend_machmirim)
                    if (showDanger) add(colors.zoneDanger to R.string.zone_legend_danger)
                }
                rows.forEachIndexed { index, (color, label) ->
                    if (index > 0) HorizontalDivider(thickness = Dp.Hairline, color = colors.border)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = DkTheme.spacing.s3),
                    ) {
                        // The swatch is drawn like the map paints the zone: a tinted fill, a solid edge.
                        Box(
                            modifier = Modifier
                                .size(DkTheme.spacing.s5)
                                .clip(RoundedCornerShape(DkTheme.radius.xs))
                                .background(color.copy(alpha = 0.25f))
                                .border(2.dp, color, RoundedCornerShape(DkTheme.radius.xs)),
                        )
                        Text(
                            text = stringResource(label),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textBody,
                            modifier = Modifier.padding(start = DkTheme.spacing.s3),
                        )
                    }
                }
            }
        }
    }
}

/** Maps a stringency to its legend label, for reuse elsewhere. */
fun zoneLegendLabelRes(stringency: Stringency): Int = when (stringency) {
    Stringency.BASIC -> R.string.zone_legend_basic
    Stringency.TAHARAT -> R.string.zone_legend_taharat
    Stringency.MACHMIRIM -> R.string.zone_legend_machmirim
}

@Preview(name = "Zone legend", showBackground = true, locale = "he")
@Composable
private fun ZoneLegendPreview() {
    DerechKohanimTheme {
        Box(modifier = Modifier.padding(DkTheme.spacing.s4)) {
            ZoneLegend()
        }
    }
}

@Preview(name = "Zone legend dark", showBackground = true, locale = "he")
@Composable
private fun ZoneLegendDarkPreview() {
    DerechKohanimTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(DkTheme.spacing.s4)) {
            ZoneLegend()
        }
    }
}

package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
 * The legend folded into a small corner button over the map. Tapping it opens the legend in a
 * popup beside the button; tapping outside closes it. Keeps the map clear by default.
 */
@Composable
fun ZoneLegendButton(modifier: Modifier = Modifier, showDanger: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        DkFloatingIconButton(
            icon = Icons.Outlined.Layers,
            contentDescription = stringResource(R.string.cd_zone_legend),
            onClick = { open = !open },
        )
        if (open) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(LocalDensity.current) { (DkTheme.touch.comfortableTarget + DkTheme.spacing.s2).roundToPx() }),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                ZoneLegend(
                    modifier = Modifier.widthIn(max = 320.dp),
                    showDanger = showDanger,
                )
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

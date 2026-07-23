package com.abaye.derechkohanim.ui.screens.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.core.Formatters
import com.abaye.derechkohanim.data.mock.MockData
import com.abaye.derechkohanim.data.model.Maneuver
import com.abaye.derechkohanim.ui.components.DkBottomSheet
import com.abaye.derechkohanim.ui.icons.maneuverGlyph
import com.abaye.derechkohanim.ui.icons.maneuverLabelRes
import com.abaye.derechkohanim.ui.icons.zoneWarningIcon
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * The full step list, opened by tapping the next-maneuver banner. Scrollable, dismissible,
 * with the current step highlighted and any zone warning inline in the danger colour.
 */
@Composable
fun StepListSheet(
    maneuvers: List<Maneuver>,
    currentIndex: Int,
    onDismiss: () -> Unit,
) {
    DkBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.nav_steps_title),
            style = MaterialTheme.typography.headlineSmall,
            color = DkTheme.colors.text,
            modifier = Modifier.padding(bottom = DkTheme.spacing.s2),
        )
        LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
            itemsIndexed(maneuvers, key = { _, m -> m.id }) { index, maneuver ->
                StepRow(maneuver = maneuver, isCurrent = index == currentIndex)
                if (index < maneuvers.lastIndex) {
                    HorizontalDivider(color = DkTheme.colors.border)
                }
            }
        }
    }
}

@Composable
private fun StepRow(maneuver: Maneuver, isCurrent: Boolean) {
    val context = LocalContext.current
    val glyph = maneuverGlyph(maneuver.action)
    val background = if (isCurrent) DkTheme.colors.surfaceMuted else DkTheme.colors.surfaceOverlay

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DkTheme.radius.sm))
            .background(background)
            .padding(vertical = DkTheme.spacing.s3, horizontal = DkTheme.spacing.s2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = glyph.icon,
                contentDescription = null,
                tint = DkTheme.colors.primary,
                modifier = Modifier
                    .size(DkTheme.spacing.s8)
                    .rotate(glyph.rotationDegrees),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = DkTheme.spacing.s3),
            ) {
                Text(
                    text = stringResource(maneuverLabelRes(maneuver.action)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = DkTheme.colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                maneuver.street?.let { street ->
                    Text(
                        text = stringResource(R.string.maneuver_onto_street, street),
                        style = MaterialTheme.typography.bodySmall,
                        color = DkTheme.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (maneuver.distanceMeters > 0) {
                Text(
                    text = Formatters.distance(context, maneuver.distanceMeters),
                    style = MaterialTheme.typography.labelLarge,
                    color = DkTheme.colors.textMuted,
                )
            }
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .padding(start = DkTheme.spacing.s2)
                        .size(DkTheme.spacing.s2)
                        .clip(RoundedCornerShape(DkTheme.radius.full))
                        .background(DkTheme.colors.primary),
                )
            }
        }
        // Zone warnings live inline, in the danger colour, so a step that touches a zone is
        // never mistaken for a clean one (spec 17.8).
        maneuver.zone?.let { zone ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = DkTheme.spacing.s2),
            ) {
                Icon(
                    imageVector = zoneWarningIcon,
                    contentDescription = stringResource(R.string.cd_zone_warning),
                    tint = DkTheme.colors.danger,
                    modifier = Modifier.size(DkTheme.spacing.s4),
                )
                Text(
                    text = zone.nameHe,
                    style = MaterialTheme.typography.bodySmall,
                    color = DkTheme.colors.danger,
                    modifier = Modifier.padding(start = DkTheme.spacing.s2),
                )
            }
        }
    }
}

@Preview(name = "Step list", showBackground = true, locale = "he")
@Composable
private fun StepListPreview() {
    DerechKohanimTheme {
        val route = MockData.route(MockData.currentLocation, MockData.savedPlaces.first().location)
        // Sheets cannot host in a preview, so the row content is previewed directly.
        Column {
            route.maneuvers.forEachIndexed { index, maneuver ->
                StepRow(maneuver = maneuver, isCurrent = index == 1)
            }
        }
    }
}

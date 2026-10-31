package com.abaye.derechkohanim.ui.screens.nav

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import com.abaye.derechkohanim.core.Formatters
import com.abaye.derechkohanim.data.mock.MockData
import com.abaye.derechkohanim.data.model.Lane
import com.abaye.derechkohanim.data.model.LaneIndication
import com.abaye.derechkohanim.data.model.Maneuver
import com.abaye.derechkohanim.ui.components.DkCard
import com.abaye.derechkohanim.ui.icons.ManeuverGlyph
import com.abaye.derechkohanim.ui.icons.maneuverGlyph
import com.abaye.derechkohanim.ui.icons.maneuverLabelRes
import com.abaye.derechkohanim.ui.theme.FontSize
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

// -- lanes ---------------------------------------------------------------------------------------

/** Lanes are shown only this close to the manoeuvre, as in DirGo, so they never distract early. */
const val LANES_VISIBLE_METERS = 1_200.0

/**
 * The lanes at the approach to the next manoeuvre, left to right as they are on the road. The
 * row is forced left-to-right even in Hebrew: lanes are physical positions, not reading order
 * (spec 14.1 - geographic left and right are never mirrored). Lanes that can make the
 * manoeuvre are drawn in the primary colour, the rest muted.
 */
@Composable
fun LanesRow(lanes: List<Lane>, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_lanes)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .background(DkTheme.colors.surfaceMuted, RoundedCornerShape(DkTheme.radius.sm))
                .padding(horizontal = DkTheme.spacing.s2, vertical = DkTheme.spacing.s1)
                .semantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            lanes.forEachIndexed { index, lane ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .width(Dp.Hairline)
                            .height(DkTheme.spacing.s6)
                            .background(DkTheme.colors.border),
                    )
                }
                LaneGlyph(lane)
            }
        }
    }
}

@Composable
private fun LaneGlyph(lane: Lane) {
    val tint = if (lane.valid) DkTheme.colors.primary else DkTheme.colors.textMuted
    val description = stringResource(if (lane.valid) R.string.cd_lane_valid else R.string.cd_lane_invalid)
    // A lane may allow several movements ("through;right"): the arrows are drawn on top of each
    // other, as painted on the asphalt.
    val indications = lane.indications.ifEmpty { listOf(LaneIndication.NONE) }
    Box(
        modifier = Modifier
            .size(DkTheme.spacing.s6)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        indications.forEach { indication ->
            val glyph = laneGlyph(indication)
            Icon(
                imageVector = glyph.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(DkTheme.spacing.s6)
                    .rotate(glyph.rotationDegrees),
            )
        }
    }
}

private fun laneGlyph(indication: LaneIndication): ManeuverGlyph = when (indication) {
    LaneIndication.LEFT -> ManeuverGlyph(Icons.Filled.ArrowUpward, -90f)
    LaneIndication.SLIGHT_LEFT -> ManeuverGlyph(Icons.Filled.ArrowUpward, -45f)
    LaneIndication.SHARP_LEFT -> ManeuverGlyph(Icons.Filled.ArrowUpward, -135f)
    LaneIndication.RIGHT -> ManeuverGlyph(Icons.Filled.ArrowUpward, 90f)
    LaneIndication.SLIGHT_RIGHT -> ManeuverGlyph(Icons.Filled.ArrowUpward, 45f)
    LaneIndication.SHARP_RIGHT -> ManeuverGlyph(Icons.Filled.ArrowUpward, 135f)
    LaneIndication.MERGE_TO_LEFT -> ManeuverGlyph(Icons.Filled.ArrowUpward, -25f)
    LaneIndication.MERGE_TO_RIGHT -> ManeuverGlyph(Icons.Filled.ArrowUpward, 25f)
    LaneIndication.REVERSE -> ManeuverGlyph(Icons.Filled.Refresh, 0f)
    LaneIndication.THROUGH, LaneIndication.NONE -> ManeuverGlyph(Icons.Filled.ArrowUpward, 0f)
}

// -- second turn ---------------------------------------------------------------------------------

/**
 * The manoeuvre after the next one ("ואז" + icon + distance), beside the instruction banner, so a
 * quick sequence of turns is never a surprise. Tapping it repeats the current instruction.
 */
@Composable
fun SecondTurnBox(maneuver: Maneuver, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val glyph = maneuverGlyph(maneuver.action)
    DkCard(modifier = modifier, color = DkTheme.colors.surfaceOverlay, onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
        ) {
            Text(
                text = stringResource(R.string.nav_then_short),
                style = MaterialTheme.typography.labelLarge,
                color = DkTheme.colors.textMuted,
            )
            Icon(
                imageVector = glyph.icon,
                contentDescription = stringResource(maneuverLabelRes(maneuver.action)),
                tint = DkTheme.colors.primary,
                modifier = Modifier
                    .size(DkTheme.spacing.s6)
                    .rotate(glyph.rotationDegrees),
            )
            Text(
                text = Formatters.distance(context, maneuver.distanceMeters),
                style = MaterialTheme.typography.labelLarge,
                color = DkTheme.colors.text,
            )
        }
    }
}

// -- street pill ---------------------------------------------------------------------------------

/**
 * The road being driven, as a floating pill: "כביש 1 • דרך בגין", or just one of the two. The
 * font steps down as the text grows (DirGo's pill: 15 sp up to 16 characters, 13 sp up to 24,
 * 12 sp beyond), mapped onto the body, label and caption size tokens.
 */
@Composable
fun StreetPill(roadRef: String?, street: String?, modifier: Modifier = Modifier) {
    val text = streetPillText(
        roadRef?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.nav_road_ref, it) },
        street,
    ) ?: return
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = streetPillFontSize(text.length)),
        color = DkTheme.colors.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(DkTheme.colors.surfaceOverlay, RoundedCornerShape(DkTheme.radius.full))
            .border(Dp.Hairline, DkTheme.colors.border, RoundedCornerShape(DkTheme.radius.full))
            .padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s2),
    )
}

/** "כביש 1 • street", or whichever of the two exists; null when neither does. */
fun streetPillText(road: String?, street: String?): String? {
    val parts = listOfNotNull(road, street?.takeIf { it.isNotBlank() })
    return if (parts.isEmpty()) null else parts.joinToString(" • ")
}

fun streetPillFontSize(length: Int): TextUnit = when {
    length <= 16 -> FontSize.body
    length <= 24 -> FontSize.label
    else -> FontSize.caption
}

// -- mute ----------------------------------------------------------------------------------------

/**
 * Mute on a short tap, voice settings on a long press (DirGo's audio button). A raised round
 * button like the other map controls, with the long-press hint in its description.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MuteButton(muted: Boolean, onToggle: () -> Unit, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(if (muted) R.string.nav_unmute else R.string.nav_mute)
    val hint = stringResource(R.string.cd_mute_long_press)
    Surface(
        modifier = modifier
            .size(DkTheme.touch.comfortableTarget)
            .clip(CircleShape)
            .combinedClickable(onClick = onToggle, onLongClick = onLongPress)
            .semantics { contentDescription = "$label. $hint" },
        shape = CircleShape,
        color = DkTheme.colors.surfaceOverlay,
        contentColor = DkTheme.colors.text,
        border = BorderStroke(Dp.Hairline, DkTheme.colors.border),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
            )
        }
    }
}

@Preview(name = "Lanes and second turn", showBackground = true, locale = "he")
@Composable
private fun HudPiecesPreview() {
    DerechKohanimTheme {
        val route = MockData.route(MockData.currentLocation, MockData.savedPlaces.first().location)
        androidx.compose.foundation.layout.Column(
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
            modifier = Modifier.padding(DkTheme.spacing.s4),
        ) {
            route.maneuvers.firstNotNullOfOrNull { it.lanes }?.let { LanesRow(it) }
            SecondTurnBox(route.maneuvers[2], onClick = {})
            StreetPill(roadRef = "1", street = "דרך מנחם בגין")
            MuteButton(muted = false, onToggle = {}, onLongPress = {})
        }
    }
}

@Preview(name = "Lanes and second turn (en)", showBackground = true, locale = "en")
@Composable
private fun HudPiecesPreviewEnglish() {
    HudPiecesPreview()
}

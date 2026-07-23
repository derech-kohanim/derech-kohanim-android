package com.abaye.derechkohanim.ui.screens.nav

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.core.Formatters
import com.abaye.derechkohanim.data.mock.MockData
import com.abaye.derechkohanim.data.model.NavigationStatus
import com.abaye.derechkohanim.data.model.RouteEventType
import com.abaye.derechkohanim.data.model.points
import com.abaye.derechkohanim.ui.components.DkBadge
import com.abaye.derechkohanim.ui.components.DkBadgeTone
import com.abaye.derechkohanim.ui.components.DkButton
import com.abaye.derechkohanim.ui.components.DkButtonVariant
import com.abaye.derechkohanim.ui.components.DkCard
import com.abaye.derechkohanim.ui.components.DkFloatingIconButton
import com.abaye.derechkohanim.ui.components.DkMap
import com.abaye.derechkohanim.ui.components.MapControls
import com.abaye.derechkohanim.ui.components.rememberDkMapController
import com.abaye.derechkohanim.ui.icons.eventIcon
import com.abaye.derechkohanim.ui.icons.eventLabelRes
import com.abaye.derechkohanim.ui.icons.maneuverGlyph
import com.abaye.derechkohanim.ui.icons.maneuverLabelRes
import com.abaye.derechkohanim.ui.icons.zoneWarningIcon
import com.abaye.derechkohanim.ui.screens.onboarding.stringencyTitleRes
import com.abaye.derechkohanim.voice.SpokenInstruction
import com.abaye.derechkohanim.voice.VoiceInstruction
import com.abaye.derechkohanim.voice.VoicePhrase
import com.abaye.derechkohanim.ui.theme.DkNavTextStyles
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme

@Composable
fun NavigationRoute(
    onFinished: () -> Unit,
    viewModel: NavigationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.status) {
        if (state.status == NavigationStatus.CANCELLED) onFinished()
    }

    // The spoken phrase is assembled here from the announcement's structured parts, never
    // from the banner text. Then it is handed to the player, which never blocks navigation.
    LaunchedEffect(Unit) {
        viewModel.announcements.collect { announcement ->
            viewModel.speak(buildSpokenInstruction(context, announcement))
        }
    }

    NavigationScreen(
        state = state,
        onToggleMute = viewModel::toggleMute,
        onTogglePause = viewModel::togglePause,
        onRestart = viewModel::restart,
        onStop = viewModel::stop,
        onPreviousStep = viewModel::jumpToPreviousManeuver,
        onNextStep = viewModel::jumpToNextManeuver,
        onReport = viewModel::addReport,
        reportAdded = viewModel.reportAdded,
        onFinish = {
            viewModel.stop()
            onFinished()
        },
    )
}

private fun buildSpokenInstruction(context: Context, announcement: Announcement): SpokenInstruction {
    return if (announcement.maneuverAction != null) {
        SpokenInstruction(
            clipKeys = VoiceInstruction.maneuverClipKeys(
                announcement.maneuverAction,
                announcement.bucketMeters,
                announcement.street,
            ),
            text = VoicePhrase.maneuver(
                context,
                announcement.maneuverAction,
                announcement.bucketMeters,
                announcement.street,
            ),
            urgent = announcement.urgent,
        )
    } else {
        val kind = announcement.eventType ?: RouteEventType.HAZARD
        SpokenInstruction(
            clipKeys = VoiceInstruction.eventClipKeys(kind, announcement.bucketMeters),
            text = VoicePhrase.event(context, kind, announcement.eventTitle, announcement.bucketMeters),
            urgent = announcement.urgent,
        )
    }
}

@Composable
fun NavigationScreen(
    state: NavigationState,
    onToggleMute: () -> Unit,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onReport: (RouteEventType) -> Unit,
    reportAdded: kotlinx.coroutines.flow.SharedFlow<Unit>? = null,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val controller = rememberDkMapController()
    var following by remember { mutableStateOf(true) }
    var showSteps by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var reportConfirmation by remember { mutableStateOf(false) }

    val routePoints = remember(state.route) { state.route?.geometry?.points().orEmpty() }

    LaunchedEffect(state.currentLocation, following, controller.headingUp) {
        val location = state.currentLocation
        if (following && location != null) controller.follow(location, state.headingDegrees)
    }

    LaunchedEffect(reportAdded) {
        reportAdded?.collect {
            reportConfirmation = true
            delay(2200)
            reportConfirmation = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DkMap(
            center = state.currentLocation ?: FALLBACK_CENTER,
            controller = controller,
            modifier = Modifier.fillMaxSize(),
            routePoints = routePoints,
            trafficSegments = state.events.traffic,
            eventMarkers = state.events.events,
            currentLocation = state.currentLocation,
            routeIsClean = state.verifiedClean,
            interactive = false,
        )

        // Top: a zone warning outranks everything and replaces the maneuver panel (spec 15.9,
        // 17.5). A report alert is shown as a lighter banner that does not hide the maneuver.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                // Keep edge-to-edge behind the map, but the guidance panel clears the status bar.
                .statusBarsPadding()
                .padding(DkTheme.spacing.s4),
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
        ) {
            val alert = state.activeAlert
            if (alert != null && alert.isZone) {
                ZoneAlertBanner(text = zoneAlertText(context, alert))
            } else {
                NextManeuverPanel(state = state, onOpenSteps = { showSteps = true })
                if (alert != null) {
                    EventAlertBanner(
                        text = eventAlertText(context, alert.type, alert.distanceMeters),
                        kind = alert.type,
                    )
                }
            }
            AnimatedVisibility(visible = reportConfirmation) {
                DkBadge(
                    text = stringResource(R.string.report_added),
                    tone = DkBadgeTone.Success,
                    icon = Icons.Filled.CheckCircle,
                )
            }
        }

        MapControls(
            onRecenter = {
                following = true
                state.currentLocation?.let { controller.follow(it, state.headingDegrees) }
            },
            onZoomIn = { controller.zoomBy(1f) },
            onZoomOut = { controller.zoomBy(-1f) },
            headingUp = controller.headingUp,
            onToggleOrientation = { controller.headingUp = !controller.headingUp },
            onOverview = {
                following = false
                controller.overview(routePoints)
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(DkTheme.spacing.s4),
        )

        // Bottom-start: current speed and the report button, reachable one-handed.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(DkTheme.spacing.s4)
                .padding(bottom = BOTTOM_BAR_CLEARANCE),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
        ) {
            SpeedPill(text = Formatters.speed(context, state.speedMps))
            DkFloatingIconButton(
                icon = Icons.Filled.Report,
                contentDescription = stringResource(R.string.cd_report),
                onClick = { showReport = true },
            )
        }

        if (state.status == NavigationStatus.ARRIVED) {
            ArrivalPanel(
                onFinish = onFinish,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(DkTheme.spacing.s4),
            )
        } else {
            BottomBar(
                state = state,
                onToggleMute = onToggleMute,
                onTogglePause = onTogglePause,
                onRestart = onRestart,
                onPreviousStep = onPreviousStep,
                onNextStep = onNextStep,
                onStop = onStop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(DkTheme.spacing.s4),
            )
        }
    }

    if (showSteps && state.route != null) {
        StepListSheet(
            maneuvers = state.route.maneuvers,
            currentIndex = state.currentManeuverIndex,
            onDismiss = { showSteps = false },
        )
    }

    if (showReport) {
        ReportSheet(
            onPick = { kind ->
                onReport(kind)
                showReport = false
            },
            onDismiss = { showReport = false },
        )
    }
}

@Composable
private fun NextManeuverPanel(state: NavigationState, onOpenSteps: () -> Unit) {
    val context = LocalContext.current
    val maneuver = state.nextManeuver ?: state.currentManeuver
    DkCard(color = DkTheme.colors.surfaceOverlay, onClick = onOpenSteps) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (maneuver != null) {
                val glyph = maneuverGlyph(maneuver.action)
                Icon(
                    imageVector = glyph.icon,
                    contentDescription = stringResource(maneuverLabelRes(maneuver.action)),
                    tint = DkTheme.colors.primary,
                    modifier = Modifier
                        .size(DkTheme.spacing.s12)
                        .rotate(glyph.rotationDegrees),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = DkTheme.spacing.s4),
            ) {
                Text(
                    text = Formatters.distance(context, state.distanceToManeuverMeters),
                    style = DkNavTextStyles.distance,
                    color = DkTheme.colors.text,
                )
                if (maneuver != null) {
                    Text(
                        text = stringResource(maneuverLabelRes(maneuver.action)),
                        style = DkNavTextStyles.instruction,
                        color = DkTheme.colors.text,
                    )
                    maneuver.street?.let { street ->
                        Text(
                            text = stringResource(R.string.maneuver_onto_street, street),
                            style = DkNavTextStyles.street,
                            color = DkTheme.colors.textMuted,
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = stringResource(R.string.cd_open_steps),
                tint = DkTheme.colors.textMuted,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DkTheme.spacing.s3),
            horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CertaintyChip(state)
            DkBadge(text = stringResource(stringencyTitleRes(state.stringency)))
        }
    }
}

@Composable
private fun CertaintyChip(state: NavigationState) {
    when {
        !state.verifiedClean -> DkBadge(
            text = stringResource(R.string.nav_badge_unverified),
            tone = DkBadgeTone.Danger,
            icon = zoneWarningIcon,
        )

        state.hasZoneNearby -> DkBadge(
            text = stringResource(R.string.nav_badge_zone_ahead),
            tone = DkBadgeTone.Warning,
            icon = zoneWarningIcon,
        )

        else -> DkBadge(
            text = stringResource(R.string.nav_badge_verified),
            tone = DkBadgeTone.Success,
            icon = Icons.Filled.CheckCircle,
        )
    }
}

@Composable
private fun ZoneAlertBanner(text: String) {
    DkCard(
        color = DkTheme.colors.danger,
        contentColor = DkTheme.colors.dangerContrast,
        borderColor = DkTheme.colors.danger,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = zoneWarningIcon,
                contentDescription = stringResource(R.string.cd_zone_warning),
                modifier = Modifier.size(DkTheme.spacing.s10),
            )
            Text(
                text = text,
                style = DkNavTextStyles.instruction,
                modifier = Modifier.padding(start = DkTheme.spacing.s4),
            )
        }
    }
}

@Composable
private fun EventAlertBanner(text: String, kind: RouteEventType) {
    DkCard(color = DkTheme.colors.surfaceOverlay, borderColor = DkTheme.colors.warning) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = eventIcon(kind),
                contentDescription = null,
                tint = DkTheme.colors.warning,
                modifier = Modifier.size(DkTheme.spacing.s6),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = DkTheme.colors.text,
                modifier = Modifier.padding(start = DkTheme.spacing.s3),
            )
        }
    }
}

@Composable
private fun SpeedPill(text: String) {
    DkCard(color = DkTheme.colors.surfaceOverlay) {
        Text(
            text = text,
            style = DkNavTextStyles.stat,
            color = DkTheme.colors.text,
        )
    }
}

@Composable
private fun BottomBar(
    state: NavigationState,
    onToggleMute: () -> Unit,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3)) {
        if (state.simulated) {
            SimulationBar(
                paused = state.simulationPaused,
                progress = state.simulationProgress,
                onTogglePause = onTogglePause,
                onRestart = onRestart,
                onPreviousStep = onPreviousStep,
                onNextStep = onNextStep,
            )
        }
        DkCard(color = DkTheme.colors.surfaceOverlay) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Formatters.arrivalTime(state.durationRemainingSeconds),
                        style = DkNavTextStyles.stat,
                        color = DkTheme.colors.text,
                    )
                    Text(
                        text = Formatters.distance(context, state.distanceRemainingMeters) +
                            "  ·  " +
                            Formatters.duration(context, state.durationRemainingSeconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DkTheme.colors.textMuted,
                    )
                    if (state.trafficDelaySeconds > 60) {
                        Text(
                            text = stringResource(
                                R.string.nav_traffic_delay,
                                Formatters.duration(context, state.trafficDelaySeconds),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = DkTheme.colors.warning,
                        )
                    }
                }
                DkFloatingIconButton(
                    icon = if (state.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = stringResource(
                        if (state.muted) R.string.nav_unmute else R.string.nav_mute
                    ),
                    onClick = onToggleMute,
                )
                Spacer(Modifier.width(DkTheme.spacing.s2))
                DkButton(
                    text = stringResource(R.string.nav_stop),
                    onClick = onStop,
                    variant = DkButtonVariant.Danger,
                )
            }
        }
    }
}

@Composable
private fun SimulationBar(
    paused: Boolean,
    progress: Float,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
) {
    DkCard(color = DkTheme.colors.surfaceOverlay, borderColor = DkTheme.colors.accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DkBadge(text = stringResource(R.string.nav_simulation_banner), tone = DkBadgeTone.Warning)
            Spacer(Modifier.weight(1f))
            DkFloatingIconButton(
                icon = Icons.Filled.NavigateBefore,
                contentDescription = stringResource(R.string.nav_previous_step),
                onClick = onPreviousStep,
            )
            Spacer(Modifier.width(DkTheme.spacing.s1))
            DkButton(
                text = stringResource(
                    if (paused) R.string.nav_simulation_play else R.string.nav_simulation_pause
                ),
                onClick = onTogglePause,
                variant = DkButtonVariant.Secondary,
            )
            Spacer(Modifier.width(DkTheme.spacing.s1))
            DkFloatingIconButton(
                icon = Icons.Filled.NavigateNext,
                contentDescription = stringResource(R.string.nav_next_step),
                onClick = onNextStep,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DkTheme.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f),
                color = DkTheme.colors.accent,
                trackColor = DkTheme.colors.surfaceMuted,
            )
            DkButton(
                text = stringResource(R.string.nav_simulation_restart),
                onClick = onRestart,
                variant = DkButtonVariant.Ghost,
            )
        }
    }
}

@Composable
private fun ArrivalPanel(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    DkCard(modifier = modifier, color = DkTheme.colors.surfaceOverlay) {
        Text(
            text = stringResource(R.string.nav_arrived_title),
            style = MaterialTheme.typography.displaySmall,
            color = DkTheme.colors.text,
        )
        DkButton(
            text = stringResource(R.string.nav_arrived_action),
            onClick = onFinish,
            large = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DkTheme.spacing.s3),
        )
    }
}

/**
 * A short driving alert: "forbidden zone in Xm - name, city". The reason (description) stays
 * out of the driving banner (spec 17.2, no reading in motion); it is shown fully in the tap
 * card on the review screen and in the step list.
 */
private fun zoneAlertText(context: Context, alert: RouteAlert): String {
    val distance = Formatters.distance(context, alert.distanceMeters)
    val place = listOfNotNull(alert.title?.takeIf { it.isNotBlank() }, alert.city).joinToString(", ")
    return context.getString(R.string.nav_zone_ahead_alert, distance)
        .let { if (place.isBlank()) it else "$it - $place" }
}

private fun eventAlertText(context: Context, kind: RouteEventType, distanceMeters: Double): String =
    context.getString(
        R.string.event_ahead_announcement,
        context.getString(eventLabelRes(kind)),
        Formatters.distance(context, distanceMeters),
    )

private val FALLBACK_CENTER = MockData.currentLocation
private val BOTTOM_BAR_CLEARANCE = androidx.compose.ui.unit.Dp(120f)

@Preview(name = "Navigation, simulation", showBackground = true, locale = "he")
@Composable
private fun NavigationPreview() {
    DerechKohanimTheme {
        val route = MockData.route(MockData.currentLocation, MockData.savedPlaces.first().location)
        NavigationScreen(
            state = NavigationState(
                status = NavigationStatus.NAVIGATING,
                route = route,
                events = MockData.eventBundle(route),
                currentManeuverIndex = 1,
                distanceToManeuverMeters = 220.0,
                distanceRemainingMeters = 4_800.0,
                durationRemainingSeconds = 620.0,
                speedMps = 14.0,
                currentLocation = MockData.currentLocation,
                simulated = true,
            ),
            onToggleMute = {},
            onTogglePause = {},
            onRestart = {},
            onStop = {},
            onPreviousStep = {},
            onNextStep = {},
            onReport = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Navigation, zone alert", showBackground = true, locale = "he")
@Composable
private fun NavigationZonePreview() {
    DerechKohanimTheme {
        val route = MockData.route(MockData.currentLocation, MockData.savedPlaces.first().location)
        NavigationScreen(
            state = NavigationState(
                status = NavigationStatus.NAVIGATING,
                route = route,
                currentManeuverIndex = 1,
                distanceToManeuverMeters = 180.0,
                distanceRemainingMeters = 3_100.0,
                durationRemainingSeconds = 420.0,
                speedMps = 9.0,
                currentLocation = MockData.currentLocation,
                activeAlert = RouteAlert(RouteEventType.ZONE, "הר המנוחות", 300.0),
            ),
            onToggleMute = {},
            onTogglePause = {},
            onRestart = {},
            onStop = {},
            onPreviousStep = {},
            onNextStep = {},
            onReport = {},
            onFinish = {},
        )
    }
}

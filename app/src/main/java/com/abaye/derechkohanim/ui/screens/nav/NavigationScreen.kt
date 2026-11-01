package com.abaye.derechkohanim.ui.screens.nav

import android.widget.Toast
import com.abaye.derechkohanim.data.repository.DriveMark
import com.abaye.derechkohanim.ui.screens.report.ReportIcon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import com.abaye.derechkohanim.ui.components.TourResult
import com.abaye.derechkohanim.data.local.Tour
import com.abaye.derechkohanim.ui.components.CoachMarkOverlay
import com.abaye.derechkohanim.ui.components.TourStep
import com.abaye.derechkohanim.ui.components.TourTargets
import com.abaye.derechkohanim.ui.components.TourViewModel
import com.abaye.derechkohanim.ui.components.rememberTourTargets
import com.abaye.derechkohanim.ui.components.tourTarget
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.Dp
import com.abaye.derechkohanim.ui.components.DkStat
import com.abaye.derechkohanim.ui.components.DkStatTable
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.core.Formatters
import com.abaye.derechkohanim.data.mock.MockData
import com.abaye.derechkohanim.data.model.NavFeedbackRating
import com.abaye.derechkohanim.data.model.NavigationStatus
import com.abaye.derechkohanim.data.model.RouteEventType
import com.abaye.derechkohanim.data.model.points
import com.abaye.derechkohanim.ui.components.DkBadge
import com.abaye.derechkohanim.ui.components.DkBadgeTone
import com.abaye.derechkohanim.ui.components.DkButton
import com.abaye.derechkohanim.ui.components.DkButtonVariant
import com.abaye.derechkohanim.ui.components.DkBottomSheet
import com.abaye.derechkohanim.ui.components.DkCard
import com.abaye.derechkohanim.ui.components.DkTextField
import com.abaye.derechkohanim.ui.components.DkFloatingIconButton
import com.abaye.derechkohanim.ui.components.DkIconButton
import com.abaye.derechkohanim.ui.components.DkMap
import com.abaye.derechkohanim.ui.components.DkMapController
import com.abaye.derechkohanim.ui.components.MapControls
import com.abaye.derechkohanim.ui.components.StatusBanner
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
    onOpenVoiceSettings: () -> Unit = {},
    onDescribeMark: (DriveMark) -> Unit = {},
    viewModel: NavigationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tourViewModel: TourViewModel = hiltViewModel()
    val tourPending by tourViewModel.pending(Tour.NAVIGATION).collectAsStateWithLifecycle()

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

    // Ready-made prompts (e.g. rerouting) are spoken as plain text through the same player.
    LaunchedEffect(Unit) {
        viewModel.voicePrompts.collect { res ->
            viewModel.speak(
                SpokenInstruction(clipKeys = emptyList(), text = context.getString(res), urgent = false)
            )
        }
    }

    NavigationScreen(
        state = state,
        onToggleMute = viewModel::toggleMute,
        onRepeatInstruction = viewModel::repeatInstruction,
        onOpenVoiceSettings = onOpenVoiceSettings,
        onTogglePause = viewModel::togglePause,
        onRestart = viewModel::restart,
        onStop = viewModel::stop,
        onPreviousStep = viewModel::jumpToPreviousManeuver,
        onNextStep = viewModel::jumpToNextManeuver,
        onSetSpeed = viewModel::setSimulationSpeed,
        onReport = viewModel::addReport,
        reportAdded = viewModel.reportAdded,
        onSubmitFeedback = viewModel::submitFeedback,
        onMarkProblem = {
            viewModel.markProblem()
            Toast.makeText(context, R.string.nav_mark_toast, Toast.LENGTH_SHORT).show()
        },
        marks = { viewModel.marks() },
        onDescribeMark = { mark ->
            viewModel.clearMarks()
            onDescribeMark(mark)
        },
        onDismissMarks = viewModel::clearMarks,
        tourPending = tourPending == true,
        onTourDone = { result -> tourViewModel.finish(Tour.NAVIGATION, result) },
        onFinish = {
            viewModel.stop()
            onFinished()
        },
    )
}

private fun buildSpokenInstruction(
    context: Context,
    announcement: Announcement,
): SpokenInstruction {
    val cue = announcement.cue
    return if (cue != null) {
        SpokenInstruction(
            clipKeys = VoiceInstruction.cueClipKeys(cue, announcement.bucketMeters),
            text = VoicePhrase.cue(context, cue, announcement.bucketMeters),
            urgent = announcement.urgent,
        )
    } else if (announcement.maneuverAction != null) {
        SpokenInstruction(
            clipKeys = VoiceInstruction.maneuverClipKeys(
                announcement.maneuverAction,
                announcement.bucketMeters,
                announcement.street,
                announcement.exitNumber,
            ),
            text = VoicePhrase.maneuver(
                context,
                announcement.maneuverAction,
                announcement.bucketMeters,
                announcement.street,
                announcement.exitNumber,
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
    onRepeatInstruction: () -> Unit = {},
    onOpenVoiceSettings: () -> Unit = {},
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onSetSpeed: (Int) -> Unit = {},
    onReport: (RouteEventType) -> Unit,
    reportAdded: kotlinx.coroutines.flow.SharedFlow<Unit>? = null,
    onSubmitFeedback: (NavFeedbackRating, String?) -> Unit = { _, _ -> },
    onFinish: () -> Unit,
    onMarkProblem: () -> Unit = {},
    /** The problems marked on this drive, read when the drive ends. */
    marks: () -> List<DriveMark> = { emptyList() },
    onDescribeMark: (DriveMark) -> Unit = {},
    onDismissMarks: () -> Unit = {},
    /** True when the navigation tour should run (first drive, tours not off). */
    tourPending: Boolean = false,
    onTourDone: (TourResult) -> Unit = {},
) {
    val targets = rememberTourTargets()
    // Never in the middle of a real drive: the tour waits for a simulation or a stop. The voice
    // guidance carries on underneath the card.
    val tourCanRun = state.simulated || state.speedMps < TOUR_STOPPED_MPS
    var tourShown by remember { mutableStateOf(false) }
    LaunchedEffect(tourPending, tourCanRun, state.status) {
        if (tourPending && tourCanRun && state.status != NavigationStatus.ARRIVED && !tourShown) {
            kotlinx.coroutines.delay(1200)
            tourShown = true
        }
    }
    val context = LocalContext.current
    val controller = rememberDkMapController()
    var following by remember { mutableStateOf(true) }
    var showSteps by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var showStopFeedback by remember { mutableStateOf(false) }
    var reportConfirmation by remember { mutableStateOf(false) }
    var offlineReadyVisible by remember { mutableStateOf(false) }

    // Surface the "ready for offline" confirmation briefly once prefetch reports done.
    LaunchedEffect(state.offlineReady) {
        if (state.offlineReady) {
            offlineReadyVisible = true
            delay(3000)
            offlineReadyVisible = false
        }
    }

    val fullRoutePoints = remember(state.route) { state.route?.geometry?.points().orEmpty() }
    // Draw only the route still ahead (from the location marker onward), not the driven part.
    // Falls back to the full route before any progress has been applied.
    val routePoints = state.remainingRoutePoints.ifEmpty { fullRoutePoints }

    LaunchedEffect(state.currentLocation, following, controller.headingUp, controller.autoZoom) {
        val location = state.currentLocation
        if (following && location != null) {
            controller.follow(location, state.headingDegrees, state.speedMps)
        }
    }

    // In overview, if the vehicle is actually moving, snap back to the driving view after a few
    // seconds so the driver is never left looking at the whole route mid-drive. While stopped it
    // stays in overview. Keyed on a stable "moving" flag, not the raw speed, so the timer runs
    // once rather than restarting on every fix.
    val overviewMoving = state.speedMps > OVERVIEW_MOVING_MPS
    LaunchedEffect(following, overviewMoving) {
        if (!following && overviewMoving) {
            delay(OVERVIEW_AUTO_RETURN_MS)
            following = true
            controller.resumeAutoZoom()
            state.currentLocation?.let { controller.follow(it, state.headingDegrees, state.speedMps) }
        }
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
            // Driving view: open close in, not at the wide default, so the first frame is
            // already a navigation view. follow() takes the zoom from there.
            zoom = DkMapController.FOLLOW_ZOOM,
            modifier = Modifier.fillMaxSize(),
            routePoints = routePoints,
            trafficSegments = state.remainingTraffic,
            eventMarkers = state.events.events,
            zones = state.zones,
            dangerZones = state.dangerZones,
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
            } else if (state.rerouting) {
                ReroutingPanel()
            } else if (state.returnToRoute != null) {
                ReturnToRoutePanel(guidance = state.returnToRoute, offline = state.offline)
            } else {
                NextManeuverPanel(
                    state = state,
                    onRepeat = onRepeatInstruction,
                    onOpenSteps = { showSteps = true },
                    modifier = Modifier.tourTarget(targets, NAV_BANNER),
                )
                NavStatusChips(state)
                if (alert != null) {
                    EventAlertBanner(
                        text = eventAlertText(context, alert),
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
            // Waze-style status banner, directly under the instruction card.
            StatusBanner(offline = state.offline, noGps = state.noGps)
            // A brief "ready for offline" confirmation once the drive's clips are cached.
            AnimatedVisibility(visible = offlineReadyVisible) {
                DkBadge(
                    text = stringResource(R.string.nav_offline_ready),
                    tone = DkBadgeTone.Success,
                    icon = Icons.Filled.CloudDone,
                )
            }
        }

        MapControls(
            onRecenter = {
                following = true
                controller.resumeAutoZoom()
                state.currentLocation?.let {
                    controller.follow(it, state.headingDegrees, state.speedMps)
                }
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
            SpeedCluster(
                speedKmh = speedKmh(state.speedMps),
                limitKmh = state.speedLimitKmh,
                speeding = state.speeding,
                modifier = Modifier.tourTarget(targets, NAV_SPEED),
            )
            // In-drive reporting is temporarily hidden behind REPORTING_ENABLED. The ReportSheet
            // and its handling are kept intact so it can be re-enabled by flipping the flag.
            if (REPORTING_ENABLED) {
                DkFloatingIconButton(
                    icon = Icons.Filled.Report,
                    contentDescription = stringResource(R.string.cd_report),
                    onClick = { showReport = true },
                )
            }
        }

        // A red frame around the whole screen while speeding: visible in peripheral vision.
        if (state.speeding) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(DkTheme.spacing.s2, DkTheme.colors.danger),
            )
        }

        if (state.status == NavigationStatus.ARRIVED) {
            ArrivalPanel(
                onSubmitFeedback = onSubmitFeedback,
                onFinish = onFinish,
                marks = remember { marks() },
                onDescribeMark = onDescribeMark,
                onDismissMarks = onDismissMarks,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
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
                onSetSpeed = onSetSpeed,
                onOpenVoiceSettings = onOpenVoiceSettings,
                onOpenSteps = { showSteps = true },
                // Stopping early offers optional feedback first; the sheet's own actions stop.
                onStop = { showStopFeedback = true },
                onMarkProblem = onMarkProblem,
                targets = targets,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(DkTheme.spacing.s4),
            )
        }

        if (tourShown) {
            val steps = remember {
                listOf(
                    TourStep(NAV_BANNER, R.string.tour_nav_banner_title, R.string.tour_nav_banner_body),
                    TourStep(NAV_SPEED, R.string.tour_nav_speed_title, R.string.tour_nav_speed_body),
                    TourStep(NAV_MUTE, R.string.tour_nav_mute_title, R.string.tour_nav_mute_body),
                    TourStep(NAV_STEPS, R.string.tour_nav_steps_title, R.string.tour_nav_steps_body),
                    TourStep(NAV_FLAG, R.string.tour_nav_flag_title, R.string.tour_nav_flag_body),
                ).filter { targets.has(it.target!!) }
            }
            CoachMarkOverlay(
                steps = steps,
                targets = targets,
                onDone = { result ->
                    tourShown = false
                    onTourDone(result)
                },
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

    if (REPORTING_ENABLED && showReport) {
        ReportSheet(
            onPick = { kind ->
                onReport(kind)
                showReport = false
            },
            onDismiss = { showReport = false },
        )
    }

    // Optional feedback when stopping early. Dismissing keeps driving; the sheet's own Stop and
    // Send actions both end navigation, so stopping never depends on leaving feedback.
    if (showStopFeedback) {
        StopFeedbackSheet(
            onSubmitFeedback = onSubmitFeedback,
            onStop = {
                showStopFeedback = false
                onStop()
            },
            onDismiss = { showStopFeedback = false },
        )
    }
}

/**
 * The next manoeuvre. A tap repeats it aloud (DirGo); the list button opens the step list. Within
 * [LANES_VISIBLE_METERS] of the turn, the lanes appear under it when the map has them.
 */
@Composable
private fun NextManeuverPanel(
    state: NavigationState,
    onRepeat: () -> Unit,
    onOpenSteps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = DkTheme.colors
    val maneuver = state.nextManeuver ?: state.currentManeuver
    // The instruction banner stays a solid royal fill (on-royal text), so the next action reads
    // at a glance over any map. The second turn sits in a deeper royal strip under it.
    val onBanner = colors.primaryContrast
    Surface(
        onClick = onRepeat,
        modifier = modifier,
        shape = RoundedCornerShape(DkTheme.radius.lg),
        color = colors.primary,
        contentColor = onBanner,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(DkTheme.spacing.s4),
            ) {
                if (maneuver != null) {
                    val glyph = maneuverGlyph(maneuver.action)
                    Icon(
                        imageVector = glyph.icon,
                        contentDescription = stringResource(maneuverLabelRes(maneuver.action)),
                        tint = onBanner,
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
                        color = onBanner,
                    )
                    if (maneuver != null) {
                        Text(
                            text = stringResource(maneuverLabelRes(maneuver.action)),
                            style = DkNavTextStyles.instruction,
                            color = onBanner,
                        )
                        maneuver.street?.let { street ->
                            Text(
                                text = stringResource(R.string.maneuver_onto_street, street),
                                style = DkNavTextStyles.street,
                                color = onBanner.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
                DkIconButton(
                    icon = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.cd_open_steps),
                    onClick = onOpenSteps,
                    tint = onBanner,
                )
            }
            val lanes = maneuver?.lanes
            if (!lanes.isNullOrEmpty() && state.distanceToManeuverMeters <= LANES_VISIBLE_METERS) {
                LanesRow(
                    lanes = lanes,
                    modifier = Modifier.padding(
                        start = DkTheme.spacing.s4,
                        end = DkTheme.spacing.s4,
                        bottom = DkTheme.spacing.s3,
                    ),
                )
            }
            state.secondManeuver?.let { second ->
                val glyph = maneuverGlyph(second.action)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.primaryHover)
                        .padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s2),
                ) {
                    Text(
                        text = stringResource(R.string.nav_then_short),
                        style = MaterialTheme.typography.labelMedium,
                        color = onBanner.copy(alpha = 0.85f),
                    )
                    Icon(
                        imageVector = glyph.icon,
                        contentDescription = null,
                        tint = onBanner,
                        modifier = Modifier
                            .size(DkTheme.spacing.s5)
                            .rotate(glyph.rotationDegrees),
                    )
                    Text(
                        text = stringResource(maneuverLabelRes(second.action)) +
                            (second.street?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = onBanner,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = Formatters.distance(context, second.distanceMeters),
                        style = MaterialTheme.typography.labelMedium,
                        color = onBanner.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

/** The status chips under the instruction banner: a route state only when it is not plain clean, and the level. */
@Composable
private fun NavStatusChips(state: NavigationState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CertaintyChip(state)
        DkBadge(
            text = stringResource(R.string.avoidance_level_chip, stringResource(stringencyTitleRes(state.stringency))),
            tone = DkBadgeTone.Info,
        )
    }
}

/**
 * Replaces the manoeuvre card while the driver is off the route with no network: an arrow
 * pointing back to the route relative to the driving direction, and how far it is.
 */
@Composable
private fun ReturnToRoutePanel(guidance: ReturnGuidance, offline: Boolean) {
    val context = LocalContext.current
    DkCard(color = DkTheme.colors.surfaceOverlay, borderColor = DkTheme.colors.warning) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = stringResource(R.string.cd_return_to_route_arrow),
                tint = DkTheme.colors.warning,
                modifier = Modifier
                    .size(DkTheme.spacing.s12)
                    .rotate(guidance.relativeDegrees),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = DkTheme.spacing.s4),
            ) {
                Text(
                    text = stringResource(R.string.nav_return_to_route),
                    style = DkNavTextStyles.instruction,
                    color = DkTheme.colors.text,
                )
                Text(
                    text = stringResource(
                        R.string.nav_return_to_route_detail,
                        Formatters.distance(context, guidance.distanceMeters),
                    ),
                    style = DkNavTextStyles.street,
                    color = DkTheme.colors.textMuted,
                )
                if (offline) {
                    Text(
                        text = stringResource(R.string.nav_return_to_route_offline),
                        style = MaterialTheme.typography.bodySmall,
                        color = DkTheme.colors.textMuted,
                    )
                }
            }
        }
    }
}

/** Replaces the manoeuvre card while a new route is being computed after going off-route. */
@Composable
private fun ReroutingPanel() {
    DkCard(color = DkTheme.colors.surfaceOverlay) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.CircularProgressIndicator(
                color = DkTheme.colors.primary,
                modifier = Modifier.size(DkTheme.spacing.s6),
            )
            Text(
                text = stringResource(R.string.nav_rerouting),
                style = DkNavTextStyles.instruction,
                color = DkTheme.colors.text,
                modifier = Modifier.padding(start = DkTheme.spacing.s4),
            )
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

        // Every route is verified; a plain clean one needs no chip.
        else -> Unit
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
private fun BottomBar(
    state: NavigationState,
    onToggleMute: () -> Unit,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onSetSpeed: (Int) -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onOpenSteps: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    onMarkProblem: () -> Unit = {},
    targets: TourTargets? = null,
) {
    val context = LocalContext.current
    fun Modifier.target(key: String) = if (targets != null) tourTarget(targets, key) else this
    // Dragging the bottom card upwards opens the route steps, like pulling up a sheet.
    val dragThreshold = with(LocalDensity.current) { DkTheme.spacing.s8.toPx() }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The road being driven, as a floating pill over the bottom card.
        state.currentManeuver?.let { current ->
            StreetPill(roadRef = current.roadRef, street = current.street)
        }
        if (state.simulated) {
            SimulationBar(
                paused = state.simulationPaused,
                progress = state.simulationProgress,
                speed = state.simulationSpeed,
                onTogglePause = onTogglePause,
                onRestart = onRestart,
                onPreviousStep = onPreviousStep,
                onNextStep = onNextStep,
                onSetSpeed = onSetSpeed,
            )
        }
        // Civic driving bar: a flush stat table (remaining, distance, arrival) over a row of
        // outline actions, with the danger "stop" at the far end. Dragging it up opens the steps.
        Surface(
            shape = RoundedCornerShape(DkTheme.radius.lg),
            color = DkTheme.colors.surface,
            border = BorderStroke(Dp.Hairline, DkTheme.colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    var dragged = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragged = 0f },
                        onVerticalDrag = { _, delta -> dragged += delta },
                        onDragEnd = { if (dragged < -dragThreshold) onOpenSteps() },
                    )
                },
        ) {
            Column {
                DkStatTable(
                    stats = listOf(
                        DkStat(stringResource(R.string.nav_remaining), Formatters.duration(context, state.durationRemainingSeconds)),
                        DkStat(stringResource(R.string.route_distance), Formatters.distance(context, state.distanceRemainingMeters)),
                        DkStat(stringResource(R.string.nav_arrival), Formatters.arrivalTime(state.durationRemainingSeconds)),
                    ),
                    bordered = false,
                    valueStyle = DkNavTextStyles.stat,
                )
                if (state.trafficDelaySeconds > 60) {
                    Text(
                        text = stringResource(
                            R.string.nav_traffic_delay,
                            Formatters.duration(context, state.trafficDelaySeconds),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = DkTheme.colors.warning,
                        modifier = Modifier.padding(horizontal = DkTheme.spacing.s3),
                    )
                }
                HorizontalDivider(thickness = Dp.Hairline, color = DkTheme.colors.border)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
                    modifier = Modifier.padding(DkTheme.spacing.s3),
                ) {
                    DkButton(
                        text = stringResource(R.string.nav_steps_button),
                        onClick = onOpenSteps,
                        variant = DkButtonVariant.Secondary,
                        leadingIcon = Icons.AutoMirrored.Filled.List,
                        modifier = Modifier.target(NAV_STEPS),
                    )
                    MuteButton(
                        muted = state.muted,
                        onToggle = onToggleMute,
                        onLongPress = onOpenVoiceSettings,
                        modifier = Modifier.target(NAV_MUTE),
                    )
                    // One tap marks a problem here; no typing while driving.
                    Surface(
                        shape = CircleShape,
                        color = DkTheme.colors.surface,
                        border = BorderStroke(1.dp, DkTheme.colors.borderStrong),
                        modifier = Modifier.target(NAV_FLAG),
                    ) {
                        DkIconButton(
                            icon = ReportIcon,
                            contentDescription = stringResource(R.string.nav_mark_problem),
                            onClick = onMarkProblem,
                            tint = DkTheme.colors.primary,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    DkButton(
                        text = stringResource(R.string.nav_stop),
                        onClick = onStop,
                        variant = DkButtonVariant.Danger,
                    )
                }
            }
        }
    }
}

@Composable
private fun SimulationBar(
    paused: Boolean,
    progress: Float,
    speed: Int,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onSetSpeed: (Int) -> Unit,
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
        // Playback-speed chips (1x/2x/4x/8x), matching the web app. The selected step is filled.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DkTheme.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s1),
        ) {
            DkBadge(text = stringResource(R.string.nav_simulation_speed))
            Spacer(Modifier.weight(1f))
            listOf(1, 2, 4, 8).forEach { option ->
                DkButton(
                    text = stringResource(R.string.nav_simulation_speed_value, option),
                    onClick = { onSetSpeed(option) },
                    variant = if (speed == option) DkButtonVariant.Primary else DkButtonVariant.Ghost,
                )
            }
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
private fun ArrivalPanel(
    onSubmitFeedback: (NavFeedbackRating, String?) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    marks: List<DriveMark> = emptyList(),
    onDescribeMark: (DriveMark) -> Unit = {},
    onDismissMarks: () -> Unit = {},
) {
    var marksPending by remember { mutableStateOf(marks.isNotEmpty()) }
    DkCard(modifier = modifier, color = DkTheme.colors.surfaceOverlay) {
        Text(
            text = stringResource(R.string.nav_arrived_title),
            style = MaterialTheme.typography.displaySmall,
            color = DkTheme.colors.text,
        )
        // Problems marked on the way: offer to describe the first, or let them go.
        if (marksPending) {
            Surface(
                shape = RoundedCornerShape(DkTheme.radius.md),
                color = DkTheme.colors.primarySoft,
                border = BorderStroke(Dp.Hairline, DkTheme.colors.primary.copy(alpha = 0.25f)),
                modifier = Modifier.padding(top = DkTheme.spacing.s3),
            ) {
                Column(
                    modifier = Modifier.padding(DkTheme.spacing.s3),
                    verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
                ) {
                    Text(
                        text = stringResource(R.string.nav_marks_prompt),
                        style = MaterialTheme.typography.titleSmall,
                        color = DkTheme.colors.text,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2)) {
                        DkButton(
                            text = stringResource(R.string.nav_marks_describe),
                            onClick = { marks.firstOrNull()?.let(onDescribeMark) },
                        )
                        DkButton(
                            text = stringResource(R.string.nav_marks_later),
                            onClick = {
                                marksPending = false
                                onDismissMarks()
                            },
                            variant = DkButtonVariant.Ghost,
                        )
                    }
                }
            }
        }
        FeedbackControls(
            onSubmitFeedback = onSubmitFeedback,
            onProceed = onFinish,
            proceedLabelRes = R.string.nav_arrived_action,
            modifier = Modifier.padding(top = DkTheme.spacing.s3),
        )
    }
}

/**
 * Optional feedback shown in a sheet when the driver stops early. Mirrors the arrival flow: a
 * good/bad rating, a "what went wrong?" note on a bad rating, and either action ends the drive.
 */
@Composable
private fun StopFeedbackSheet(
    onSubmitFeedback: (NavFeedbackRating, String?) -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
) {
    DkBottomSheet(onDismissRequest = onDismiss) {
        FeedbackControls(
            onSubmitFeedback = onSubmitFeedback,
            onProceed = onStop,
            proceedLabelRes = R.string.nav_stop,
        )
    }
}

/**
 * The shared good/bad feedback control. Thumbs up / thumbs down set the rating; a bad rating
 * reveals the optional free-text note. Sending POSTs the feedback (fire and forget in the VM),
 * shows a brief thank-you, then proceeds. The proceed action (finish or stop) always works even
 * with no rating, so feedback never blocks leaving.
 */
@Composable
private fun FeedbackControls(
    onSubmitFeedback: (NavFeedbackRating, String?) -> Unit,
    onProceed: () -> Unit,
    proceedLabelRes: Int,
    modifier: Modifier = Modifier,
) {
    var rating by remember { mutableStateOf<NavFeedbackRating?>(null) }
    var dislikeText by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    if (submitted) {
        // Brief confirmation, then proceed on its own so the flow never stalls.
        LaunchedEffect(Unit) {
            delay(1200)
            onProceed()
        }
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = DkTheme.colors.success,
                modifier = Modifier.size(DkTheme.spacing.s6),
            )
            Text(
                text = stringResource(R.string.nav_feedback_thanks),
                style = MaterialTheme.typography.titleMedium,
                color = DkTheme.colors.text,
                modifier = Modifier.padding(start = DkTheme.spacing.s3),
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
    ) {
        Text(
            text = stringResource(R.string.nav_feedback_prompt),
            style = MaterialTheme.typography.titleMedium,
            color = DkTheme.colors.text,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2)) {
            ThumbButton(
                icon = Icons.Filled.ThumbUp,
                label = stringResource(R.string.nav_feedback_good),
                contentDescription = stringResource(R.string.cd_feedback_good),
                selected = rating == NavFeedbackRating.GOOD,
                tone = DkBadgeTone.Success,
                onClick = { rating = NavFeedbackRating.GOOD },
                modifier = Modifier.weight(1f),
            )
            ThumbButton(
                icon = Icons.Filled.ThumbDown,
                label = stringResource(R.string.nav_feedback_bad),
                contentDescription = stringResource(R.string.cd_feedback_bad),
                selected = rating == NavFeedbackRating.BAD,
                tone = DkBadgeTone.Danger,
                onClick = { rating = NavFeedbackRating.BAD },
                modifier = Modifier.weight(1f),
            )
        }
        AnimatedVisibility(visible = rating == NavFeedbackRating.BAD) {
            DkTextField(
                value = dislikeText,
                onValueChange = { dislikeText = it },
                label = stringResource(R.string.nav_feedback_dislike_placeholder),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DkButton(
                text = stringResource(proceedLabelRes),
                onClick = onProceed,
                variant = DkButtonVariant.Ghost,
                modifier = Modifier.weight(1f),
            )
            DkButton(
                text = stringResource(R.string.nav_feedback_submit),
                onClick = {
                    rating?.let { chosen ->
                        onSubmitFeedback(chosen, dislikeText.takeIf { chosen == NavFeedbackRating.BAD })
                        submitted = true
                    }
                },
                enabled = rating != null,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThumbButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String,
    selected: Boolean,
    tone: DkBadgeTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (tone == DkBadgeTone.Success) DkTheme.colors.success else DkTheme.colors.danger
    DkCard(
        modifier = modifier,
        color = if (selected) accent else DkTheme.colors.surface,
        borderColor = if (selected) accent else DkTheme.colors.border,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) DkTheme.colors.text else accent,
                modifier = Modifier.size(DkTheme.spacing.s6),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = DkTheme.colors.text,
                modifier = Modifier.padding(start = DkTheme.spacing.s2),
            )
        }
    }
}

/**
 * A short driving alert: "forbidden zone in Xm - name, city". The reason (description) stays
 * out of the driving banner (spec 17.2, no reading in motion); it is shown fully in the tap
 * card on the review screen and in the step list.
 */
private fun zoneAlertText(context: Context, alert: RouteAlert): String {
    val place = listOfNotNull(alert.title?.takeIf { it.isNotBlank() }, alert.city).joinToString(", ")
    val head = if (alert.inside) {
        context.getString(R.string.nav_zone_enter_alert)
    } else {
        context.getString(R.string.nav_zone_ahead_alert, Formatters.distance(context, alert.distanceMeters))
    }
    return if (place.isBlank()) head else "$head - $place"
}

private fun eventAlertText(context: Context, alert: RouteAlert): String {
    // A camera with a known enforced limit names it: "מצלמת מהירות, 90 קמ״ש בעוד 300 מ'".
    val name = alert.maxSpeedKmh
        ?.takeIf { alert.type == RouteEventType.SPEED_CAMERA }
        ?.let { context.getString(R.string.event_speed_camera_limit, it) }
        ?: context.getString(eventLabelRes(alert.type))
    return context.getString(
        R.string.event_ahead_announcement,
        name,
        Formatters.distance(context, alert.distanceMeters),
    )
}

private val FALLBACK_CENTER = MockData.currentLocation
private val BOTTOM_BAR_CLEARANCE = androidx.compose.ui.unit.Dp(120f)
/** How long overview stays up while moving before snapping back to the driving view. */
private const val OVERVIEW_AUTO_RETURN_MS = 10_000L
/** Above this speed (~7 km/h) the vehicle counts as moving for the overview auto-return. */
private const val OVERVIEW_MOVING_MPS = 2.0

/**
 * In-drive reporting (the report FAB and its ReportSheet) is temporarily hidden. Flip to true to
 * restore the entry point; the ReportSheet and its wiring are left in place.
 */
private const val REPORTING_ENABLED = false

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
                speedLimitKmh = 50,
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

@Preview(name = "Navigation, back to route offline", showBackground = true, locale = "he")
@Composable
private fun NavigationReturnPreview() {
    DerechKohanimTheme {
        val route = MockData.route(MockData.currentLocation, MockData.savedPlaces.first().location)
        NavigationScreen(
            state = NavigationState(
                status = NavigationStatus.NAVIGATING,
                route = route,
                currentLocation = MockData.currentLocation,
                offline = true,
                returnToRoute = ReturnGuidance(distanceMeters = 140.0, bearingDegrees = 250f, relativeDegrees = -70f),
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

@Preview(name = "Navigation, back to route offline (en)", showBackground = true, locale = "en")
@Composable
private fun NavigationReturnPreviewEnglish() {
    NavigationReturnPreview()
}

@Preview(name = "Navigation, speed camera", showBackground = true, locale = "he")
@Composable
private fun NavigationCameraPreview() {
    DerechKohanimTheme {
        val route = MockData.route(MockData.currentLocation, MockData.savedPlaces.first().location)
        NavigationScreen(
            state = NavigationState(
                status = NavigationStatus.NAVIGATING,
                route = route,
                events = MockData.eventBundle(route),
                currentManeuverIndex = 1,
                distanceToManeuverMeters = 640.0,
                currentLocation = MockData.currentLocation,
                speedMps = 24.0,
                activeAlert = RouteAlert(RouteEventType.SPEED_CAMERA, null, 420.0, maxSpeedKmh = 90),
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

@Preview(name = "Navigation, speed camera (en)", showBackground = true, locale = "en")
@Composable
private fun NavigationCameraPreviewEnglish() {
    NavigationCameraPreview()
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

/** Below this the car counts as stopped and the navigation tour may open. */
private const val TOUR_STOPPED_MPS = 1.0
private const val NAV_BANNER = "nav.banner"
private const val NAV_SPEED = "nav.speed"
private const val NAV_MUTE = "nav.mute"
private const val NAV_STEPS = "nav.steps"
private const val NAV_FLAG = "nav.flag"

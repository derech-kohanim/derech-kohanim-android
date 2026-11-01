package com.abaye.derechkohanim.ui.screens.route

import com.abaye.derechkohanim.ui.components.TourResult
import com.abaye.derechkohanim.data.local.Tour
import com.abaye.derechkohanim.ui.components.CoachMarkOverlay
import com.abaye.derechkohanim.ui.components.TourStep
import com.abaye.derechkohanim.ui.components.TourTargets
import com.abaye.derechkohanim.ui.components.TourViewModel
import com.abaye.derechkohanim.ui.components.rememberTourTargets
import com.abaye.derechkohanim.ui.components.tourTarget
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import com.abaye.derechkohanim.ui.components.DkChipTone
import com.abaye.derechkohanim.ui.components.DkConfirmDialog
import com.abaye.derechkohanim.ui.components.DkListBox
import com.abaye.derechkohanim.ui.components.DkListDivider
import com.abaye.derechkohanim.ui.components.DkListRow
import com.abaye.derechkohanim.ui.components.DkSectionTitle
import com.abaye.derechkohanim.ui.screens.onboarding.stringencyTitleRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.abaye.derechkohanim.ui.components.DkIconButton
import com.abaye.derechkohanim.ui.components.DkStat
import com.abaye.derechkohanim.ui.components.DkStatTable
import com.abaye.derechkohanim.ui.components.ZoneLegendButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.core.ErrorCode
import com.abaye.derechkohanim.core.Formatters
import com.abaye.derechkohanim.data.mock.MockData
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.PlaceCategory
import com.abaye.derechkohanim.data.model.RouteResult
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.points
import com.abaye.derechkohanim.ui.components.DkBadge
import com.abaye.derechkohanim.ui.components.DkBadgeTone
import com.abaye.derechkohanim.ui.components.ComputingRouteDialog
import com.abaye.derechkohanim.ui.components.DkButton
import com.abaye.derechkohanim.ui.components.DkButtonVariant
import com.abaye.derechkohanim.ui.components.DkCard
import com.abaye.derechkohanim.ui.components.DkMap
import com.abaye.derechkohanim.ui.components.DkSwitchRow
import com.abaye.derechkohanim.ui.components.ZoneInfoCard
import com.abaye.derechkohanim.ui.components.ZoneLegend
import com.abaye.derechkohanim.ui.components.ZoneTapInfo
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme

@Composable
fun RouteReviewRoute(
    origin: LatLng?,
    onStart: (simulated: Boolean) -> Unit,
    onChangeDestination: () -> Unit,
    onChangeStringency: () -> Unit,
    onBack: () -> Unit,
    viewModel: RouteReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tourViewModel: TourViewModel = hiltViewModel()
    val tourPending by tourViewModel.pending(Tour.ROUTE).collectAsStateWithLifecycle()
    val noCleanPending by tourViewModel.pending(Tour.ROUTE_NO_CLEAN).collectAsStateWithLifecycle()

    // Cancelling the computation returns to planning cleanly: pop back with no route, no error.
    LaunchedEffect(Unit) {
        viewModel.cancelled.collect { onBack() }
    }

    RouteReviewScreen(
        state = state,
        origin = origin,
        onRetry = viewModel::compute,
        onCancelCompute = viewModel::cancelCompute,
        onStart = { simulated ->
            viewModel.startNavigation(simulated)
            onStart(simulated)
        },
        onChangeDestination = onChangeDestination,
        onChangeStringency = onChangeStringency,
        onSelectRoute = viewModel::selectRoute,
        onSetTrafficAware = viewModel::setTrafficAware,
        onSetAvoidTolls = viewModel::setAvoidTolls,
        onSetAvoidDangerZones = viewModel::setAvoidDangerZones,
        onSetShowUnavoided = viewModel::setShowUnavoided,
        onSave = viewModel::saveDestination,
        tourPending = tourPending == true,
        noCleanStepPending = noCleanPending == true,
        onTourDone = { result ->
            tourViewModel.finish(Tour.ROUTE, result, onlyStepKey = ROUTE_BLOCKED, onlyStepTour = Tour.ROUTE_NO_CLEAN)
        },
    )
}

@Composable
fun RouteReviewScreen(
    state: RouteReviewState,
    origin: LatLng?,
    onRetry: () -> Unit,
    onStart: (simulated: Boolean) -> Unit,
    onChangeDestination: () -> Unit,
    onChangeStringency: () -> Unit,
    onCancelCompute: () -> Unit = {},
    onSelectRoute: (String) -> Unit = {},
    onSetTrafficAware: (Boolean) -> Unit = {},
    onSetAvoidTolls: (Boolean) -> Unit = {},
    onSetAvoidDangerZones: (Boolean) -> Unit = {},
    onSetShowUnavoided: (Boolean) -> Unit = {},
    onSave: (PlaceCategory) -> Unit = {},
    /** True when the route review tour should run (first route ever, tours not off). */
    tourPending: Boolean = false,
    /** False once the "no clean route" step was seen on its own, so it is not repeated. */
    noCleanStepPending: Boolean = true,
    onTourDone: (TourResult) -> Unit = {},
) {
    var tappedZone by remember { mutableStateOf<ZoneTapInfo?>(null) }
    val targets = rememberTourTargets()
    // The tour runs once a ready route is on screen, a moment after it settles.
    // A ready route, or the no-clean-route panel (its options step explains that state).
    val routeReady = !state.loading && state.route != null
    var tourShown by remember { mutableStateOf(false) }
    LaunchedEffect(tourPending, routeReady) {
        if (tourPending && routeReady) {
            kotlinx.coroutines.delay(700)
            tourShown = true
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        val route = state.route
        val selectedPoints = route?.geometry?.points().orEmpty()
        // Every alternative except the selected one, drawn faded.
        val alternatives = state.routes
            .filter { it.id != route?.id }
            .map { it.geometry.points() }
        val unavoidedPoints = if (state.showUnavoided) {
            state.unavoidedRoute?.geometry?.points().orEmpty()
        } else {
            emptyList()
        }
        // Zones now cover the whole country; only draw those near the route to stay smooth.
        val allRoutePoints = selectedPoints + alternatives.flatten()
        val visibleZones = remember(state.zones, allRoutePoints) {
            zonesNearRoute(state.zones, allRoutePoints) { it.geometry }
        }
        val visibleDangerZones = remember(state.dangerZones, allRoutePoints) {
            zonesNearRoute(state.dangerZones, allRoutePoints) { it.geometry }
        }

        DkMap(
            center = selectedPoints.firstOrNull() ?: origin ?: FALLBACK_CENTER,
            modifier = Modifier.fillMaxSize(),
            routePoints = selectedPoints,
            alternativeRoutes = alternatives,
            unavoidedRoutePoints = unavoidedPoints,
            zones = visibleZones,
            dangerZones = visibleDangerZones,
            onZoneClick = { tappedZone = it },
            routeIsClean = state.certainty != Certainty.Unverified,
            interactive = false,
        )

        // The legend explains the per-layer zone colours; shown when any zones are present.
        if (visibleZones.isNotEmpty() || visibleDangerZones.isNotEmpty()) {
            ZoneLegendButton(
                showDanger = visibleDangerZones.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(DkTheme.spacing.s4),
            )
        }

        // Tapping a zone shows its full detail. Sits above the bottom panel.
        tappedZone?.let { info ->
            ZoneInfoCard(
                info = info,
                onClose = { tappedZone = null },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(DkTheme.spacing.s4),
            )
        }

        when {
            // Nothing behind the modal while computing; the panels appear on the result.
            state.loading -> Unit
            // A missing current location is not a routing failure: we never sent a request. Say so,
            // and offer a retry (which re-attempts the fix) rather than "no clean route".
            state.errorCode == ErrorCode.NO_LOCATION ->
                LocationFailurePanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                    onRetry = onRetry,
                    onChangeDestination = onChangeDestination,
                )
            state.certainty == Certainty.Unverified || route == null ->
                RouteFailurePanel(
                    targets = targets,
                    route = route,
                    stringency = state.stringency,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                    onChangeDestination = onChangeDestination,
                    onChangeStringency = onChangeStringency,
                    onProceed = { onStart(false) },
                    canProceed = route != null,
                )

            else -> RouteReadyPanel(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
                state = state,
                route = route,
                onSelectRoute = onSelectRoute,
                onSetTrafficAware = onSetTrafficAware,
                onSetAvoidTolls = onSetAvoidTolls,
                onSetAvoidDangerZones = onSetAvoidDangerZones,
                onSetShowUnavoided = onSetShowUnavoided,
                onSave = onSave,
                onStart = onStart,
                targets = targets,
            )
        }

        if (tourShown) {
            val steps = remember {
                listOf(
                    TourStep(ROUTE_BLOCKED, R.string.tour_route_blocked_title, R.string.tour_route_blocked_body),
                    TourStep(ROUTE_ALTS, R.string.tour_route_alts_title, R.string.tour_route_alts_body),
                    TourStep(ROUTE_TOGGLES, R.string.tour_route_toggles_title, R.string.tour_route_toggles_body),
                    TourStep(ROUTE_SIM, R.string.tour_route_sim_title, R.string.tour_route_sim_body),
                    TourStep(ROUTE_STAR, R.string.tour_route_star_title, R.string.tour_route_star_body),
                ).filter { targets.has(it.target!!) && (it.target != ROUTE_BLOCKED || noCleanStepPending) }
            }
            // Nothing to show (only the no-clean step, already seen): stay pending, mark nothing.
            LaunchedEffect(steps) { if (steps.isEmpty()) tourShown = false }
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

    // The blocking modal: dismissed only on a result (loading flips false) or by Cancel.
    if (state.loading) {
        ComputingRouteDialog(
            onCancel = onCancelCompute,
            destinationName = state.destination?.title,
        )
    }
}

@Composable
private fun RouteReadyPanel(
    state: RouteReviewState,
    route: RouteResult,
    onSelectRoute: (String) -> Unit,
    onSetTrafficAware: (Boolean) -> Unit,
    onSetAvoidTolls: (Boolean) -> Unit,
    onSetAvoidDangerZones: (Boolean) -> Unit,
    onSetShowUnavoided: (Boolean) -> Unit,
    onSave: (PlaceCategory) -> Unit,
    onStart: (simulated: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    targets: TourTargets? = null,
) {
    val context = LocalContext.current
    val colors = DkTheme.colors
    var chooserOpen by remember { mutableStateOf(false) }
    fun Modifier.target(key: String) = if (targets != null) tourTarget(targets, key) else this

    // Civic bottom panel: a white surface with a hairline top edge, no shadow.
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = DkTheme.radius.lg, topEnd = DkTheme.radius.lg),
        color = colors.surface,
        border = BorderStroke(Dp.Hairline, colors.border),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(DkTheme.spacing.s4),
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s4),
        ) {
            // Every route shown here was verified against the zones, so a clean route carries no
            // badge. Only a route that passes close to a zone gets a notice.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.destination?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (state.savedToFavorites) {
                    DkBadge(
                        text = stringResource(R.string.route_saved),
                        tone = DkBadgeTone.Success,
                        icon = Icons.Filled.Favorite,
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.borderStrong),
                        modifier = Modifier.target(ROUTE_STAR),
                    ) {
                        DkIconButton(
                            icon = Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(R.string.route_save),
                            onClick = { chooserOpen = !chooserOpen },
                            tint = colors.primary,
                        )
                    }
                }
            }

            AnimatedVisibility(visible = chooserOpen && !state.savedToFavorites) {
                Column {
                    Text(
                        text = stringResource(R.string.route_save_as),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                    Row(
                        modifier = Modifier.padding(top = DkTheme.spacing.s2),
                        horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
                    ) {
                        SaveChoice(R.string.category_home, PlaceCategory.HOME, onSave) { chooserOpen = false }
                        SaveChoice(R.string.category_work, PlaceCategory.WORK, onSave) { chooserOpen = false }
                        SaveChoice(R.string.route_save_other, PlaceCategory.OTHER, onSave) { chooserOpen = false }
                    }
                }
            }

            if (state.certainty == Certainty.ZoneNearby) {
                val visual = certaintyVisual(Certainty.ZoneNearby)
                Column(verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s1)) {
                    CertaintyBadge(Certainty.ZoneNearby)
                    Text(
                        text = stringResource(visual.bodyRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s1)) {
                DkStatTable(
                    stats = listOf(
                        DkStat(stringResource(R.string.route_duration), Formatters.duration(context, route.durationSeconds)),
                        DkStat(stringResource(R.string.route_distance), Formatters.distance(context, route.distanceMeters)),
                        DkStat(stringResource(R.string.route_eta), Formatters.arrivalTime(route.durationSeconds)),
                    ),
                )
                // The real avoidance cost: chosen route versus the plain unavoided route. Only
                // shown when the server returned an unavoided route to compare against.
                state.unavoidedRoute?.let { unavoided ->
                    val delta = route.durationSeconds - unavoided.durationSeconds
                    val cost = Formatters.avoidanceCostByDelta(context, delta)
                    Text(
                        text = cost ?: stringResource(R.string.route_avoidance_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                }
            }

            // Up to three alternatives as Civic list rows in one bordered box. Tapping one selects
            // it. The extra time is route choice, never avoidance, so it reads neutrally.
            if (state.routes.size > 1) {
                val fastestSeconds = state.routes.minOf { it.durationSeconds }
                Column(verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2)) {
                    Text(
                        text = stringResource(R.string.route_alternatives),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                    Surface(
                        shape = RoundedCornerShape(DkTheme.radius.md),
                        color = colors.surface,
                        border = BorderStroke(Dp.Hairline, colors.border),
                        modifier = Modifier.target(ROUTE_ALTS),
                    ) {
                        Column {
                            state.routes.take(3).forEachIndexed { index, candidate ->
                                if (index > 0) HorizontalDivider(thickness = Dp.Hairline, color = colors.border)
                                RouteChoiceRow(
                                    route = candidate,
                                    selected = candidate.id == route.id,
                                    deltaToFastestSeconds = candidate.durationSeconds - fastestSeconds,
                                    onClick = { onSelectRoute(candidate.id) },
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.target(ROUTE_TOGGLES)) {
                // Toggling this re-prices the trip; the server returns the new durationSeconds.
                DkSwitchRow(
                    label = stringResource(R.string.route_traffic_aware),
                    checked = state.trafficAware,
                    onCheckedChange = onSetTrafficAware,
                )
                HorizontalDivider(thickness = Dp.Hairline, color = colors.border)
                // Avoiding tolls changes the route, so it recomputes.
                DkSwitchRow(
                    label = stringResource(R.string.route_avoid_tolls),
                    checked = state.avoidTolls,
                    onCheckedChange = onSetAvoidTolls,
                )
                HorizontalDivider(thickness = Dp.Hairline, color = colors.border)
                // Danger zones (Area A). On by default; recomputes on change.
                DkSwitchRow(
                    label = stringResource(R.string.route_avoid_danger_zones),
                    checked = state.avoidDangerZones,
                    onCheckedChange = onSetAvoidDangerZones,
                )
                // Only offered when the server actually returned an unavoided route.
                if (state.unavoidedRoute != null) {
                    HorizontalDivider(thickness = Dp.Hairline, color = colors.border)
                    DkSwitchRow(
                        label = stringResource(R.string.route_show_unavoided),
                        checked = state.showUnavoided,
                        onCheckedChange = onSetShowUnavoided,
                    )
                    AnimatedVisibility(visible = state.showUnavoided) {
                        Text(
                            text = stringResource(R.string.route_unavoided_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.danger,
                            modifier = Modifier.padding(horizontal = DkTheme.spacing.s4),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2)) {
                DkButton(
                    text = stringResource(R.string.route_start),
                    onClick = { onStart(false) },
                    large = true,
                    modifier = Modifier.weight(1f),
                )
                DkButton(
                    text = stringResource(R.string.route_simulate),
                    onClick = { onStart(true) },
                    variant = DkButtonVariant.Secondary,
                    large = true,
                    modifier = Modifier.target(ROUTE_SIM),
                )
            }
        }
    }
}

/**
 * A selectable alternative as a Civic list row: radio, a neutral time label (the fastest reads
 * "fastest", the rest "+X min" relative to it: route choice, never avoidance), and duration and
 * distance. Only a route that could not be verified is marked.
 */
@Composable
private fun RouteChoiceRow(
    route: RouteResult,
    selected: Boolean,
    deltaToFastestSeconds: Double,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val colors = DkTheme.colors
    val isFastest = deltaToFastestSeconds < 60.0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .background(if (selected) colors.band else colors.surface)
            .defaultMinSize(minHeight = DkTheme.touch.minTarget)
            .padding(horizontal = DkTheme.spacing.s3, vertical = DkTheme.spacing.s2),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = colors.primary,
                unselectedColor = colors.borderStrong,
            ),
        )
        Text(
            text = if (isFastest) {
                stringResource(R.string.route_label_fastest)
            } else {
                Formatters.relativeLonger(context, deltaToFastestSeconds)
            },
            style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = Formatters.duration(context, route.durationSeconds) +
                " · " + Formatters.distance(context, route.distanceMeters),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
        )
        if (!route.verifiedClean) {
            Text(
                text = stringResource(R.string.route_unverified),
                style = MaterialTheme.typography.labelSmall,
                color = colors.danger,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SaveChoice(
    labelRes: Int,
    category: PlaceCategory,
    onSave: (PlaceCategory) -> Unit,
    onPicked: () -> Unit,
) {
    DkButton(
        text = stringResource(labelRes),
        onClick = {
            onSave(category)
            onPicked()
        },
        variant = DkButtonVariant.Secondary,
        modifier = Modifier.weight(1f),
    )
}

/**
 * No clean route: a danger notice, then the three ways forward as Civic list rows. Driving the
 * blocked route anyway asks for an explicit confirmation first; zone alerts stay active on it.
 */
@Composable
private fun RouteFailurePanel(
    route: RouteResult?,
    stringency: Stringency,
    onChangeDestination: () -> Unit,
    onChangeStringency: () -> Unit,
    onProceed: () -> Unit,
    canProceed: Boolean,
    modifier: Modifier = Modifier,
    targets: TourTargets? = null,
) {
    val context = LocalContext.current
    val colors = DkTheme.colors
    var confirmProceed by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = DkTheme.radius.lg, topEnd = DkTheme.radius.lg),
        color = colors.surface,
        border = BorderStroke(Dp.Hairline, colors.border),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(DkTheme.spacing.s4),
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s4),
        ) {
            // Civic notice: one emphasized line and one sentence, on the soft danger fill.
            Surface(
                shape = RoundedCornerShape(DkTheme.radius.md),
                color = colors.dangerSoft,
                border = BorderStroke(Dp.Hairline, colors.danger.copy(alpha = 0.3f)),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
                    modifier = Modifier.padding(DkTheme.spacing.s4),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(DkTheme.spacing.s6),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.route_failure_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.danger,
                        )
                        Text(
                            text = stringResource(R.string.route_failure_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textBody,
                        )
                    }
                }
            }

            Column {
                DkSectionTitle(stringResource(R.string.route_failure_options_title))
                DkListBox(modifier = if (targets != null) Modifier.tourTarget(targets, ROUTE_BLOCKED) else Modifier) {
                    DkListRow(
                        title = stringResource(R.string.route_failure_change_destination),
                        icon = Icons.Filled.Search,
                        onClick = onChangeDestination,
                    )
                    DkListDivider()
                    DkListRow(
                        title = stringResource(R.string.route_failure_change_stringency),
                        supporting = stringResource(
                            R.string.route_failure_current_level,
                            stringResource(stringencyTitleRes(stringency)),
                        ),
                        icon = Icons.Outlined.Shield,
                        onClick = onChangeStringency,
                    )
                    if (canProceed) {
                        DkListDivider()
                        DkListRow(
                            title = stringResource(R.string.route_failure_proceed),
                            supporting = stringResource(R.string.route_failure_proceed_hint),
                            icon = Icons.Filled.Warning,
                            chipTone = DkChipTone.Danger,
                            onClick = { confirmProceed = true },
                        )
                    }
                }
            }

            if (route != null) {
                DkStatTable(
                    stats = listOf(
                        DkStat(stringResource(R.string.route_duration), Formatters.duration(context, route.durationSeconds)),
                        DkStat(stringResource(R.string.route_distance), Formatters.distance(context, route.distanceMeters)),
                        DkStat(stringResource(R.string.route_eta), Formatters.arrivalTime(route.durationSeconds)),
                    ),
                )
            }

            DkButton(
                text = stringResource(R.string.route_failure_change_destination),
                onClick = onChangeDestination,
                large = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmProceed) {
        DkConfirmDialog(
            title = stringResource(R.string.route_proceed_confirm_title),
            body = stringResource(R.string.route_proceed_confirm_body),
            confirmText = stringResource(R.string.route_proceed_confirm),
            dismissText = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = {
                confirmProceed = false
                onProceed()
            },
            onDismiss = { confirmProceed = false },
        )
    }
}

@Composable
private fun LocationFailurePanel(
    onRetry: () -> Unit,
    onChangeDestination: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DkCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(DkTheme.spacing.s4),
        color = DkTheme.colors.surfaceOverlay,
        borderColor = DkTheme.colors.warning,
    ) {
        Text(
            text = stringResource(R.string.route_location_failure_title),
            style = MaterialTheme.typography.titleLarge,
            color = DkTheme.colors.text,
        )
        Text(
            text = stringResource(R.string.route_location_failure_body),
            style = MaterialTheme.typography.bodyMedium,
            color = DkTheme.colors.textMuted,
            modifier = Modifier.padding(top = DkTheme.spacing.s2),
        )
        DkButton(
            text = stringResource(R.string.route_location_failure_retry),
            onClick = onRetry,
            large = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DkTheme.spacing.s4),
        )
        DkButton(
            text = stringResource(R.string.route_failure_change_destination),
            onClick = onChangeDestination,
            variant = DkButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val FALLBACK_CENTER = LatLng(31.7942, 35.2000)

/**
 * Zones now cover the whole country (hundreds of polygons). Draw only those whose polygon has
 * a vertex within a margin of the route's bounding box, so rendering stays smooth. Small sets
 * (the mock, or a short trip) are drawn unfiltered.
 */
private const val ZONE_MARGIN_DEG = 0.06
private const val ZONE_DRAW_ALL_BELOW = 40

private fun <T> zonesNearRoute(
    features: List<T>,
    routePoints: List<LatLng>,
    geometry: (T) -> com.abaye.derechkohanim.data.model.GeoJsonPolygon,
): List<T> {
    if (features.size <= ZONE_DRAW_ALL_BELOW || routePoints.isEmpty()) return features
    val minLat = routePoints.minOf { it.lat } - ZONE_MARGIN_DEG
    val maxLat = routePoints.maxOf { it.lat } + ZONE_MARGIN_DEG
    val minLng = routePoints.minOf { it.lng } - ZONE_MARGIN_DEG
    val maxLng = routePoints.maxOf { it.lng } + ZONE_MARGIN_DEG
    return features.filter { feature ->
        geometry(feature).coordinates.firstOrNull().orEmpty().any { c ->
            c[1] in minLat..maxLat && c[0] in minLng..maxLng
        }
    }
}

private fun previewPlan(includeUnavoided: Boolean = true) = MockData.routePlan(
    com.abaye.derechkohanim.data.model.RouteRequest(
        origin = MockData.currentLocation,
        destination = MockData.savedPlaces.first().location,
        stringency = Stringency.TAHARAT,
        mode = com.abaye.derechkohanim.data.model.TravelMode.DRIVE,
        trafficAware = true,
        language = com.abaye.derechkohanim.data.model.Language.HE,
        avoidTolls = false,
        includeUnavoided = includeUnavoided,
    )
)

@Preview(name = "Route, verified + alternatives", showBackground = true, locale = "he")
@Composable
private fun RouteReadyPreview() {
    DerechKohanimTheme {
        val plan = previewPlan()
        RouteReviewScreen(
            state = RouteReviewState(
                routes = plan.routes,
                selectedRouteId = plan.routes.first().id,
                unavoidedRoute = plan.unavoidedRoute,
                showUnavoided = true,
                zones = MockData.zonesFor(Stringency.TAHARAT),
                dangerZones = MockData.dangerZoneFeatures,
                loading = false,
            ),
            origin = MockData.currentLocation,
            onRetry = {},
            onStart = {},
            onChangeDestination = {},
            onChangeStringency = {},
        )
    }
}

@Preview(name = "Route, computing", showBackground = true, locale = "he")
@Composable
private fun RouteComputingPreview() {
    DerechKohanimTheme {
        RouteReviewScreen(
            state = RouteReviewState(loading = true),
            origin = MockData.currentLocation,
            onRetry = {},
            onStart = {},
            onChangeDestination = {},
            onChangeStringency = {},
        )
    }
}

@Preview(name = "Route, failure", showBackground = true, locale = "he")
@Composable
private fun RouteFailurePreview() {
    DerechKohanimTheme {
        val unclean = MockData.route(
            MockData.currentLocation,
            MockData.savedPlaces.first().location,
            clean = false,
        )
        RouteReviewScreen(
            state = RouteReviewState(
                routes = listOf(unclean),
                selectedRouteId = unclean.id,
                stringency = Stringency.MACHMIRIM,
                loading = false,
            ),
            origin = MockData.currentLocation,
            onRetry = {},
            onStart = {},
            onChangeDestination = {},
            onChangeStringency = {},
        )
    }
}

internal const val ROUTE_BLOCKED = "route.blocked"
private const val ROUTE_ALTS = "route.alternatives"
private const val ROUTE_TOGGLES = "route.toggles"
private const val ROUTE_SIM = "route.simulate"
private const val ROUTE_STAR = "route.save"

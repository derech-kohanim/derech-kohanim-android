package com.abaye.derechkohanim.ui.screens.route

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.abaye.derechkohanim.ui.components.ZoneLegend
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
        onSetShowUnavoided = viewModel::setShowUnavoided,
        onSave = viewModel::saveDestination,
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
    onSetShowUnavoided: (Boolean) -> Unit = {},
    onSave: (PlaceCategory) -> Unit = {},
) {
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

        DkMap(
            center = selectedPoints.firstOrNull() ?: origin ?: FALLBACK_CENTER,
            modifier = Modifier.fillMaxSize(),
            routePoints = selectedPoints,
            alternativeRoutes = alternatives,
            unavoidedRoutePoints = unavoidedPoints,
            zones = state.zones,
            routeIsClean = state.certainty != Certainty.Unverified,
            interactive = false,
        )

        // The legend explains the per-layer zone colours; only shown when zones are present.
        if (state.zones.isNotEmpty()) {
            ZoneLegend(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(DkTheme.spacing.s4),
            )
        }

        when {
            // Nothing behind the modal while computing; the panels appear on the result.
            state.loading -> Unit
            state.certainty == Certainty.Unverified || route == null ->
                RouteFailurePanel(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onChangeDestination = onChangeDestination,
                    onChangeStringency = onChangeStringency,
                    onProceed = { onStart(false) },
                    canProceed = route != null,
                )

            else -> RouteReadyPanel(
                modifier = Modifier.align(Alignment.BottomCenter),
                state = state,
                route = route,
                onSelectRoute = onSelectRoute,
                onSetTrafficAware = onSetTrafficAware,
                onSetAvoidTolls = onSetAvoidTolls,
                onSetShowUnavoided = onSetShowUnavoided,
                onSave = onSave,
                onStart = onStart,
            )
        }
    }

    // The blocking modal: dismissed only on a result (loading flips false) or by Cancel.
    if (state.loading) {
        ComputingRouteDialog(onCancel = onCancelCompute)
    }
}

@Composable
private fun RouteReadyPanel(
    state: RouteReviewState,
    route: RouteResult,
    onSelectRoute: (String) -> Unit,
    onSetTrafficAware: (Boolean) -> Unit,
    onSetAvoidTolls: (Boolean) -> Unit,
    onSetShowUnavoided: (Boolean) -> Unit,
    onSave: (PlaceCategory) -> Unit,
    onStart: (simulated: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val certainty = state.certainty
    val visual = certaintyVisual(certainty)
    var chooserOpen by remember { mutableStateOf(false) }

    DkCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(DkTheme.spacing.s4)
            .verticalScroll(rememberScrollState()),
        color = DkTheme.colors.surfaceOverlay,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CertaintyBadge(certainty)
            Spacer(Modifier.weight(1f))
            // Save-to-favourites. Once saved, the control reflects it and stops prompting.
            if (state.savedToFavorites) {
                DkBadge(
                    text = stringResource(R.string.route_saved),
                    tone = DkBadgeTone.Success,
                    icon = Icons.Filled.Favorite,
                )
            } else {
                DkButton(
                    text = stringResource(R.string.route_save),
                    onClick = { chooserOpen = !chooserOpen },
                    variant = DkButtonVariant.Secondary,
                    leadingIcon = Icons.Filled.FavoriteBorder,
                )
            }
        }

        AnimatedVisibility(visible = chooserOpen && !state.savedToFavorites) {
            Column(modifier = Modifier.padding(top = DkTheme.spacing.s2)) {
                Text(
                    text = stringResource(R.string.route_save_as),
                    style = MaterialTheme.typography.labelMedium,
                    color = DkTheme.colors.textMuted,
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

        // Up to three alternatives. Tapping one selects it - the map highlights it and
        // navigation will use its id. The extra time between alternatives is route choice,
        // never "avoidance", so it is shown neutrally relative to the fastest.
        if (state.routes.size > 1) {
            val fastestSeconds = state.routes.minOf { it.durationSeconds }
            Text(
                text = stringResource(R.string.route_alternatives),
                style = MaterialTheme.typography.labelMedium,
                color = DkTheme.colors.textMuted,
                modifier = Modifier.padding(top = DkTheme.spacing.s3),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = DkTheme.spacing.s2),
                horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
            ) {
                state.routes.take(3).forEach { candidate ->
                    RouteChoiceCard(
                        route = candidate,
                        selected = candidate.id == route.id,
                        deltaToFastestSeconds = candidate.durationSeconds - fastestSeconds,
                        onClick = { onSelectRoute(candidate.id) },
                    )
                }
            }
        }

        Text(
            text = stringResource(visual.bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = DkTheme.colors.textMuted,
            modifier = Modifier.padding(top = DkTheme.spacing.s3),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DkTheme.spacing.s3),
            horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s4),
        ) {
            Stat(
                label = stringResource(R.string.route_duration),
                value = Formatters.duration(context, route.durationSeconds),
            )
            Stat(
                label = stringResource(R.string.route_distance),
                value = Formatters.distance(context, route.distanceMeters),
            )
            Stat(
                label = stringResource(R.string.route_eta),
                value = Formatters.arrivalTime(route.durationSeconds),
            )
        }

        // The real avoidance cost: chosen route versus the plain unavoided route. Only shown
        // when the server returned an unavoided route to compare against.
        state.unavoidedRoute?.let { unavoided ->
            val delta = route.durationSeconds - unavoided.durationSeconds
            val cost = Formatters.avoidanceCostByDelta(context, delta)
            Text(
                text = cost ?: stringResource(R.string.route_avoidance_none),
                style = MaterialTheme.typography.bodyMedium,
                color = if (cost != null) DkTheme.colors.warning else DkTheme.colors.textMuted,
                modifier = Modifier.padding(top = DkTheme.spacing.s3),
            )
        }

        // Toggling this re-prices the trip; the server returns the new durationSeconds.
        DkSwitchRow(
            label = stringResource(R.string.route_traffic_aware),
            checked = state.trafficAware,
            onCheckedChange = onSetTrafficAware,
        )
        // Avoiding tolls changes the route, so it recomputes.
        DkSwitchRow(
            label = stringResource(R.string.route_avoid_tolls),
            checked = state.avoidTolls,
            onCheckedChange = onSetAvoidTolls,
        )
        // Only offered when the server actually returned an unavoided route.
        if (state.unavoidedRoute != null) {
            DkSwitchRow(
                label = stringResource(R.string.route_show_unavoided),
                checked = state.showUnavoided,
                onCheckedChange = onSetShowUnavoided,
            )
            AnimatedVisibility(visible = state.showUnavoided) {
                Text(
                    text = stringResource(R.string.route_unavoided_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = DkTheme.colors.warning,
                    modifier = Modifier.padding(top = DkTheme.spacing.s1),
                )
            }
        }

        DkButton(
            text = stringResource(R.string.route_start),
            onClick = { onStart(false) },
            large = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DkTheme.spacing.s2),
        )
        DkButton(
            text = stringResource(R.string.route_simulate),
            onClick = { onStart(true) },
            variant = DkButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * A selectable alternative: a neutral time indicator (the fastest reads "fastest", the rest
 * read "+Xmin" relative to it - this is route choice, never avoidance), distance/duration,
 * and a verified-clean badge.
 */
@Composable
private fun RouteChoiceCard(
    route: RouteResult,
    selected: Boolean,
    deltaToFastestSeconds: Double,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val isFastest = deltaToFastestSeconds < 60.0
    DkCard(
        modifier = Modifier
            .widthIn(min = 140.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        color = if (selected) DkTheme.colors.surfaceMuted else DkTheme.colors.surface,
        borderColor = if (selected) DkTheme.colors.primary else DkTheme.colors.border,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isFastest) {
                    stringResource(R.string.route_label_fastest)
                } else {
                    Formatters.relativeLonger(context, deltaToFastestSeconds)
                },
                style = MaterialTheme.typography.titleSmall,
                color = DkTheme.colors.text,
                modifier = Modifier.weight(1f),
            )
            if (route.verifiedClean) {
                DkBadge(
                    text = stringResource(R.string.route_verified_short),
                    tone = DkBadgeTone.Success,
                    icon = Icons.Filled.CheckCircle,
                )
            }
        }
        Text(
            text = Formatters.duration(context, route.durationSeconds) +
                "  ·  " + Formatters.distance(context, route.distanceMeters),
            style = MaterialTheme.typography.bodySmall,
            color = DkTheme.colors.textMuted,
            modifier = Modifier.padding(top = DkTheme.spacing.s1),
        )
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

@Composable
private fun androidx.compose.foundation.layout.RowScope.Stat(label: String, value: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = DkTheme.colors.text,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = DkTheme.colors.textMuted,
        )
    }
}

@Composable
private fun RouteFailurePanel(
    onChangeDestination: () -> Unit,
    onChangeStringency: () -> Unit,
    onProceed: () -> Unit,
    canProceed: Boolean,
    modifier: Modifier = Modifier,
) {
    DkCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(DkTheme.spacing.s4),
        color = DkTheme.colors.surfaceOverlay,
        borderColor = DkTheme.colors.danger,
    ) {
        CertaintyBadge(Certainty.Unverified)
        Text(
            text = stringResource(R.string.route_failure_title),
            style = MaterialTheme.typography.titleLarge,
            color = DkTheme.colors.text,
            modifier = Modifier.padding(top = DkTheme.spacing.s2),
        )
        Text(
            text = stringResource(R.string.route_failure_body),
            style = MaterialTheme.typography.bodyMedium,
            color = DkTheme.colors.textMuted,
            modifier = Modifier.padding(top = DkTheme.spacing.s2),
        )
        DkButton(
            text = stringResource(R.string.route_failure_change_destination),
            onClick = onChangeDestination,
            large = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DkTheme.spacing.s4),
        )
        DkButton(
            text = stringResource(R.string.route_failure_change_stringency),
            onClick = onChangeStringency,
            variant = DkButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        if (canProceed) {
            DkButton(
                text = stringResource(R.string.route_failure_proceed),
                onClick = onProceed,
                variant = DkButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val FALLBACK_CENTER = LatLng(31.7942, 35.2000)

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

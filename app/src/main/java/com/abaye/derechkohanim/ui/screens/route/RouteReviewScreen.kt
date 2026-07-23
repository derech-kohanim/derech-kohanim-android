package com.abaye.derechkohanim.ui.screens.route

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.tooling.preview.Preview
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
        onSetTrafficAware = viewModel::setTrafficAware,
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
    onSetTrafficAware: (Boolean) -> Unit = {},
    onSave: (PlaceCategory) -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val route = state.route
        DkMap(
            center = route?.geometry?.points()?.firstOrNull() ?: origin ?: FALLBACK_CENTER,
            modifier = Modifier.fillMaxSize(),
            routePoints = route?.geometry?.points().orEmpty(),
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
                route = route,
                certainty = state.certainty,
                trafficAware = state.trafficAware,
                savedToFavorites = state.savedToFavorites,
                onSetTrafficAware = onSetTrafficAware,
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
    route: RouteResult,
    certainty: Certainty,
    trafficAware: Boolean,
    savedToFavorites: Boolean,
    onSetTrafficAware: (Boolean) -> Unit,
    onSave: (PlaceCategory) -> Unit,
    onStart: (simulated: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val visual = certaintyVisual(certainty)
    var chooserOpen by remember { mutableStateOf(false) }

    DkCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(DkTheme.spacing.s4),
        color = DkTheme.colors.surfaceOverlay,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CertaintyBadge(certainty)
            Spacer(Modifier.weight(1f))
            // Save-to-favourites. Once saved, the control reflects it and stops prompting.
            if (savedToFavorites) {
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

        AnimatedVisibility(visible = chooserOpen && !savedToFavorites) {
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

        Text(
            text = stringResource(visual.bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = DkTheme.colors.textMuted,
            modifier = Modifier.padding(top = DkTheme.spacing.s2),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DkTheme.spacing.s4),
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

        Formatters.avoidanceCost(context, route.durationSeconds, route.detourRatio)?.let { cost ->
            Text(
                text = cost,
                style = MaterialTheme.typography.bodyMedium,
                color = DkTheme.colors.warning,
                modifier = Modifier.padding(top = DkTheme.spacing.s3),
            )
        }

        // Toggling this re-prices the trip; the server returns the new durationSeconds.
        DkSwitchRow(
            label = stringResource(R.string.route_traffic_aware),
            checked = trafficAware,
            onCheckedChange = onSetTrafficAware,
        )

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

@Preview(name = "Route, verified", showBackground = true, locale = "he")
@Composable
private fun RouteReadyPreview() {
    DerechKohanimTheme {
        RouteReviewScreen(
            state = RouteReviewState(
                route = MockData.route(MockData.currentLocation, MockData.savedPlaces.first().location),
                zones = MockData.zonesFor(com.abaye.derechkohanim.data.model.Stringency.TAHARAT),
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
        RouteReviewScreen(
            state = RouteReviewState(
                route = MockData.route(
                    MockData.currentLocation,
                    MockData.savedPlaces.first().location,
                    clean = false,
                ),
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

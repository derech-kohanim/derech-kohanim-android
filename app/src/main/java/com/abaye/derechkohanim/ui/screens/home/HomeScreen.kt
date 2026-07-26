package com.abaye.derechkohanim.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.data.mock.MockData
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.SavedPlace
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.toSuggestion
import com.abaye.derechkohanim.service.rememberLocationPermissionState
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.ui.components.DkBadge
import com.abaye.derechkohanim.ui.components.DkFloatingIconButton
import com.abaye.derechkohanim.ui.components.DkMap
import com.abaye.derechkohanim.ui.components.MapControls
import com.abaye.derechkohanim.ui.components.StatusBanner
import com.abaye.derechkohanim.ui.components.ZoneInfoCard
import com.abaye.derechkohanim.ui.components.ZoneTapInfo
import com.abaye.derechkohanim.ui.components.rememberDkMapController
import com.abaye.derechkohanim.ui.icons.categoryIcon
import com.abaye.derechkohanim.ui.screens.onboarding.stringencyTitleRes
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme

@Composable
fun HomeRoute(
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onDestinationChosen: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onOpenSearch = onOpenSearch,
        onOpenSettings = onOpenSettings,
        onLocateMe = { viewModel.locateMe() },
        onSavedPlaceChosen = { place ->
            viewModel.chooseDestination(place.toSuggestion())
            onDestinationChosen()
        },
        mapUpdates = viewModel.mapUpdates,
    )
}

/**
 * Planning screen. Spec 17.5 asks for three things only: where to, the stringency level, and
 * one obvious way to start. Browsing the map is not the product.
 */
@Composable
fun HomeScreen(
    state: HomeState,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onSavedPlaceChosen: (SavedPlace) -> Unit,
    onLocateMe: suspend () -> LatLng? = { null },
    mapUpdates: kotlinx.coroutines.flow.SharedFlow<com.abaye.derechkohanim.data.repository.ZoneLayerChange>? = null,
) {
    val controller = rememberDkMapController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // The permission launcher needs an activity, which a @Preview does not have.
    val permissions = if (LocalInspectionMode.current) null else rememberLocationPermissionState(context)

    // A tapped zone/danger polygon, shown as a detail card until dismissed.
    var tappedZone by remember { mutableStateOf<ZoneTapInfo?>(null) }

    // A brief, unobtrusive banner when a launch sync altered the map layers. Cleared after a moment.
    var layerUpdate by remember { mutableStateOf<com.abaye.derechkohanim.data.repository.ZoneLayerChange?>(null) }
    LaunchedEffect(mapUpdates) {
        mapUpdates?.collect { change ->
            layerUpdate = change
            kotlinx.coroutines.delay(4000)
            layerUpdate = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DkMap(
            center = state.origin ?: FALLBACK_CENTER,
            controller = controller,
            currentLocation = state.origin,
            zones = state.zones,
            dangerZones = state.dangerZones,
            onZoneClick = { tappedZone = it },
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                // The map draws edge-to-edge behind the status bar, but the controls must sit
                // below it so they never overlap the clock/battery.
                .statusBarsPadding()
                .padding(DkTheme.spacing.s4),
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
            ) {
                SearchLauncher(
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSearch,
                )
                DkFloatingIconButton(
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    onClick = onOpenSettings,
                )
            }

            DkBadge(text = stringResource(stringencyTitleRes(state.stringency)))

            // Waze-style connectivity banner at the top of the home view.
            StatusBanner(offline = state.offline, noGps = false)

            // Brief "map points updated" banner after a background layer sync applied changes.
            androidx.compose.animation.AnimatedVisibility(visible = layerUpdate != null) {
                val change = layerUpdate
                if (change != null) {
                    val text = if (change.upserted > 0) {
                        stringResource(R.string.map_layer_updated, change.upserted)
                    } else {
                        stringResource(R.string.map_layer_removed, change.removed)
                    }
                    DkBadge(
                        text = text,
                        tone = com.abaye.derechkohanim.ui.components.DkBadgeTone.Info,
                        icon = Icons.Filled.Place,
                    )
                }
            }
        }

        MapControls(
            onRecenter = {
                // Ask for location on first use, then centre. Denial just leaves the map put.
                if (permissions == null || permissions.hasForeground) {
                    scope.launch { onLocateMe()?.let { controller.center(it) } }
                } else {
                    permissions.askForeground()
                }
            },
            onZoomIn = { controller.zoomBy(1f) },
            onZoomOut = { controller.zoomBy(-1f) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(DkTheme.spacing.s4),
        )

        // Tapping a zone/danger polygon shows its full detail, above the other overlays.
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

        if (state.savedPlaces.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(DkTheme.spacing.s4),
                horizontalAlignment = Alignment.Start,
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2)) {
                    items(state.savedPlaces, key = { it.id }) { place ->
                        SavedPlaceChip(place = place, onClick = { onSavedPlaceChosen(place) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchLauncher(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier,
        // Search entry is an interactive control: a full pill, matching DkSearchField.
        shape = androidx.compose.foundation.shape.RoundedCornerShape(DkTheme.radius.full),
        color = DkTheme.colors.surfaceOverlay,
        border = androidx.compose.foundation.BorderStroke(
            androidx.compose.ui.unit.Dp.Hairline,
            DkTheme.colors.border,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = DkTheme.spacing.s4)
                .fillMaxWidth()
                .padding(vertical = DkTheme.spacing.s4),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = DkTheme.colors.textMuted,
            )
            Text(
                text = stringResource(R.string.home_search_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = DkTheme.colors.textMuted,
                modifier = Modifier.padding(start = DkTheme.spacing.s3),
            )
        }
    }
}

@Composable
private fun SavedPlaceChip(place: SavedPlace, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(DkTheme.radius.full),
        color = DkTheme.colors.surfaceOverlay,
        contentColor = DkTheme.colors.text,
        border = androidx.compose.foundation.BorderStroke(
            androidx.compose.ui.unit.Dp.Hairline,
            DkTheme.colors.border,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = DkTheme.spacing.s4,
                vertical = DkTheme.spacing.s3,
            ),
        ) {
            Icon(imageVector = categoryIcon(place.category), contentDescription = null)
            Text(
                text = place.label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = DkTheme.spacing.s2),
            )
        }
    }
}

private val FALLBACK_CENTER = LatLng(31.7942, 35.2000)

@Preview(name = "Home", showBackground = true, locale = "he")
@Composable
private fun HomePreview() {
    DerechKohanimTheme {
        HomeScreen(
            state = HomeState(
                origin = MockData.currentLocation,
                stringency = Stringency.TAHARAT,
                savedPlaces = MockData.savedPlaces,
            ),
            onOpenSearch = {},
            onOpenSettings = {},
            onSavedPlaceChosen = {},
        )
    }
}

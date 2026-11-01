package com.abaye.derechkohanim.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.layout.size
import com.abaye.derechkohanim.data.local.Tour
import com.abaye.derechkohanim.data.model.ZoneFeature
import com.abaye.derechkohanim.ui.components.CoachMarkOverlay
import com.abaye.derechkohanim.ui.components.TourStep
import com.abaye.derechkohanim.ui.components.TourViewModel
import com.abaye.derechkohanim.ui.components.rememberTourTargets
import com.abaye.derechkohanim.ui.components.toTapInfo
import com.abaye.derechkohanim.ui.components.tourTarget
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.core.ZoneFilter
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
    onOpenSearchForOrigin: () -> Unit = {},
    originJustSet: Boolean = false,
    onOriginConfirmConsumed: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tourViewModel: TourViewModel = hiltViewModel()
    val tourPending by tourViewModel.pending(Tour.HOME).collectAsStateWithLifecycle()
    val context = LocalContext.current
    // The foreground GPS watchdog: only ticks while Home is RESUMED, and is cancelled the moment
    // Home is backgrounded or left, restarting on return. The 10s/5s cadence lives in the VM loop.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.watchLocation()
        }
    }
    HomeScreen(
        state = state,
        onOpenSearch = onOpenSearch,
        onOpenSettings = onOpenSettings,
        onLocateMe = { viewModel.locateMe() },
        onSetManualOrigin = { viewModel.setManualOrigin(it) },
        onViewportChange = { viewModel.setViewport(it) },
        onSearchOriginAddress = onOpenSearchForOrigin,
        onSetStringency = { viewModel.setStringency(it) },
        originJustSet = originJustSet,
        onOriginConfirmConsumed = onOriginConfirmConsumed,
        onSavedPlaceChosen = { place ->
            viewModel.chooseDestination(place.toSuggestion())
            onDestinationChosen()
        },
        mapUpdates = viewModel.mapUpdates,
        tourPending = tourPending == true,
        onTourDone = { _, never ->
            tourViewModel.finish(Tour.HOME, never)
            if (never) Toast.makeText(context, R.string.tour_off_toast, Toast.LENGTH_SHORT).show()
        },
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
    onSetManualOrigin: (LatLng) -> Unit = {},
    /** The box the map camera settled on, so the drawn zones follow the pan/zoom rather than the origin. */
    onViewportChange: (ZoneFilter.GeoBounds) -> Unit = {},
    onSearchOriginAddress: () -> Unit = {},
    onSetStringency: (Stringency) -> Unit = {},
    originJustSet: Boolean = false,
    onOriginConfirmConsumed: () -> Unit = {},
    mapUpdates: kotlinx.coroutines.flow.SharedFlow<com.abaye.derechkohanim.data.repository.ZoneLayerChange>? = null,
    /** True when the home tour should run (first visit after onboarding, tours not off). */
    tourPending: Boolean = false,
    onTourDone: (skipped: Boolean, never: Boolean) -> Unit = { _, _ -> },
) {
    val controller = rememberDkMapController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // The permission launcher needs an activity, which a @Preview does not have.
    val permissions = if (LocalInspectionMode.current) null else rememberLocationPermissionState(context)

    val targets = rememberTourTargets()
    // The tour starts a moment after the screen settles, so every target has been measured.
    var tourShown by remember { mutableStateOf(false) }
    LaunchedEffect(tourPending) {
        if (tourPending) {
            kotlinx.coroutines.delay(TOUR_START_DELAY_MS)
            tourShown = true
        }
    }

    // The avoidance level picker, opened from the chip under the search row.
    var pickStringency by remember { mutableStateOf(false) }

    // A tapped zone/danger polygon, shown as a detail card until dismissed.
    var tappedZone by remember { mutableStateOf<ZoneTapInfo?>(null) }

    // Manual start-point entry, for when there is no GPS (NetFree indoors). While pick-start mode is
    // on, the next map tap sets the origin. A brief confirmation shows once a point is picked.
    var pickStart by remember { mutableStateOf(false) }
    var manualConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(manualConfirm) {
        if (manualConfirm) {
            kotlinx.coroutines.delay(3000)
            manualConfirm = false
        }
    }
    // Once a fix returns there is no reason to keep offering manual entry; drop the mode.
    LaunchedEffect(state.noGps) {
        if (!state.noGps) pickStart = false
    }
    // An origin picked via address search (returning from the search screen) confirms the same way
    // as a map-tapped one: show the brief "start location set" badge and drop the pick-start hint.
    LaunchedEffect(originJustSet) {
        if (originJustSet) {
            pickStart = false
            manualConfirm = true
            onOriginConfirmConsumed()
        }
    }

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
            onMapClick = { latLng ->
                // Only a tap while in pick-start mode sets the origin; otherwise the map is inert.
                if (pickStart) {
                    onSetManualOrigin(latLng)
                    pickStart = false
                    manualConfirm = true
                }
            },
            onVisibleBoundsChange = onViewportChange,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                // The map draws edge-to-edge behind the status bar, but the controls must sit
                // below it so they never overlap the clock/battery.
                .statusBarsPadding()
                .padding(DkTheme.spacing.s2),
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
            ) {
                SearchLauncher(
                    modifier = Modifier
                        .weight(1f)
                        .tourTarget(targets, TARGET_SEARCH),
                    onClick = onOpenSearch,
                )
                DkFloatingIconButton(
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    onClick = onOpenSettings,
                    modifier = Modifier.tourTarget(targets, TARGET_ACCOUNT),
                )
            }

            // The avoidance level (tap to change) and, at the far edge, the folded zone legend.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                com.abaye.derechkohanim.ui.components.StringencyChip(
                    stringency = state.stringency,
                    onClick = { pickStringency = true },
                    modifier = Modifier.tourTarget(targets, TARGET_PILL),
                )
            }

            // Waze-style connectivity banner at the top of the home view.
            StatusBanner(offline = state.offline, noGps = state.noGps)

            // With no GPS, let the user set the start point by hand so planning still works. Two
            // ways are offered: search an address (needs connectivity) or tap the map (fully
            // offline). Arming map-tap swaps the buttons for a hint to tap the map.
            androidx.compose.animation.AnimatedVisibility(visible = state.noGps) {
                if (pickStart) {
                    DkBadge(
                        text = stringResource(R.string.home_manual_origin_hint),
                        tone = com.abaye.derechkohanim.ui.components.DkBadgeTone.Info,
                        icon = Icons.Filled.Place,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2)) {
                        Text(
                            text = stringResource(R.string.home_manual_origin),
                            style = MaterialTheme.typography.labelLarge,
                            color = DkTheme.colors.text,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2)) {
                            com.abaye.derechkohanim.ui.components.DkButton(
                                text = stringResource(R.string.home_manual_origin_search),
                                onClick = onSearchOriginAddress,
                                variant = com.abaye.derechkohanim.ui.components.DkButtonVariant.Secondary,
                                leadingIcon = Icons.Filled.Search,
                                modifier = Modifier.weight(1f),
                            )
                            com.abaye.derechkohanim.ui.components.DkButton(
                                text = stringResource(R.string.home_manual_origin_map),
                                onClick = { pickStart = true },
                                variant = com.abaye.derechkohanim.ui.components.DkButtonVariant.Secondary,
                                leadingIcon = Icons.Filled.Place,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // Brief confirmation once a manual start point has been set.
            androidx.compose.animation.AnimatedVisibility(visible = manualConfirm) {
                DkBadge(
                    text = stringResource(R.string.home_manual_origin_set),
                    tone = com.abaye.derechkohanim.ui.components.DkBadgeTone.Success,
                    icon = Icons.Filled.Place,
                )
            }

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
            locateModifier = Modifier.tourTarget(targets, TARGET_LOCATE),
        )

        // The map area the zone step points at: the middle of the map, where the zones are.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(180.dp)
                .tourTarget(targets, TARGET_ZONE),
        )


        if (pickStringency) {
            com.abaye.derechkohanim.ui.components.StringencyDialog(
                current = state.stringency,
                onSave = {
                    onSetStringency(it)
                    pickStringency = false
                },
                onDismiss = { pickStringency = false },
            )
        }

        // The folded legend, at the bottom start of the map (the right edge in Hebrew), clear of
        // the avoidance pill at the top and the zoom/locate column on the other edge. It sits
        // above the saved-place chips when there are any.
        com.abaye.derechkohanim.ui.components.ZoneLegendButton(
            showDanger = state.dangerZones.isNotEmpty(),
            modifier = Modifier
                .tourTarget(targets, TARGET_LEGEND)
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(
                    start = DkTheme.spacing.s2,
                    bottom = if (state.savedPlaces.isNotEmpty()) DkTheme.spacing.s16 + DkTheme.spacing.s4 else DkTheme.spacing.s4,
                ),
        )

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

        if (tourShown) {
            val steps = remember {
                homeTourSteps(
                    onTryZone = {
                        // Open the card of the visible zone nearest the start point, like a real tap.
                        nearestZone(state.zones, state.origin)?.let { tappedZone = it.properties.toTapInfo() }
                    },
                ).filter { it.target == null || targets.has(it.target) }
            }
            CoachMarkOverlay(
                steps = steps,
                targets = targets,
                onStepChange = { tappedZone = null },
                onDone = { skipped, never ->
                    tourShown = false
                    tappedZone = null
                    onTourDone(skipped, never)
                },
            )
        }

        // Tapping a zone/danger polygon shows its full detail, above the other overlays.
        tappedZone?.let { info ->
            ZoneInfoCard(
                info = info,
                onClose = { tappedZone = null },
                modifier = Modifier
                    // At the top: during the tour the coach card sits under the map's middle.
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(DkTheme.spacing.s4),
            )
        }
    }
}

@Composable
private fun SearchLauncher(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier,
        // Civic input: small radius, control-strength outline.
        shape = androidx.compose.foundation.shape.RoundedCornerShape(DkTheme.radius.sm),
        color = DkTheme.colors.surfaceOverlay,
        border = androidx.compose.foundation.BorderStroke(1.dp, DkTheme.colors.borderStrong),
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
                tint = DkTheme.colors.text,
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

private const val TOUR_START_DELAY_MS = 700L
private const val TARGET_SEARCH = "home.search"
private const val TARGET_ZONE = "home.zone"
private const val TARGET_LEGEND = "home.legend"
private const val TARGET_PILL = "home.pill"
private const val TARGET_LOCATE = "home.locate"
private const val TARGET_ACCOUNT = "home.account"

/** The home tour, in order. The account step points at settings, where the account lives. */
private fun homeTourSteps(onTryZone: () -> Unit) = listOf(
    TourStep(TARGET_SEARCH, R.string.tour_home_search_title, R.string.tour_home_search_body),
    TourStep(TARGET_ZONE, R.string.tour_home_zone_title, R.string.tour_home_zone_body, R.string.tour_try, onTryZone),
    TourStep(TARGET_LEGEND, R.string.tour_home_legend_title, R.string.tour_home_legend_body),
    TourStep(TARGET_PILL, R.string.tour_home_pill_title, R.string.tour_home_pill_body),
    TourStep(TARGET_LOCATE, R.string.tour_home_locate_title, R.string.tour_home_locate_body),
    TourStep(TARGET_ACCOUNT, R.string.tour_home_account_title, R.string.tour_home_account_body),
    TourStep(null, R.string.tour_home_done_title, R.string.tour_home_done_body),
)

/** The zone whose first vertex is closest to [origin]; the first one when there is no origin. */
private fun nearestZone(zones: List<ZoneFeature>, origin: LatLng?): ZoneFeature? {
    if (origin == null) return zones.firstOrNull()
    return zones.minByOrNull { zone ->
        val c = zone.geometry.coordinates.firstOrNull()?.firstOrNull() ?: return@minByOrNull Double.MAX_VALUE
        val dLat = c[1] - origin.lat
        val dLng = c[0] - origin.lng
        dLat * dLat + dLng * dLng
    }
}

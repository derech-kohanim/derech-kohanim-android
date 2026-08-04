package com.abaye.derechkohanim.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.BuildConfig
import com.abaye.derechkohanim.analytics.Analytics
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.core.ZoneFilter
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.mock.MockData
import com.abaye.derechkohanim.data.model.DangerZoneFeature
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.PlaceSuggestion
import com.abaye.derechkohanim.data.model.SavedPlace
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.ZoneFeature
import com.abaye.derechkohanim.data.repository.PlacesRepository
import com.abaye.derechkohanim.data.repository.ZoneLayerChange
import com.abaye.derechkohanim.data.repository.ZoneLayerStore
import com.abaye.derechkohanim.nav.TripStore
import com.abaye.derechkohanim.service.Connectivity
import com.abaye.derechkohanim.service.DeviceLocation
import javax.inject.Inject

data class HomeState(
    val origin: LatLng? = null,
    val stringency: Stringency = Stringency.BASIC,
    val savedPlaces: List<SavedPlace> = emptyList(),
    /** Zone polygons the map can currently show, from the local layer store (clipped for speed). */
    val zones: List<ZoneFeature> = emptyList(),
    /** Danger-zone (Area A) polygons the map can currently show, from the local layer store. */
    val dangerZones: List<DangerZoneFeature> = emptyList(),
    /** No internet-capable network right now (drives the status banner). */
    val offline: Boolean = false,
    /** No GPS/location fix right now (drives the Waze-style "no GPS" banner and manual entry). */
    val noGps: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val sessionStore: SessionStore,
    private val deviceLocation: DeviceLocation,
    private val tripStore: TripStore,
    private val connectivity: Connectivity,
    private val zoneLayerStore: ZoneLayerStore,
    private val analytics: Analytics,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    /** The origin, tracked separately so the zone filter recomputes as it changes. */
    private val originFlow = MutableStateFlow<LatLng?>(null)

    /**
     * The box the map camera currently shows, reported by [setViewport]. Null until the map has
     * reported once, which is the only window in which the origin decides what is drawn.
     */
    private val viewportFlow = MutableStateFlow<ZoneFilter.GeoBounds?>(null)

    /** Re-emits the layer store's change summary so the screen can show a brief banner. */
    val mapUpdates: SharedFlow<ZoneLayerChange> = zoneLayerStore.changes

    init {
        viewModelScope.launch {
            connectivity.online().collect { online ->
                _state.value = _state.value.copy(offline = !online)
            }
        }
        viewModelScope.launch {
            val snapshot = sessionStore.snapshot()
            val located = deviceLocation.lastKnown()
                ?: if (BuildConfig.USE_MOCK_DATA) MockData.currentLocation else null
            val saved = placesRepository.savedPlaces()
            originFlow.value = located
            _state.value = _state.value.copy(
                origin = located,
                stringency = snapshot.stringency,
                savedPlaces = (saved as? DkResult.Success)?.value.orEmpty(),
            )
        }
        // Keep Home's shown origin in step with a hand-picked start point. The map-tap path sets it
        // directly, but the address-search path stores it only in the TripStore (from the search
        // screen), so observing the store lets that origin recentre the map here too. Only non-null
        // values apply; clearing (a real GPS fix) is handled by the watchdog, which sets the fix.
        viewModelScope.launch {
            tripStore.manualOrigin.collect { manual ->
                if (manual != null) {
                    originFlow.value = manual
                    _state.value = _state.value.copy(origin = manual)
                }
            }
        }
        // Draw only the zone/danger polygons the map is actually looking at, recomputed whenever the
        // camera reports a new viewport, the origin moves, or the store's layers change (initial
        // cache load or a background sync). Before the first camera report the padded box around the
        // origin stands in, so the first paint is never empty; after it, the camera decides, which
        // is what makes panning or zooming to a distant area fill in that area's zones. The store's
        // features carry precomputed bounding boxes, so this is a box comparison per feature.
        viewModelScope.launch {
            combine(
                originFlow,
                viewportFlow,
                zoneLayerStore.zonesIndexed,
                zoneLayerStore.dangerIndexed,
            ) { origin, viewport, zones, danger ->
                val bounds = viewport
                    ?: ZoneFilter.boundsAround(origin ?: FALLBACK_CENTER, ZoneFilter.HOME_PAD_DEG)
                val fromCamera = viewport != null
                ZoneFilter.inBounds(zones, bounds, fromCamera) to
                    ZoneFilter.inBounds(danger, bounds, fromCamera)
            }.flowOn(Dispatchers.Default).collect { (zones, danger) ->
                _state.value = _state.value.copy(zones = zones, dangerZones = danger)
            }
        }
    }

    /**
     * The map reports the (padded) box its camera shows, throttled by the map component itself. From
     * the first report on, it - not the origin - decides which polygons are drawn, so the zones
     * follow the user wherever they pan or zoom instead of clinging to where the map opened.
     */
    fun setViewport(bounds: ZoneFilter.GeoBounds) {
        viewportFlow.value = bounds
    }

    fun chooseDestination(suggestion: PlaceSuggestion) {
        tripStore.setDestination(suggestion)
    }

    /**
     * The foreground GPS watchdog. Mirrors the old app's resilience: it never dead-ends on a missing
     * fix but keeps re-checking, and it drives [HomeState.noGps] so the banner (and manual-entry
     * action) appear the moment location is lost and disappear the moment it returns. It is a plain
     * suspend loop; the screen runs it under `repeatOnLifecycle(RESUMED)`, so it only ticks while
     * Home is in the foreground and is cancelled when Home is backgrounded or left.
     *
     * Each cycle does a single bounded [DeviceLocation.quickFix] (cached fix, else one capped GPS
     * request - no heavy feed fallback). With a fix: update the origin, clear noGps, and a real fix
     * wins over any hand-picked start, so it drops the manual origin; re-check every ~10s. With no
     * fix: flag noGps and re-check every ~5s so recovery is picked up quickly. In a mock build a
     * mock location seeds the map once so it is never empty.
     */
    suspend fun watchLocation() {
        while (true) {
            val fix = deviceLocation.quickFix()
            if (fix != null) {
                // A real fix wins over a manually picked start.
                tripStore.clearManualOrigin()
                originFlow.value = fix
                _state.value = _state.value.copy(origin = fix, noGps = false)
                delay(WATCH_INTERVAL_OK_MS)
            } else {
                if (BuildConfig.USE_MOCK_DATA &&
                    originFlow.value == null &&
                    tripStore.manualOrigin.value == null
                ) {
                    originFlow.value = MockData.currentLocation
                    _state.value = _state.value.copy(origin = MockData.currentLocation)
                }
                _state.value = _state.value.copy(noGps = true)
                delay(WATCH_INTERVAL_NO_GPS_MS)
            }
        }
    }

    /**
     * Sets a start point the user picked by hand on the map because there is no GPS fix. Planning
     * reads it from [TripStore]; Home also shows it as the current origin so the map recentres on it.
     */
    fun setManualOrigin(latLng: LatLng) {
        tripStore.setManualOrigin(latLng)
        originFlow.value = latLng
        _state.value = _state.value.copy(origin = latLng)
        // No coordinates in the event - just that the user set a start point by hand (no-GPS path).
        analytics.track("manual_origin_set")
    }

    /**
     * Fetches the freshest known location for the recenter button and remembers it as the
     * origin. Returns null when permission was not granted or no fix is available, so the
     * caller can fail quietly.
     */
    suspend fun locateMe(): LatLng? {
        val located = deviceLocation.lastKnown()
            ?: if (BuildConfig.USE_MOCK_DATA) MockData.currentLocation else null
        if (located != null) {
            // A forced fresh fix wins over any hand-picked start: real GPS is authoritative.
            tripStore.clearManualOrigin()
            originFlow.value = located
            _state.value = _state.value.copy(origin = located, noGps = false)
        }
        return located
    }

    private companion object {
        /** Jerusalem, matching the home screen's own map fallback centre. */
        val FALLBACK_CENTER = LatLng(31.7942, 35.2000)

        /** Re-check cadence with a fix: relaxed, to keep the origin fresh without churn. */
        const val WATCH_INTERVAL_OK_MS = 10_000L

        /** Re-check cadence with no fix: quicker, so recovery from no-GPS is picked up fast. */
        const val WATCH_INTERVAL_NO_GPS_MS = 5_000L
    }
}

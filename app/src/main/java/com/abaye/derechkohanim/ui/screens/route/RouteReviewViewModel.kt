package com.abaye.derechkohanim.ui.screens.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.BuildConfig
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.core.ErrorCode
import com.abaye.derechkohanim.core.ZoneFilter
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.mock.MockData
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.PlaceSuggestion
import com.abaye.derechkohanim.data.model.RouteRequest
import com.abaye.derechkohanim.data.model.RouteResult
import com.abaye.derechkohanim.data.model.PlaceCategory
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.TravelMode
import com.abaye.derechkohanim.data.repository.PlacesRepository
import com.abaye.derechkohanim.data.repository.RoutingRepository
import com.abaye.derechkohanim.data.repository.ZoneLayerStore
import com.abaye.derechkohanim.data.repository.toCreateSavedRequest
import com.abaye.derechkohanim.nav.TripStore
import com.abaye.derechkohanim.service.DeviceLocation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

data class RouteReviewState(
    val destination: PlaceSuggestion? = null,
    /** Up to three zone-verified alternatives from the plan, best first. */
    val routes: List<RouteResult> = emptyList(),
    /** The alternative the user picked; drives certainty, stats and navigation. */
    val selectedRouteId: String? = null,
    /** Plain provider route with no avoidance. Shown for comparison, never navigable. */
    val unavoidedRoute: RouteResult? = null,
    val showUnavoided: Boolean = false,
    val zones: List<com.abaye.derechkohanim.data.model.ZoneFeature> = emptyList(),
    val dangerZones: List<com.abaye.derechkohanim.data.model.DangerZoneFeature> = emptyList(),
    val stringency: Stringency = Stringency.BASIC,
    /** Drives whether travel time is priced with live traffic. Re-fetches on change. */
    val trafficAware: Boolean = true,
    /** Avoid toll roads. Persisted, re-computes on change. */
    val avoidTolls: Boolean = false,
    /** Avoid danger zones (Area A). On by default. Persisted, re-computes on change. */
    val avoidDangerZones: Boolean = true,
    /** True once the destination has been saved to favourites in this session. */
    val savedToFavorites: Boolean = false,
    val loading: Boolean = true,
    val errorCode: String? = null,
) {
    /** The selected route, defaulting to the first alternative. */
    val route: RouteResult?
        get() = routes.firstOrNull { it.id == selectedRouteId } ?: routes.firstOrNull()

    /** Spec 17.6: three states, and the interface must never blur them together. */
    val certainty: Certainty
        get() = route.let { r ->
            when {
                r == null -> Certainty.Unknown
                !r.verifiedClean -> Certainty.Unverified
                r.maneuvers.any { it.zone != null } -> Certainty.ZoneNearby
                else -> Certainty.VerifiedClean
            }
        }
}

enum class Certainty { VerifiedClean, ZoneNearby, Unverified, Unknown }

@HiltViewModel
class RouteReviewViewModel @Inject constructor(
    private val routingRepository: RoutingRepository,
    private val placesRepository: PlacesRepository,
    private val sessionStore: SessionStore,
    private val deviceLocation: DeviceLocation,
    private val tripStore: TripStore,
    private val zoneLayerStore: ZoneLayerStore,
) : ViewModel() {

    private val _state = MutableStateFlow(RouteReviewState())
    val state: StateFlow<RouteReviewState> = _state.asStateFlow()

    /** Emits the category that was just saved, so the screen can confirm it briefly. */
    private val _savedConfirmation = MutableSharedFlow<PlaceCategory>(extraBufferCapacity = 4)
    val savedConfirmation: SharedFlow<PlaceCategory> = _savedConfirmation.asSharedFlow()

    /** Emitted when the user cancels the computation, so the screen returns to planning. */
    private val _cancelled = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val cancelled: SharedFlow<Unit> = _cancelled.asSharedFlow()

    private var computeJob: kotlinx.coroutines.Job? = null

    init {
        compute()
    }

    fun compute() {
        // One computation at a time; a new request supersedes any in flight.
        computeJob?.cancel()
        computeJob = viewModelScope.launch {
            val destination = tripStore.destination.value
            val snapshot = sessionStore.snapshot()
            _state.value = _state.value.copy(
                destination = destination,
                stringency = snapshot.stringency,
                trafficAware = snapshot.trafficAware,
                avoidTolls = snapshot.avoidTolls,
                avoidDangerZones = snapshot.avoidDangerZones,
                loading = true,
                errorCode = null,
            )

            val target = destination?.location
            // A hand-picked start (map tap when there was no GPS) wins over the device location, so
            // planning works with no fix (NetFree indoors). Falls back to the real fix, then mock.
            val origin = tripStore.manualOrigin.value
                ?: deviceLocation.lastKnown()
                ?: if (BuildConfig.USE_MOCK_DATA) MockData.currentLocation else null
            // Distinguish "couldn't locate the device" from "no clean route": a null origin means
            // we never even sent a compute request (common on a filtered network where Google's
            // network-location service is blocked and there is no GPS fix yet), so the user needs
            // to enable/await GPS, not think every route crosses a zone.
            if (origin == null) {
                _state.value = _state.value.copy(loading = false, errorCode = ErrorCode.NO_LOCATION)
                return@launch
            }
            if (target == null) {
                _state.value = _state.value.copy(loading = false, errorCode = ErrorCode.NO_ROUTE)
                return@launch
            }

            // Zones for the chosen layer, coloured per feature by their own stringency, plus
            // the separate danger-zone (Area A) layer. Prefer the local store (cache + offline,
            // and consistent with Home/Navigation), filtered to the origin-destination corridor;
            // fall back to a direct fetch only when the store has not populated yet.
            val bounds = ZoneFilter.boundsAround(listOf(origin, target), ZoneFilter.ROUTE_PAD_DEG)
            val storeZones = zoneLayerStore.zones.value
            val storeDanger = zoneLayerStore.danger.value
            val zones = if (storeZones.isNotEmpty()) {
                bounds?.let { ZoneFilter.zonesIn(storeZones, it) } ?: storeZones
            } else {
                (routingRepository.zones(snapshot.stringency, null) as? DkResult.Success)
                    ?.value?.features.orEmpty()
            }
            val dangerZones = if (storeDanger.isNotEmpty()) {
                bounds?.let { ZoneFilter.dangerIn(storeDanger, it) } ?: storeDanger
            } else {
                (routingRepository.dangerZones(null) as? DkResult.Success)
                    ?.value?.features.orEmpty()
            }

            // No engine is set: the server picks its default (hybrid), which prices travel
            // time - with or without traffic - and returns it as durationSeconds. We always
            // ask for the unavoided route so the comparison toggle has data on demand.
            val result = routingRepository.computeRoute(
                RouteRequest(
                    origin = origin,
                    destination = target,
                    stringency = snapshot.stringency,
                    mode = TravelMode.DRIVE,
                    trafficAware = snapshot.trafficAware,
                    language = snapshot.language,
                    avoidTolls = snapshot.avoidTolls,
                    avoidDangerZones = snapshot.avoidDangerZones,
                    includeUnavoided = true,
                )
            )
            _state.value = when (result) {
                is DkResult.Success -> {
                    val plan = result.value
                    val selected = plan.routes.firstOrNull()
                    if (selected != null) tripStore.setRoute(selected)
                    _state.value.copy(
                        loading = false,
                        routes = plan.routes,
                        selectedRouteId = selected?.id,
                        unavoidedRoute = plan.unavoidedRoute,
                        zones = zones,
                        dangerZones = dangerZones,
                    )
                }

                is DkResult.Failure ->
                    _state.value.copy(
                        loading = false,
                        errorCode = result.code,
                        zones = zones,
                        dangerZones = dangerZones,
                    )
            }
        }
    }

    /**
     * Picks an alternative. Navigation uses this route's id. The unavoided route is never
     * selectable here, so navigation can never start on a zone-crossing route.
     */
    fun selectRoute(routeId: String) {
        val chosen = _state.value.routes.firstOrNull { it.id == routeId } ?: return
        tripStore.setRoute(chosen)
        _state.value = _state.value.copy(selectedRouteId = chosen.id)
    }

    /** Toggles drawing the unavoided route overlay. Data is already fetched; no recompute. */
    fun setShowUnavoided(show: Boolean) {
        _state.value = _state.value.copy(showUnavoided = show)
    }

    /** Avoiding tolls changes the route, so recompute. Persisted like the traffic toggle. */
    fun setAvoidTolls(enabled: Boolean) {
        if (_state.value.loading || enabled == _state.value.avoidTolls) return
        _state.value = _state.value.copy(avoidTolls = enabled)
        viewModelScope.launch {
            sessionStore.setAvoidTolls(enabled)
            compute()
        }
    }

    /** Avoiding danger zones changes the route, so recompute. Persisted, on by default. */
    fun setAvoidDangerZones(enabled: Boolean) {
        if (_state.value.loading || enabled == _state.value.avoidDangerZones) return
        _state.value = _state.value.copy(avoidDangerZones = enabled)
        viewModelScope.launch {
            sessionStore.setAvoidDangerZones(enabled)
            compute()
        }
    }

    /**
     * Cancels the in-flight computation for real: cancelling the coroutine cancels the
     * suspended Retrofit call underneath it. Returns to a clean planning state - no route,
     * no error - and signals the screen to leave the review.
     */
    fun cancelCompute() {
        computeJob?.cancel()
        computeJob = null
        _state.value = _state.value.copy(
            loading = false,
            errorCode = null,
            routes = emptyList(),
            selectedRouteId = null,
            unavoidedRoute = null,
        )
        _cancelled.tryEmit(Unit)
    }

    fun setStringency(stringency: Stringency) {
        if (_state.value.loading) return
        viewModelScope.launch {
            sessionStore.setStringency(stringency)
            compute()
        }
    }

    /** Flipping the traffic toggle re-prices the trip, so the route is recomputed. */
    fun setTrafficAware(enabled: Boolean) {
        if (_state.value.loading || enabled == _state.value.trafficAware) return
        _state.value = _state.value.copy(trafficAware = enabled)
        viewModelScope.launch {
            sessionStore.setTrafficAware(enabled)
            compute()
        }
    }

    /** Saves the reviewed destination to favourites under home, work or other. */
    fun saveDestination(category: PlaceCategory) {
        val request = _state.value.destination?.toCreateSavedRequest(category) ?: return
        viewModelScope.launch {
            if (placesRepository.addSavedPlace(request) is DkResult.Success) {
                _state.value = _state.value.copy(savedToFavorites = true)
                _savedConfirmation.tryEmit(category)
            }
        }
    }

    fun startNavigation(simulated: Boolean) {
        tripStore.setSimulated(simulated)
    }

    fun originFor(preview: LatLng? = null): LatLng? = preview
}

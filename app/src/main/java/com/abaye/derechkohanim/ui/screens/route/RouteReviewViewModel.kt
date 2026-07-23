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
import com.abaye.derechkohanim.data.repository.toCreateSavedRequest
import com.abaye.derechkohanim.nav.TripStore
import com.abaye.derechkohanim.service.DeviceLocation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

data class RouteReviewState(
    val destination: PlaceSuggestion? = null,
    val route: RouteResult? = null,
    val zones: List<com.abaye.derechkohanim.data.model.ZoneFeature> = emptyList(),
    val stringency: Stringency = Stringency.BASIC,
    /** Drives whether travel time is priced with live traffic. Re-fetches on change. */
    val trafficAware: Boolean = true,
    /** True once the destination has been saved to favourites in this session. */
    val savedToFavorites: Boolean = false,
    val loading: Boolean = true,
    val errorCode: String? = null,
) {
    /** Spec 17.6: three states, and the interface must never blur them together. */
    val certainty: Certainty
        get() = when {
            route == null -> Certainty.Unknown
            !route.verifiedClean -> Certainty.Unverified
            route.maneuvers.any { it.zone != null } -> Certainty.ZoneNearby
            else -> Certainty.VerifiedClean
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
                loading = true,
                errorCode = null,
            )

            val target = destination?.location
            val origin = deviceLocation.lastKnown()
                ?: if (BuildConfig.USE_MOCK_DATA) MockData.currentLocation else null
            if (target == null || origin == null) {
                _state.value = _state.value.copy(loading = false, errorCode = ErrorCode.NO_ROUTE)
                return@launch
            }

            // Zones for the chosen layer, coloured per feature by their own stringency.
            val zones = (routingRepository.zones(snapshot.stringency, null) as? DkResult.Success)
                ?.value?.features.orEmpty()

            // No engine is set: the server picks its default (hybrid), which prices travel
            // time - with or without traffic - and returns it as durationSeconds.
            val result = routingRepository.computeRoute(
                RouteRequest(
                    origin = origin,
                    destination = target,
                    stringency = snapshot.stringency,
                    mode = TravelMode.DRIVE,
                    trafficAware = snapshot.trafficAware,
                    language = snapshot.language,
                )
            )
            _state.value = when (result) {
                is DkResult.Success -> {
                    tripStore.setRoute(result.value)
                    _state.value.copy(loading = false, route = result.value, zones = zones)
                }

                is DkResult.Failure ->
                    _state.value.copy(loading = false, errorCode = result.code, zones = zones)
            }
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
        _state.value = _state.value.copy(loading = false, errorCode = null, route = null)
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

package com.abaye.derechkohanim.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.BuildConfig
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
    /** Zone polygons near the origin, from the local layer store (filtered for performance). */
    val zones: List<ZoneFeature> = emptyList(),
    /** Danger-zone (Area A) polygons near the origin, from the local layer store. */
    val dangerZones: List<DangerZoneFeature> = emptyList(),
    /** No internet-capable network right now (drives the status banner). */
    val offline: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val sessionStore: SessionStore,
    private val deviceLocation: DeviceLocation,
    private val tripStore: TripStore,
    private val connectivity: Connectivity,
    private val zoneLayerStore: ZoneLayerStore,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    /** The origin, tracked separately so the zone filter recomputes as it changes. */
    private val originFlow = MutableStateFlow<LatLng?>(null)

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
        // Draw only the zone/danger polygons near the origin, recomputed whenever the origin moves
        // or the store's layers change (initial cache load or a background sync).
        viewModelScope.launch {
            combine(originFlow, zoneLayerStore.zones, zoneLayerStore.danger) { origin, zones, danger ->
                Triple(origin, zones, danger)
            }.collect { (origin, zones, danger) ->
                val center = origin ?: FALLBACK_CENTER
                val bounds = ZoneFilter.boundsAround(center, ZoneFilter.HOME_PAD_DEG)
                _state.value = _state.value.copy(
                    zones = ZoneFilter.zonesIn(zones, bounds),
                    dangerZones = ZoneFilter.dangerIn(danger, bounds),
                )
            }
        }
    }

    fun chooseDestination(suggestion: PlaceSuggestion) {
        tripStore.setDestination(suggestion)
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
            originFlow.value = located
            _state.value = _state.value.copy(origin = located)
        }
        return located
    }

    private companion object {
        /** Jerusalem, matching the home screen's own map fallback centre. */
        val FALLBACK_CENTER = LatLng(31.7942, 35.2000)
    }
}

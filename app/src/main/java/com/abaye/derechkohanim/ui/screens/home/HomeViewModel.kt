package com.abaye.derechkohanim.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.BuildConfig
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.mock.MockData
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.PlaceSuggestion
import com.abaye.derechkohanim.data.model.SavedPlace
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.repository.PlacesRepository
import com.abaye.derechkohanim.nav.TripStore
import com.abaye.derechkohanim.service.DeviceLocation
import javax.inject.Inject

data class HomeState(
    val origin: LatLng? = null,
    val stringency: Stringency = Stringency.BASIC,
    val savedPlaces: List<SavedPlace> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val sessionStore: SessionStore,
    private val deviceLocation: DeviceLocation,
    private val tripStore: TripStore,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val snapshot = sessionStore.snapshot()
            val located = deviceLocation.lastKnown()
                ?: if (BuildConfig.USE_MOCK_DATA) MockData.currentLocation else null
            val saved = placesRepository.savedPlaces()
            _state.value = HomeState(
                origin = located,
                stringency = snapshot.stringency,
                savedPlaces = (saved as? DkResult.Success)?.value.orEmpty(),
            )
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
        if (located != null) _state.value = _state.value.copy(origin = located)
        return located
    }
}

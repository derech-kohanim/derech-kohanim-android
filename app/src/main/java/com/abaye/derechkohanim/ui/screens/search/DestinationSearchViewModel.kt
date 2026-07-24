package com.abaye.derechkohanim.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.core.CoordinateParser
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.core.ErrorCode
import com.abaye.derechkohanim.core.Geo
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.PlaceCategory
import com.abaye.derechkohanim.data.model.PlaceQueryKind
import com.abaye.derechkohanim.data.model.PlaceSuggestion
import com.abaye.derechkohanim.data.model.toSuggestion
import com.abaye.derechkohanim.data.repository.PlacesRepository
import com.abaye.derechkohanim.data.repository.toCreateSavedRequest
import com.abaye.derechkohanim.ui.components.matchRanges
import java.util.UUID
import javax.inject.Inject

data class DestinationSearchState(
    val query: String = "",
    /** Recognised straight from the input, so it needs no network call. */
    val coordinate: PlaceSuggestion? = null,
    /** Dedicated quick slots, null when the user has not set them yet. */
    val home: PlaceSuggestion? = null,
    val work: PlaceSuggestion? = null,
    val saved: List<PlaceSuggestion> = emptyList(),
    val recent: List<PlaceSuggestion> = emptyList(),
    val results: List<PlaceSuggestion> = emptyList(),
    val isSearching: Boolean = false,
    val errorCode: String? = null,
    /** When set, picking a place assigns it to this category instead of navigating. */
    val assignCategory: PlaceCategory? = null,
) {
    val hasLocalResults: Boolean get() = saved.isNotEmpty() || recent.isNotEmpty()
    val showShortcuts: Boolean get() = query.isBlank() && assignCategory == null
    val isEmptyResult: Boolean
        get() = query.isNotBlank() && !isSearching && errorCode == null &&
            results.isEmpty() && !hasLocalResults && coordinate == null
}

/**
 * Saved and recent places answer instantly from memory while the server search is still
 * being debounced. Every keystroke supersedes the previous request through flatMapLatest,
 * so a slow response can never overwrite a newer one.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class DestinationSearchViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val sessionStore: SessionStore,
    private val deviceLocation: com.abaye.derechkohanim.service.DeviceLocation,
    private val tripStore: com.abaye.derechkohanim.nav.TripStore,
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    private val _state = MutableStateFlow(DestinationSearchState())
    val state: StateFlow<DestinationSearchState> = _state.asStateFlow()

    private val _selection = MutableStateFlow<PlaceSuggestion?>(null)
    val selection: StateFlow<PlaceSuggestion?> = _selection.asStateFlow()

    /** Emits the category that was just saved, so the screen can confirm it briefly. */
    private val _savedConfirmation = MutableSharedFlow<PlaceCategory>(extraBufferCapacity = 4)
    val savedConfirmation: SharedFlow<PlaceCategory> = _savedConfirmation.asSharedFlow()

    private var allSaved: List<PlaceSuggestion> = emptyList()
    private var allRecent: List<PlaceSuggestion> = emptyList()
    private var language: Language = Language.HE
    private var origin: LatLng? = null

    /** Kept stable while the user types one destination, then rotated after a selection. */
    private var sessionToken: String = UUID.randomUUID().toString()

    init {
        viewModelScope.launch {
            language = sessionStore.snapshot().language
            // The current location biases the server search and orders local results by
            // distance. Absent permission or fix, search simply runs without a bias.
            if (origin == null) {
                origin = deviceLocation.lastKnown()
                    ?: if (com.abaye.derechkohanim.BuildConfig.USE_MOCK_DATA) {
                        com.abaye.derechkohanim.data.mock.MockData.currentLocation
                    } else {
                        null
                    }
            }
            loadLocalPlaces()
        }
        observeQuery()
    }

    /** An explicit origin from the caller wins over the device location. */
    fun setOrigin(location: LatLng?) {
        if (location != null) origin = location
    }

    fun onQueryChange(text: String) {
        val coordinate = CoordinateParser.parse(text)?.let { asCoordinateSuggestion(it) }
        _state.value = _state.value.copy(
            query = text,
            coordinate = coordinate,
            errorCode = null,
            results = if (text.isBlank()) emptyList() else _state.value.results,
            isSearching = if (text.isBlank()) false else _state.value.isSearching,
        )
        recomputeLocal(text)
        queryFlow.value = text
    }

    /** Enters assign mode: the next place the user picks is saved with this category. */
    fun startAssign(category: PlaceCategory) {
        _state.value = _state.value.copy(assignCategory = category)
    }

    fun cancelAssign() {
        _state.value = _state.value.copy(assignCategory = null)
    }

    fun onSelect(suggestion: PlaceSuggestion) {
        val assign = _state.value.assignCategory
        if (suggestion.location != null) {
            if (assign != null) saveAs(suggestion, assign) else publish(suggestion)
            return
        }
        viewModelScope.launch {
            when (val details = placesRepository.details(suggestion.id, language, sessionToken)) {
                is DkResult.Success ->
                    if (assign != null) saveAs(details.value, assign) else publish(details.value)

                is DkResult.Failure -> _state.value = _state.value.copy(errorCode = details.code)
            }
        }
    }

    /**
     * Saves the picked place under the given category and refreshes the local lists, so a
     * new home or work slot appears immediately. The saved place API is the store of record.
     */
    private fun saveAs(suggestion: PlaceSuggestion, category: PlaceCategory) {
        val request = suggestion.toCreateSavedRequest(category) ?: return
        viewModelScope.launch {
            when (placesRepository.addSavedPlace(request)) {
                is DkResult.Success -> {
                    loadLocalPlaces()
                    _state.value = _state.value.copy(assignCategory = null, query = "")
                    queryFlow.value = ""
                    _savedConfirmation.tryEmit(category)
                }

                is DkResult.Failure ->
                    _state.value = _state.value.copy(errorCode = ErrorCode.UNKNOWN, assignCategory = null)
            }
        }
    }

    fun clearSelection() {
        _selection.value = null
    }

    fun retry() {
        val current = queryFlow.value
        queryFlow.value = ""
        queryFlow.value = current
    }

    private fun publish(suggestion: PlaceSuggestion) {
        sessionToken = UUID.randomUUID().toString()
        // Persist the chosen destination (now carrying a resolved location) before signalling
        // the screen to navigate. The route review reads it from here; without this the trip
        // has no destination and every route fails with "no clean route". Mirrors HomeViewModel.
        tripStore.setDestination(suggestion)
        _selection.value = suggestion
    }

    private fun observeQuery() {
        queryFlow
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce { if (it.length < MIN_QUERY_LENGTH) 0L else DEBOUNCE_MS }
            .flatMapLatest { query ->
                val skip = query.length < MIN_QUERY_LENGTH || CoordinateParser.parse(query) != null
                if (skip) {
                    flowOf<SearchOutcome>(SearchOutcome.Idle)
                } else {
                    flow<SearchOutcome> {
                        emit(SearchOutcome.Loading)
                        emit(
                            SearchOutcome.Done(
                                placesRepository.search(query, language, origin, sessionToken)
                            )
                        )
                    }
                }
            }
            .onEach(::applyOutcome)
            .launchIn(viewModelScope)
    }

    private fun applyOutcome(outcome: SearchOutcome) {
        _state.value = when (outcome) {
            SearchOutcome.Idle -> _state.value.copy(isSearching = false, results = emptyList())
            SearchOutcome.Loading -> _state.value.copy(isSearching = true, errorCode = null)
            is SearchOutcome.Done -> when (val result = outcome.result) {
                is DkResult.Success -> _state.value.copy(
                    isSearching = false,
                    results = result.value,
                    errorCode = null,
                )

                is DkResult.Failure -> _state.value.copy(
                    isSearching = false,
                    results = emptyList(),
                    errorCode = result.code,
                )
            }
        }
    }

    private suspend fun loadLocalPlaces() {
        val saved = placesRepository.savedPlaces()
        if (saved is DkResult.Success) {
            allSaved = saved.value.map { it.toSuggestion().withDistance() }
        }
        val recent = placesRepository.recentPlaces()
        if (recent is DkResult.Success) {
            allRecent = recent.value.map { it.toSuggestion().withDistance() }
        }
        recomputeLocal(_state.value.query)
    }

    /**
     * Splits home and work into their own slots and keeps the general saved list free of
     * them while the field is empty, so the two shortcuts are not shown twice. While the user
     * is typing, everything is searched together.
     */
    private fun recomputeLocal(query: String) {
        val home = allSaved.firstOrNull { it.category == PlaceCategory.HOME }
        val work = allSaved.firstOrNull { it.category == PlaceCategory.WORK }
        val blank = query.isBlank()
        val generalSource = if (blank) {
            allSaved.filter { it.category != PlaceCategory.HOME && it.category != PlaceCategory.WORK }
        } else {
            allSaved
        }
        _state.value = _state.value.copy(
            home = home,
            work = work,
            saved = filterLocal(generalSource, query),
            recent = filterLocal(allRecent, query),
        )
    }

    private fun PlaceSuggestion.withDistance(): PlaceSuggestion {
        val from = origin ?: return this
        val to = location ?: return this
        return copy(distanceMeters = Geo.distanceMeters(from, to))
    }

    private fun filterLocal(source: List<PlaceSuggestion>, query: String): List<PlaceSuggestion> {
        val needle = query.trim()
        if (needle.isEmpty()) return source
        return source.mapNotNull { suggestion ->
            val titleRanges = matchRanges(suggestion.title, needle)
            val subtitleHit = suggestion.subtitle?.contains(needle, ignoreCase = true) == true
            if (titleRanges.isEmpty() && !subtitleHit) return@mapNotNull null
            suggestion.copy(matchedRanges = titleRanges)
        }
    }

    private fun asCoordinateSuggestion(location: LatLng) = PlaceSuggestion(
        id = "coordinates:${location.lat},${location.lng}",
        title = CoordinateParser.format(location),
        subtitle = null,
        category = PlaceCategory.COORDINATES,
        kind = PlaceQueryKind.COORDINATES,
        location = location,
        externalId = null,
        distanceMeters = origin?.let { Geo.distanceMeters(it, location) },
        matchedRanges = emptyList(),
    )

    private sealed interface SearchOutcome {
        data object Idle : SearchOutcome
        data object Loading : SearchOutcome
        data class Done(val result: DkResult<List<PlaceSuggestion>>) : SearchOutcome
    }

    private companion object {
        const val DEBOUNCE_MS = 250L
        const val MIN_QUERY_LENGTH = 2
    }
}

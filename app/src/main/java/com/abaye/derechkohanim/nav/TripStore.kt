package com.abaye.derechkohanim.nav

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.abaye.derechkohanim.data.model.PlaceSuggestion
import com.abaye.derechkohanim.data.model.RouteResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The trip being planned, shared between the search, review and driving screens. It lives
 * here rather than in navigation arguments because a route with its geometry is far too
 * large to serialise into a back stack entry.
 */
@Singleton
class TripStore @Inject constructor() {

    private val _destination = MutableStateFlow<PlaceSuggestion?>(null)
    val destination: StateFlow<PlaceSuggestion?> = _destination.asStateFlow()

    private val _route = MutableStateFlow<RouteResult?>(null)
    val route: StateFlow<RouteResult?> = _route.asStateFlow()

    private val _simulated = MutableStateFlow(false)
    val simulated: StateFlow<Boolean> = _simulated.asStateFlow()

    fun setDestination(suggestion: PlaceSuggestion) {
        _destination.value = suggestion
        _route.value = null
    }

    fun setRoute(route: RouteResult?) {
        _route.value = route
    }

    fun setSimulated(simulated: Boolean) {
        _simulated.value = simulated
    }

    fun clear() {
        _destination.value = null
        _route.value = null
        _simulated.value = false
    }
}

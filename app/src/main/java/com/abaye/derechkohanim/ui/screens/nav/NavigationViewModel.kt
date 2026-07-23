package com.abaye.derechkohanim.ui.screens.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.core.Geo
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.CreateReportRequest
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.Maneuver
import com.abaye.derechkohanim.data.model.NavigationStatus
import com.abaye.derechkohanim.data.model.RouteEvent
import com.abaye.derechkohanim.data.model.RouteEventBundle
import com.abaye.derechkohanim.data.model.RouteEventType
import com.abaye.derechkohanim.data.model.RouteResult
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.points
import com.abaye.derechkohanim.data.repository.RouteEventsRepository
import com.abaye.derechkohanim.nav.TripStore
import com.abaye.derechkohanim.voice.SpokenInstruction
import com.abaye.derechkohanim.voice.VoicePhrase
import com.abaye.derechkohanim.voice.VoicePlayer
import javax.inject.Inject

/** The banner state for an approaching event, distinct from the on-screen maneuver panel. */
data class RouteAlert(
    val type: RouteEventType,
    val title: String?,
    val distanceMeters: Double,
) {
    val isZone: Boolean get() = type == RouteEventType.ZONE
}

/**
 * A request to say something, structured parts only. The screen turns it into a phrase and
 * clip keys through VoicePhrase, so the spoken sentence is never taken from the banner text.
 */
data class Announcement(
    val id: Long,
    val urgent: Boolean,
    val bucketMeters: Int,
    val maneuverAction: String? = null,
    val street: String? = null,
    val eventType: RouteEventType? = null,
    val eventTitle: String? = null,
)

data class NavigationState(
    val status: NavigationStatus = NavigationStatus.IDLE,
    val route: RouteResult? = null,
    val events: RouteEventBundle = RouteEventBundle.empty(""),
    val currentManeuverIndex: Int = 0,
    val distanceToManeuverMeters: Double = 0.0,
    val distanceRemainingMeters: Double = 0.0,
    val durationRemainingSeconds: Double = 0.0,
    val trafficDelaySeconds: Double = 0.0,
    val currentLocation: LatLng? = null,
    val headingDegrees: Float = 0f,
    val speedMps: Double = 0.0,
    val stringency: Stringency = Stringency.BASIC,
    val simulated: Boolean = false,
    val muted: Boolean = false,
    val activeAlert: RouteAlert? = null,
    val simulationProgress: Float = 0f,
    val simulationPaused: Boolean = false,
) {
    val currentManeuver: Maneuver?
        get() = route?.maneuvers?.getOrNull(currentManeuverIndex)
    val nextManeuver: Maneuver?
        get() = route?.maneuvers?.getOrNull(currentManeuverIndex + 1)
    val verifiedClean: Boolean get() = route?.verifiedClean == true
    val hasZoneNearby: Boolean get() = route?.maneuvers?.any { it.zone != null } == true
}

/**
 * Drives both live navigation and simulation from one state machine (spec 15.4). The live
 * position feed is scaffolding: the simulator advances along the geometry, and a real device
 * feed would replace [tick] without changing the announcement, alert or ETA logic.
 */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val tripStore: TripStore,
    private val routeEventsRepository: RouteEventsRepository,
    private val voicePlayer: VoicePlayer,
) : ViewModel() {

    private val _state = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = _state.asStateFlow()

    private val _announcements = MutableSharedFlow<Announcement>(extraBufferCapacity = 8)
    val announcements: SharedFlow<Announcement> = _announcements.asSharedFlow()

    private val _reportAdded = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val reportAdded: SharedFlow<Unit> = _reportAdded.asSharedFlow()

    private var loop: Job? = null
    private var geometry: List<LatLng> = emptyList()
    private var cumulative: List<Double> = emptyList()
    private var progressIndex = 0
    private var announcementId = 0L

    private val announcedManeuverRungs = mutableSetOf<String>()
    private val announcedEvents = mutableSetOf<String>()
    private val userReports = mutableListOf<RouteEvent>()

    init {
        viewModelScope.launch {
            val snapshot = sessionStore.snapshot()
            val route = tripStore.route.value
            val simulated = tripStore.simulated.value
            geometry = route?.geometry?.points().orEmpty()
            cumulative = cumulativeDistances(geometry)

            val bundle = route
                ?.let { routeEventsRepository.events(it.id) }
                ?.let { (it as? DkResult.Success)?.value }
                ?: RouteEventBundle.empty(route?.id.orEmpty())

            voicePlayer.setMuted(!snapshot.voiceEnabled)
            voicePlayer.prepareTts(snapshot.language)

            _state.value = _state.value.copy(
                route = route,
                events = bundle,
                simulated = simulated,
                stringency = snapshot.stringency,
                muted = !snapshot.voiceEnabled,
                status = if (route == null) NavigationStatus.IDLE else NavigationStatus.NAVIGATING,
                currentLocation = geometry.firstOrNull(),
                distanceRemainingMeters = route?.distanceMeters ?: 0.0,
                durationRemainingSeconds = (route?.durationSeconds ?: 0.0) + bundle.delaySeconds,
                trafficDelaySeconds = bundle.delaySeconds,
                distanceToManeuverMeters = route?.maneuvers?.getOrNull(1)?.distanceMeters ?: 0.0,
            )
            if (route != null) startLoop()
        }
    }

    private fun startLoop() {
        loop?.cancel()
        loop = viewModelScope.launch {
            while (progressIndex < geometry.lastIndex) {
                if (!_state.value.simulationPaused) {
                    progressIndex++
                    tick()
                }
                delay(TICK_MS)
            }
            _state.value = _state.value.copy(status = NavigationStatus.ARRIVED)
        }
    }

    private fun progressedMeters(): Double = cumulative.getOrNull(progressIndex) ?: 0.0

    private fun tick() {
        val route = _state.value.route ?: return
        val previous = _state.value.currentLocation
        val location = geometry.getOrNull(progressIndex) ?: return
        val fraction = progressIndex.toDouble() / geometry.lastIndex.coerceAtLeast(1)

        val heading = previous?.let { Geo.bearingDegrees(it, location) } ?: _state.value.headingDegrees
        val speed = previous?.let { Geo.distanceMeters(it, location) / (TICK_MS / 1000.0) } ?: 0.0

        val upcomingIndex = route.maneuvers.indexOfFirst { it.geometryIndex >= progressIndex }
            .let { if (it < 0) route.maneuvers.lastIndex else it }
        val toManeuver = route.maneuvers.getOrNull(upcomingIndex)
        val distanceToManeuver = toManeuver?.let { Geo.distanceMeters(location, it.point) } ?: 0.0

        val trafficAhead = if (_state.value.events.traffic.any { it.toIndex > progressIndex }) {
            _state.value.events.delaySeconds
        } else {
            0.0
        }

        _state.value = _state.value.copy(
            currentLocation = location,
            headingDegrees = heading,
            speedMps = speed,
            currentManeuverIndex = (upcomingIndex - 1).coerceAtLeast(0),
            distanceToManeuverMeters = distanceToManeuver,
            distanceRemainingMeters = route.distanceMeters * (1 - fraction),
            durationRemainingSeconds = route.durationSeconds * (1 - fraction) + trafficAhead,
            trafficDelaySeconds = trafficAhead,
            simulationProgress = fraction.toFloat(),
        )

        announceManeuver(upcomingIndex, toManeuver, distanceToManeuver)
        updateEventAlert()
    }

    /** Announces a manoeuvre once per ladder rung, never continuously (spec 15.2, 15.9). */
    private fun announceManeuver(index: Int, maneuver: Maneuver?, distance: Double) {
        maneuver ?: return
        for (rung in VoicePhrase.ANNOUNCE_RUNGS_METERS) {
            if (rung == 0) continue
            if (distance <= rung && !announcedManeuverRungs.contains("$index:$rung")) {
                VoicePhrase.ANNOUNCE_RUNGS_METERS
                    .filter { it >= rung }
                    .forEach { announcedManeuverRungs.add("$index:$it") }
                emitManeuver(maneuver, rung)
                return
            }
        }
        if (distance <= NOW_THRESHOLD_METERS && !announcedManeuverRungs.contains("$index:0")) {
            announcedManeuverRungs.add("$index:0")
            emitManeuver(maneuver, 0)
        }
    }

    private fun emitManeuver(maneuver: Maneuver, bucket: Int) {
        _announcements.tryEmit(
            Announcement(
                id = announcementId++,
                urgent = false,
                bucketMeters = bucket,
                maneuverAction = maneuver.action,
                street = maneuver.street,
            )
        )
    }

    /**
     * Keeps the approaching-event banner current using along-route distance, and announces
     * each event once when it first enters the trigger radius. Zone warnings are urgent and
     * preempt (spec 15.9).
     */
    private fun updateEventAlert() {
        val progressed = progressedMeters()
        val ahead = (_state.value.events.events + userReports)
            .map { it to (it.routeDistanceMeters - progressed) }
            .filter { (event, remaining) -> remaining >= 0 && remaining <= triggerFor(event.type) }
            .minByOrNull { it.second }

        if (ahead == null) {
            if (_state.value.activeAlert != null) {
                _state.value = _state.value.copy(activeAlert = null)
            }
            return
        }

        val (event, remaining) = ahead
        _state.value = _state.value.copy(
            activeAlert = RouteAlert(event.type, event.labelHe ?: event.label, remaining),
        )
        if (!announcedEvents.contains(event.id)) {
            announcedEvents.add(event.id)
            _announcements.tryEmit(
                Announcement(
                    id = announcementId++,
                    urgent = event.isZone,
                    bucketMeters = VoicePhrase.snapToRung(remaining),
                    eventType = event.type,
                    eventTitle = event.labelHe ?: event.label,
                )
            )
        }
    }

    private fun triggerFor(type: RouteEventType): Double =
        if (type == RouteEventType.ZONE) ZONE_TRIGGER_METERS else REPORT_TRIGGER_METERS

    /**
     * The screen assembles the phrase, then hands it back here. Submitting is fire and forget:
     * the player supersedes stale announcements itself, so navigation never waits and never
     * chains through a backlog of passed manoeuvres.
     */
    fun speak(instruction: SpokenInstruction) {
        voicePlayer.submit(instruction)
    }

    fun toggleMute() {
        val muted = !_state.value.muted
        _state.value = _state.value.copy(muted = muted)
        voicePlayer.setMuted(muted)
        viewModelScope.launch { sessionStore.setVoiceEnabled(!muted) }
    }

    fun togglePause() {
        _state.value = _state.value.copy(simulationPaused = !_state.value.simulationPaused)
    }

    /** Simulation review controls: jump straight to the next or previous manoeuvre. */
    fun jumpToNextManeuver() {
        val route = _state.value.route ?: return
        val next = route.maneuvers.firstOrNull { it.geometryIndex > progressIndex }
        progressIndex = (next?.geometryIndex ?: geometry.lastIndex).coerceAtMost(geometry.lastIndex)
        resumeAt()
    }

    fun jumpToPreviousManeuver() {
        val route = _state.value.route ?: return
        val prev = route.maneuvers.lastOrNull { it.geometryIndex < progressIndex - 1 }
        progressIndex = (prev?.geometryIndex ?: 0).coerceIn(0, geometry.lastIndex)
        resumeAt()
    }

    private fun resumeAt() {
        if (_state.value.status == NavigationStatus.ARRIVED) {
            _state.value = _state.value.copy(status = NavigationStatus.NAVIGATING)
            startLoop()
        }
        tick()
    }

    fun restart() {
        progressIndex = 0
        announcedManeuverRungs.clear()
        announcedEvents.clear()
        _state.value = _state.value.copy(
            status = NavigationStatus.NAVIGATING,
            simulationPaused = false,
            activeAlert = null,
        )
        startLoop()
    }

    fun addReport(type: RouteEventType) {
        val location = _state.value.currentLocation ?: return
        val routeId = _state.value.route?.id ?: return
        val report = RouteEvent(
            id = "user-${announcementId++}",
            type = type,
            location = location,
            routeDistanceMeters = progressedMeters(),
            reportedByUser = true,
        )
        userReports += report
        _state.value = _state.value.copy(
            events = _state.value.events.copy(events = _state.value.events.events + report),
        )
        _reportAdded.tryEmit(Unit)
        // Persist to the server; the optimistic marker already shows locally.
        viewModelScope.launch {
            routeEventsRepository.report(CreateReportRequest(routeId, type, location))
        }
    }

    fun stop() {
        loop?.cancel()
        _state.value = _state.value.copy(status = NavigationStatus.CANCELLED)
        tripStore.clear()
    }

    override fun onCleared() {
        loop?.cancel()
        voicePlayer.release()
    }

    private fun cumulativeDistances(geometry: List<LatLng>): List<Double> {
        if (geometry.isEmpty()) return emptyList()
        val out = DoubleArray(geometry.size)
        for (i in 1 until geometry.size) {
            out[i] = out[i - 1] + Geo.distanceMeters(geometry[i - 1], geometry[i])
        }
        return out.toList()
    }

    private companion object {
        const val TICK_MS = 220L
        const val NOW_THRESHOLD_METERS = 25.0
        const val ZONE_TRIGGER_METERS = 500.0
        const val REPORT_TRIGGER_METERS = 350.0
    }
}

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
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.core.Geo
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.CreateReportRequest
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.Maneuver
import com.abaye.derechkohanim.data.model.NavFeedbackRating
import com.abaye.derechkohanim.data.model.NavFeedbackRequest
import com.abaye.derechkohanim.data.model.NavigationStatus
import com.abaye.derechkohanim.data.model.RouteEvent
import com.abaye.derechkohanim.data.model.RouteEventBundle
import com.abaye.derechkohanim.data.model.RouteEventType
import com.abaye.derechkohanim.data.model.RouteRequest
import com.abaye.derechkohanim.data.model.RouteResult
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.TrafficSegment
import com.abaye.derechkohanim.data.model.TravelMode
import com.abaye.derechkohanim.data.model.points
import com.abaye.derechkohanim.data.repository.RouteEventsRepository
import com.abaye.derechkohanim.data.repository.RoutingRepository
import com.abaye.derechkohanim.nav.TripStore
import com.abaye.derechkohanim.service.Connectivity
import com.abaye.derechkohanim.service.DeviceLocation
import com.abaye.derechkohanim.voice.SpokenInstruction
import com.abaye.derechkohanim.voice.VoicePackManager
import com.abaye.derechkohanim.voice.VoicePhrase
import com.abaye.derechkohanim.voice.VoicePlayer
import android.os.SystemClock
import javax.inject.Inject

/** The banner state for an approaching event, distinct from the on-screen maneuver panel. */
data class RouteAlert(
    val type: RouteEventType,
    val title: String?,
    val distanceMeters: Double,
    /** The reason and locality for a zone, surfaced with the alert (short) - not just a name. */
    val city: String? = null,
    val description: String? = null,
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
    /**
     * The route still ahead: the current position followed by the remaining geometry from
     * [NavigationViewModel.progressIndex] onward. The map draws only this, so the portion
     * already driven is not painted over. Empty until progress has been applied.
     */
    val remainingRoutePoints: List<LatLng> = emptyList(),
    /** Traffic segments re-based onto [remainingRoutePoints] indices; passed ones are dropped. */
    val remainingTraffic: List<com.abaye.derechkohanim.data.model.TrafficSegment> = emptyList(),
    val headingDegrees: Float = 0f,
    val speedMps: Double = 0.0,
    val stringency: Stringency = Stringency.BASIC,
    val simulated: Boolean = false,
    val muted: Boolean = false,
    val activeAlert: RouteAlert? = null,
    /**
     * True while the driver has left the route and a new one is being computed (online only).
     * The instruction card shows a "recalculating" line instead of the next manoeuvre.
     */
    val rerouting: Boolean = false,
    val simulationProgress: Float = 0f,
    val simulationPaused: Boolean = false,
    /** Simulation playback speed multiplier: 1x, 2x, 4x or 8x. Higher advances faster. */
    val simulationSpeed: Int = 1,
    /** No internet-capable network right now (drives the Waze-style status banner). */
    val offline: Boolean = false,
    /** No GPS fix: permission denied, or the last real fix is older than the staleness window. */
    val noGps: Boolean = false,
    /** The route's voice clips are all cached, so the drive can run with no connectivity. */
    val offlineReady: Boolean = false,
) {
    val currentManeuver: Maneuver?
        get() = route?.maneuvers?.getOrNull(currentManeuverIndex)
    val nextManeuver: Maneuver?
        get() = route?.maneuvers?.getOrNull(currentManeuverIndex + 1)
    val verifiedClean: Boolean get() = route?.verifiedClean == true
    val hasZoneNearby: Boolean get() = route?.maneuvers?.any { it.zone != null } == true
}

/**
 * Drives both live navigation and simulation from one state machine (spec 15.4). A real drive
 * (`simulated == false`) is fed by the device GPS: each fix is snapped onto the route geometry
 * to set [progressIndex], then run through the same [applyProgress] path the simulator uses, so
 * announcements, zone alerts, ETA and server progress reporting are identical either way. The
 * timer simulator runs only for `simulated == true`.
 */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val tripStore: TripStore,
    private val routeEventsRepository: RouteEventsRepository,
    private val routingRepository: RoutingRepository,
    private val voicePlayer: VoicePlayer,
    private val voicePackManager: VoicePackManager,
    private val deviceLocation: DeviceLocation,
    private val connectivity: Connectivity,
) : ViewModel() {

    private val _state = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = _state.asStateFlow()

    private val _announcements = MutableSharedFlow<Announcement>(extraBufferCapacity = 8)
    val announcements: SharedFlow<Announcement> = _announcements.asSharedFlow()

    /** Ready-made voice prompts by string resource (e.g. rerouting), spoken as plain text. */
    private val _voicePrompts = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val voicePrompts: SharedFlow<Int> = _voicePrompts.asSharedFlow()

    private val _reportAdded = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val reportAdded: SharedFlow<Unit> = _reportAdded.asSharedFlow()

    private var loop: Job? = null
    private var geometry: List<LatLng> = emptyList()
    private var cumulative: List<Double> = emptyList()
    private var progressIndex = 0
    private var announcementId = 0L
    /** The upcoming-manoeuvre index last seen, so a stale announcement can be preempted when the
     *  driver passes a manoeuvre and it advances. */
    private var lastUpcomingIndex = -1
    /** Consecutive fixes seen beyond the off-route threshold; a reroute triggers past a floor. */
    private var offRouteFixes = 0
    private var rerouteJob: Job? = null
    /** Uptime of the last real GPS fix, for the "no GPS" staleness check. */
    private var lastFixMs = 0L
    private var gpsWatchdog: Job? = null

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
                remainingRoutePoints = geometry,
                remainingTraffic = bundle.traffic,
                distanceRemainingMeters = route?.distanceMeters ?: 0.0,
                durationRemainingSeconds = (route?.durationSeconds ?: 0.0) + bundle.delaySeconds,
                trafficDelaySeconds = bundle.delaySeconds,
                distanceToManeuverMeters = route?.maneuvers?.getOrNull(1)?.distanceMeters ?: 0.0,
            )
            if (route != null) {
                prefetchRouteClips(route)
                if (simulated) startLoop() else startRealFeed()
            }
        }
        observeConnectivity()
    }

    /**
     * Streams real connectivity into the state so the banner reacts the instant the network drops
     * or returns. The route geometry and events are already in memory, so an offline drop never
     * stops the drive - only the banner appears.
     */
    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivity.online().collect { online ->
                _state.value = _state.value.copy(offline = !online)
            }
        }
    }

    /**
     * Per-drive prefetch (offline continuity): resolve and cache the route's required voice clips
     * that are not already in the pack (typically street-name clips), so the whole drive can run
     * with no connectivity. Non-blocking; a clip that cannot be fetched simply falls back to TTS.
     * On completion the "ready for offline" indicator is shown.
     */
    private fun prefetchRouteClips(route: RouteResult) {
        viewModelScope.launch {
            voicePackManager.ensureRouteClips(route.requiredVoiceKeys)
            _state.value = _state.value.copy(offlineReady = true)
        }
    }

    /**
     * Watches the age of the last GPS fix during a real drive. [DeviceLocation.updates] emits
     * nothing without permission or a fix, so a stale age (or none at all) flips the "no GPS" state
     * that drives the banner. Simulated drives never lack a fix, so the watchdog runs only for real
     * navigation.
     */
    private fun startGpsWatchdog() {
        gpsWatchdog?.cancel()
        // Grant the initial fix a full window before declaring no GPS.
        lastFixMs = SystemClock.elapsedRealtime()
        gpsWatchdog = viewModelScope.launch {
            while (true) {
                val stale = SystemClock.elapsedRealtime() - lastFixMs > GPS_STALE_MS
                if (_state.value.noGps != stale) {
                    _state.value = _state.value.copy(noGps = stale)
                }
                delay(GPS_CHECK_MS)
            }
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
                // Higher playback speed shortens the per-tick delay proportionally.
                delay(TICK_MS / _state.value.simulationSpeed.coerceAtLeast(1))
            }
            _state.value = _state.value.copy(status = NavigationStatus.ARRIVED)
        }
    }

    /**
     * Live navigation from the real device GPS. Each fix is projected onto the route to advance
     * [progressIndex], the heading is taken from the fix's own bearing or, failing that, derived
     * from the motion between the last two fixes, and the shared [applyProgress] path handles the
     * rest. If the stream never emits (permission denied, no fix) navigation simply waits - the
     * timer simulator is never started for a real drive.
     */
    private fun startRealFeed() {
        loop?.cancel()
        startGpsWatchdog()
        loop = viewModelScope.launch {
            deviceLocation.updates().collect { fix ->
                lastFixMs = SystemClock.elapsedRealtime()
                if (_state.value.noGps) _state.value = _state.value.copy(noGps = false)
                val previous = _state.value.currentLocation
                progressIndex = nearestIndex(fix.location, progressIndex)

                val heading = fix.bearingDegrees
                    ?: previous
                        ?.takeIf { Geo.distanceMeters(it, fix.location) >= MIN_HEADING_MOVE_METERS }
                        ?.let { Geo.bearingDegrees(it, fix.location) }
                    ?: _state.value.headingDegrees
                val speed = fix.speedMps ?: 0.0

                trackOffRoute(fix.location)
                applyProgress(fix.location, heading, speed)

                if (progressIndex >= geometry.lastIndex) {
                    _state.value = _state.value.copy(status = NavigationStatus.ARRIVED)
                }
            }
        }
    }

    /**
     * Off-route detection for a real drive. The offset is the distance from the fix to the nearest
     * route vertex (already found by [nearestIndex]). Once the driver stays beyond the threshold
     * for a few consecutive fixes, we react by connectivity: online, recompute the route from here;
     * offline, keep the existing route and current position on screen so the driver can steer back.
     */
    private fun trackOffRoute(location: LatLng) {
        if (_state.value.rerouting || geometry.isEmpty()) return
        val nearest = geometry.getOrNull(progressIndex) ?: return
        val offset = Geo.distanceMeters(location, nearest)
        if (offset <= OFF_ROUTE_THRESHOLD_METERS) {
            offRouteFixes = 0
            return
        }
        offRouteFixes++
        if (offRouteFixes < OFF_ROUTE_MIN_FIXES) return
        offRouteFixes = 0
        // Offline: do not recompute. The existing route and the live position stay on screen.
        if (connectivity.isOnline()) reroute(location)
    }

    /**
     * Recomputes the route from the current position after going off-route. Shows a "recalculating"
     * state and speaks the rerouting prompt while the request is in flight; on success the active
     * route and geometry are replaced and navigation continues. On failure the old route is kept.
     */
    private fun reroute(origin: LatLng) {
        if (rerouteJob?.isActive == true) return
        val destination = tripStore.destination.value?.location ?: geometry.lastOrNull() ?: return
        _state.value = _state.value.copy(rerouting = true, status = NavigationStatus.REROUTING)
        _voicePrompts.tryEmit(R.string.nav_rerouting)
        rerouteJob = viewModelScope.launch {
            val snapshot = sessionStore.snapshot()
            val result = routingRepository.computeRoute(
                RouteRequest(
                    origin = origin,
                    destination = destination,
                    stringency = snapshot.stringency,
                    mode = TravelMode.DRIVE,
                    trafficAware = snapshot.trafficAware,
                    language = snapshot.language,
                    avoidTolls = snapshot.avoidTolls,
                    avoidDangerZones = snapshot.avoidDangerZones,
                )
            )
            val newRoute = (result as? DkResult.Success)?.value?.routes?.firstOrNull()
            if (newRoute != null) {
                replaceRoute(newRoute)
            } else {
                // Keep the existing route; just leave the recalculating state.
                _state.value = _state.value.copy(rerouting = false, status = NavigationStatus.NAVIGATING)
            }
        }
    }

    /** Swaps in a freshly computed route: resets progress and announcement bookkeeping, reloads
     *  its events, and continues from the start of the new geometry (the current position). */
    private suspend fun replaceRoute(route: RouteResult) {
        tripStore.setRoute(route)
        geometry = route.geometry.points()
        cumulative = cumulativeDistances(geometry)
        progressIndex = 0
        lastUpcomingIndex = -1
        offRouteFixes = 0
        announcedManeuverRungs.clear()
        announcedEvents.clear()
        val bundle = (routeEventsRepository.events(route.id) as? DkResult.Success)?.value
            ?: RouteEventBundle.empty(route.id)
        _state.value = _state.value.copy(
            route = route,
            events = bundle,
            rerouting = false,
            status = NavigationStatus.NAVIGATING,
            currentManeuverIndex = 0,
            currentLocation = geometry.firstOrNull(),
            remainingRoutePoints = geometry,
            remainingTraffic = bundle.traffic,
            distanceRemainingMeters = route.distanceMeters,
            durationRemainingSeconds = route.durationSeconds + bundle.delaySeconds,
            trafficDelaySeconds = bundle.delaySeconds,
        )
    }

    /**
     * Nearest route vertex to a real fix, searched forward from the current index (with a small
     * look-back for GPS jitter) so progress does not jump backwards when the route passes near
     * itself. Falls back to a full scan only at the very start.
     */
    private fun nearestIndex(location: LatLng, from: Int): Int {
        if (geometry.isEmpty()) return 0
        val start = (from - LOOK_BACK_STEPS).coerceAtLeast(0)
        var best = from.coerceIn(0, geometry.lastIndex)
        var bestDist = Double.MAX_VALUE
        for (i in start..geometry.lastIndex) {
            val d = Geo.distanceMeters(location, geometry[i])
            if (d < bestDist) {
                bestDist = d
                best = i
            }
        }
        return best
    }

    private fun progressedMeters(): Double = cumulative.getOrNull(progressIndex) ?: 0.0

    /**
     * The route still ahead: the exact current position, then the geometry vertices from just
     * past [progressIndex] to the end. Drawing only this leaves the already-driven stretch
     * unpainted, so the line runs from the location marker to the destination.
     */
    private fun remainingFrom(location: LatLng): List<LatLng> {
        if (geometry.isEmpty()) return listOf(location)
        val start = (progressIndex + 1).coerceIn(0, geometry.size)
        return buildList {
            add(location)
            if (start < geometry.size) addAll(geometry.subList(start, geometry.size))
        }
    }

    /**
     * Traffic segments re-based onto [remainingFrom]'s indices. Trimmed index 0 is the current
     * position (~ [progressIndex]), so every segment shifts back by [progressIndex]; segments
     * fully behind the driver are dropped, and a straddling one is clamped to start at 0.
     */
    private fun remainingTrafficFrom(): List<TrafficSegment> =
        _state.value.events.traffic.mapNotNull { segment ->
            if (segment.toIndex <= progressIndex) return@mapNotNull null
            val from = (segment.fromIndex - progressIndex).coerceAtLeast(0)
            val to = (segment.toIndex - progressIndex).coerceAtLeast(from)
            segment.copy(fromIndex = from, toIndex = to)
        }

    /** The simulator step: the position is the next geometry vertex, heading and speed inferred. */
    private fun tick() {
        val previous = _state.value.currentLocation
        val location = geometry.getOrNull(progressIndex) ?: return
        val heading = previous?.let { Geo.bearingDegrees(it, location) } ?: _state.value.headingDegrees
        val speed = previous?.let { Geo.distanceMeters(it, location) / (TICK_MS / 1000.0) } ?: 0.0
        applyProgress(location, heading, speed)
    }

    /**
     * The shared progress path for both the simulator and the real GPS feed. [progressIndex] must
     * already be set; this maps it to the upcoming manoeuvre, ETA and traffic, then fires the
     * announcement and alert logic - unchanged whichever source drove the update.
     */
    private fun applyProgress(location: LatLng, heading: Float, speed: Double) {
        val route = _state.value.route ?: return
        val fraction = progressIndex.toDouble() / geometry.lastIndex.coerceAtLeast(1)

        val upcomingIndex = route.maneuvers.indexOfFirst { it.geometryIndex >= progressIndex }
            .let { if (it < 0) route.maneuvers.lastIndex else it }
        val toManeuver = route.maneuvers.getOrNull(upcomingIndex)
        val distanceToManeuver = toManeuver?.let { Geo.distanceMeters(location, it.point) } ?: 0.0

        // Progress just passed a manoeuvre: preempt any in-flight/queued announcement that belonged
        // to it, so the device does not keep reading a now-stale instruction (fast simulation and
        // real drives alike). An active zone warning is spared inside the player.
        if (lastUpcomingIndex in 0 until upcomingIndex) {
            voicePlayer.dropStale()
        }
        lastUpcomingIndex = upcomingIndex

        val trafficAhead = if (_state.value.events.traffic.any { it.toIndex > progressIndex }) {
            _state.value.events.delaySeconds
        } else {
            0.0
        }

        _state.value = _state.value.copy(
            currentLocation = location,
            remainingRoutePoints = remainingFrom(location),
            remainingTraffic = remainingTrafficFrom(),
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
        // Enrich a zone alert with the reason and city from the matching maneuver zone, so the
        // alert carries the explanation, not just the layer name (the events feed has no
        // description field). Falls back to name only when no match is found.
        val eventName = event.labelHe ?: event.label
        val zone = if (event.isZone) {
            _state.value.route?.maneuvers
                ?.mapNotNull { it.zone }
                ?.firstOrNull { it.nameHe == eventName || it.name == eventName }
        } else {
            null
        }
        _state.value = _state.value.copy(
            activeAlert = RouteAlert(
                type = event.type,
                title = event.labelHe ?: event.label,
                distanceMeters = remaining,
                city = zone?.city,
                description = zone?.description,
            ),
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

    /** Sets the simulation playback speed. Clamped to the supported 1x/2x/4x/8x steps. */
    fun setSimulationSpeed(speed: Int) {
        val clamped = if (speed in SIMULATION_SPEEDS) speed else 1
        _state.value = _state.value.copy(simulationSpeed = clamped)
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

    /**
     * Sends optional end-of-trip feedback for the current route. Fire and forget and fully
     * best-effort: a failure is swallowed so finishing or stopping the drive is never blocked
     * (the endpoint is OptionalAuth, so it also works for guests). A BAD rating may carry the
     * free-text note; an empty note is dropped.
     */
    fun submitFeedback(rating: NavFeedbackRating, text: String? = null) {
        val routeId = _state.value.route?.id ?: tripStore.route.value?.id ?: return
        viewModelScope.launch {
            routingRepository.submitFeedback(
                NavFeedbackRequest(
                    routeId = routeId,
                    rating = rating,
                    text = text?.trim()?.ifBlank { null },
                )
            )
        }
    }

    fun stop() {
        loop?.cancel()
        _state.value = _state.value.copy(status = NavigationStatus.CANCELLED)
        tripStore.clear()
    }

    override fun onCleared() {
        loop?.cancel()
        gpsWatchdog?.cancel()
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
        val SIMULATION_SPEEDS = listOf(1, 2, 4, 8)
        const val NOW_THRESHOLD_METERS = 25.0
        const val ZONE_TRIGGER_METERS = 500.0
        const val REPORT_TRIGGER_METERS = 350.0
        // Below this movement between fixes the derived bearing is just GPS noise, so the last
        // heading is kept instead of spinning the map.
        const val MIN_HEADING_MOVE_METERS = 3.0
        // How far back along the route the snap may look, to absorb GPS jitter without jumping.
        const val LOOK_BACK_STEPS = 2
        // Distance from the route beyond which a fix counts as off-route.
        const val OFF_ROUTE_THRESHOLD_METERS = 45.0
        // Consecutive off-route fixes before reacting, to ignore a single bad GPS reading.
        const val OFF_ROUTE_MIN_FIXES = 2
        // No real fix within this window (or no permission) counts as "no GPS".
        const val GPS_STALE_MS = 10_000L
        const val GPS_CHECK_MS = 2_000L
    }
}

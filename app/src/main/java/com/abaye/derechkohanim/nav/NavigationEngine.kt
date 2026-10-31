package com.abaye.derechkohanim.nav

import android.content.Context
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.analytics.Analytics
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.core.Geo
import com.abaye.derechkohanim.data.local.NavStateStore
import com.abaye.derechkohanim.data.local.PersistedNavState
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
import com.abaye.derechkohanim.data.model.TrafficSegment
import com.abaye.derechkohanim.data.model.TravelMode
import com.abaye.derechkohanim.data.model.points
import com.abaye.derechkohanim.core.AlertThrottle
import com.abaye.derechkohanim.core.RouteProjection
import com.abaye.derechkohanim.core.ZoneFilter
import com.abaye.derechkohanim.core.ZoneTripwire
import com.abaye.derechkohanim.data.model.DangerZoneFeature
import com.abaye.derechkohanim.data.model.ManeuverZone
import com.abaye.derechkohanim.data.model.ZoneFeature
import com.abaye.derechkohanim.data.repository.RouteEventsRepository
import com.abaye.derechkohanim.data.repository.RoutingRepository
import com.abaye.derechkohanim.data.repository.ZoneLayerStore
import com.abaye.derechkohanim.service.Connectivity
import com.abaye.derechkohanim.service.DeviceLocation
import com.abaye.derechkohanim.service.LocationPermissions
import com.abaye.derechkohanim.service.NavigationForegroundService
import com.abaye.derechkohanim.ui.screens.nav.Announcement
import com.abaye.derechkohanim.ui.screens.nav.NavigationState
import com.abaye.derechkohanim.ui.screens.nav.ReturnGuidance
import com.abaye.derechkohanim.ui.screens.nav.RouteAlert
import com.abaye.derechkohanim.ui.screens.nav.speedKmh
import com.abaye.derechkohanim.data.model.VoiceIds
import com.abaye.derechkohanim.voice.SpokenInstruction
import com.abaye.derechkohanim.voice.VoiceCue
import com.abaye.derechkohanim.voice.VoiceInstruction
import com.abaye.derechkohanim.voice.VoicePackManager
import com.abaye.derechkohanim.voice.VoicePhrase
import com.abaye.derechkohanim.voice.VoicePlayer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single, process-durable source of truth for a live drive (spec 15.4). Previously the whole
 * driving state machine lived in the screen-scoped [com.abaye.derechkohanim.ui.screens.nav.NavigationViewModel],
 * so navigation died the instant the screen went away. It now lives here, in a @Singleton with its
 * own coroutine scope, so it keeps running while the app is backgrounded (kept alive by
 * [NavigationForegroundService] for a real drive) and can be reconstructed after a kill from
 * [NavStateStore].
 *
 * Both the view model and the foreground service share this one instance: the view model observes
 * [state] and forwards user actions, and the service keeps the process alive and mirrors [state]
 * into its ongoing notification. A real drive (`simulated == false`) is fed by the device GPS and
 * persisted as it advances; a simulated drive runs the timer loop only, is never persisted and
 * never starts the service.
 */
@Singleton
class NavigationEngine @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val sessionStore: SessionStore,
    private val tripStore: TripStore,
    private val navStateStore: NavStateStore,
    private val routeEventsRepository: RouteEventsRepository,
    private val routingRepository: RoutingRepository,
    private val voicePlayer: VoicePlayer,
    private val voicePackManager: VoicePackManager,
    private val zoneLayerStore: ZoneLayerStore,
    private val deviceLocation: DeviceLocation,
    private val connectivity: Connectivity,
    private val analytics: Analytics,
) {
    // A scope that outlives any single screen. Main.immediate because MediaPlayer/TextToSpeech and
    // the fused-location callback expect a Looper thread, exactly as the old viewModelScope gave.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = _state.asStateFlow()

    private val _announcements = MutableSharedFlow<Announcement>(extraBufferCapacity = 8)
    val announcements: SharedFlow<Announcement> = _announcements.asSharedFlow()

    private val _voicePrompts = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val voicePrompts: SharedFlow<Int> = _voicePrompts.asSharedFlow()

    private val _reportAdded = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val reportAdded: SharedFlow<Unit> = _reportAdded.asSharedFlow()

    /** True while a drive is running (or arrived and awaiting the user), so the screen re-attaches
     *  to it instead of starting a second one, and app start skips the resume dialog. */
    @Volatile
    var isActive: Boolean = false
        private set

    private var loop: Job? = null
    private var geometry: List<LatLng> = emptyList()
    private var cumulative: List<Double> = emptyList()
    private var progressIndex = 0
    private var announcementId = 0L
    private var lastUpcomingIndex = -1
    private var offRouteFixes = 0
    private var rerouteJob: Job? = null
    private var lastFixMs = 0L
    private var gpsWatchdog: Job? = null
    private var persistJob: Job? = null
    private var connectivityJob: Job? = null
    private var zonesJob: Job? = null

    /**
     * The on-device tripwire over the zones near the route (spec section 2). Rebuilt whenever the
     * layers or the corridor change; works with no network because the layers are cached on disk.
     */
    private var tripwire: ZoneTripwire<ManeuverZone>? = null
    private var insideZone: ManeuverZone? = null
    private var avoidDangerZones = true
    private var offRouteNow = false
    private val zoneThrottle = AlertThrottle(ZONE_REPEAT_MS, ZONE_REPEAT_METERS)
    private val returnThrottle = AlertThrottle(RETURN_REPEAT_MS, RETURN_REPEAT_METERS)
    private val gpsThrottle = AlertThrottle(GPS_REPEAT_MS, 0.0)
    /** Cameras and level crossings: one announcement per feature, even across reroutes. */
    private val roadAlertThrottle = AlertThrottle(ROAD_ALERT_REPEAT_MS, ROAD_ALERT_REPEAT_METERS)
    private val speedingThrottle = AlertThrottle(SPEEDING_REPEAT_MS, SPEEDING_REPEAT_METERS)
    /** When the current stretch over the limit began (elapsed realtime), 0 when not over it. */
    private var overLimitSinceMs = 0L

    private val announcedManeuverRungs = mutableSetOf<String>()
    private val announcedEvents = mutableSetOf<String>()
    private val userReports = mutableListOf<RouteEvent>()

    /**
     * Attaches a freshly opened navigation screen. If a drive is already running (a normal drive
     * still going, or one just resumed after a kill) this is a no-op and the screen simply observes
     * the live [state]. Otherwise it begins a new drive from whatever [TripStore] holds.
     */
    fun attach() {
        if (isActive) return
        startFromTrip()
    }

    private fun startFromTrip() {
        val route = tripStore.route.value ?: return
        val simulated = tripStore.simulated.value
        beginDrive(route, simulated, startIndex = 0, persistImmediately = !simulated)
    }

    /**
     * Restores a drive after the process was killed. Called before the screen is shown, so the
     * later [attach] finds an already-active engine. Simulated drives are never persisted, so a
     * resumed drive is always real.
     */
    fun resume(persisted: PersistedNavState) {
        if (isActive) return
        tripStore.setRoute(persisted.route)
        persisted.destination?.let { tripStore.setDestination(it); tripStore.setRoute(persisted.route) }
        tripStore.setSimulated(false)
        beginDrive(
            persisted.route,
            simulated = false,
            startIndex = persisted.progressIndex,
            persistImmediately = true,
            fallbackEvents = persisted.events,
        )
    }

    /**
     * Starts (or resumes) a drive. [fallbackEvents] are the events saved with a persisted drive,
     * used when they cannot be fetched now - a drive resumed with no network keeps its alerts.
     */
    private fun beginDrive(
        route: RouteResult,
        simulated: Boolean,
        startIndex: Int,
        persistImmediately: Boolean,
        fallbackEvents: RouteEventBundle? = null,
    ) {
        isActive = true
        analytics.track("navigation_started", mapOf("simulated" to simulated))
        cancelLoops()
        announcedManeuverRungs.clear()
        announcedEvents.clear()
        userReports.clear()
        lastUpcomingIndex = -1
        offRouteFixes = 0
        insideZone = null
        offRouteNow = false
        zoneThrottle.reset()
        returnThrottle.reset()
        gpsThrottle.reset()
        roadAlertThrottle.reset()
        speedingThrottle.reset()
        overLimitSinceMs = 0L

        scope.launch {
            val snapshot = sessionStore.snapshot()
            avoidDangerZones = snapshot.avoidDangerZones
            geometry = route.geometry.points()
            cumulative = cumulativeDistances(geometry)
            progressIndex = startIndex.coerceIn(0, geometry.lastIndex.coerceAtLeast(0))

            val bundle = (routeEventsRepository.events(route.id) as? DkResult.Success)?.value
                ?: fallbackEvents?.takeIf { it.routeId == route.id }
                ?: RouteEventBundle.empty(route.id)

            voicePlayer.setMuted(!snapshot.voiceEnabled)
            voicePlayer.prepareTts(snapshot.language)

            _state.value = NavigationState(
                route = route,
                events = bundle,
                simulated = simulated,
                stringency = snapshot.stringency,
                muted = !snapshot.voiceEnabled,
                voiceFemale = VoiceIds.isFemale(snapshot.voiceId),
                status = NavigationStatus.NAVIGATING,
                currentLocation = geometry.getOrNull(progressIndex) ?: geometry.firstOrNull(),
                remainingRoutePoints = geometry,
                remainingTraffic = bundle.traffic,
                distanceRemainingMeters = route.distanceMeters,
                durationRemainingSeconds = route.durationSeconds + bundle.delaySeconds,
                trafficDelaySeconds = bundle.delaySeconds,
                distanceToManeuverMeters = route.maneuvers.getOrNull(1)?.distanceMeters ?: 0.0,
            )

            // If resuming mid-route, recompute the derived state (remaining line, ETA, upcoming
            // manoeuvre) at the saved position so the screen and notification are correct at once.
            if (progressIndex > 0) {
                geometry.getOrNull(progressIndex)?.let { applyProgress(it, 0f, 0.0) }
            }

            prefetchRouteClips(route)
            observeZoneLayers()

            if (simulated) {
                startLoop()
            } else {
                startForegroundService()
                startRealFeed()
                startPersistLoop()
                if (persistImmediately) persistNow()
            }
        }
        observeConnectivity()
    }

    // -- persistence -----------------------------------------------------------------------------

    private fun startPersistLoop() {
        persistJob?.cancel()
        persistJob = scope.launch {
            while (true) {
                delay(PERSIST_INTERVAL_MS)
                if (!isActive || _state.value.simulated) continue
                if (_state.value.status == NavigationStatus.NAVIGATING ||
                    _state.value.status == NavigationStatus.REROUTING
                ) {
                    navStateStore.updateProgress(progressIndex)
                }
            }
        }
    }

    /** Writes the full record (static route + current progress). Called at start and on reroute. */
    private fun persistNow() {
        val route = _state.value.route ?: return
        if (_state.value.simulated) return
        val persisted = PersistedNavState(
            route = route,
            destination = tripStore.destination.value,
            simulated = false,
            progressIndex = progressIndex,
            stringency = _state.value.stringency,
            events = _state.value.events,
        )
        scope.launch { navStateStore.save(persisted) }
    }

    private fun clearPersisted() {
        scope.launch { navStateStore.clear() }
    }

    // -- foreground service ----------------------------------------------------------------------

    private fun startForegroundService() {
        // A location-type foreground service may only start with location permission granted; a
        // real drive with no permission gets no fixes anyway, so we simply skip the service.
        if (!LocationPermissions.hasForeground(appContext)) return
        NavigationForegroundService.start(appContext)
    }

    private fun stopForegroundService() {
        NavigationForegroundService.stop(appContext)
    }

    // -- the drive --------------------------------------------------------------------------------

    private fun observeConnectivity() {
        connectivityJob?.cancel()
        connectivityJob = scope.launch {
            connectivity.online().collect { online ->
                _state.value = _state.value.copy(offline = !online)
                // Back online while being guided back to the route: compute a proper new route.
                val location = _state.value.currentLocation
                if (online && _state.value.returnToRoute != null && location != null) {
                    reroute(location)
                }
            }
        }
    }

    /**
     * Draws only the zone/danger polygons near the route, from the local layer store, and keeps them
     * current as the store's cache loads or a background sync lands. The filter is recomputed on each
     * store emission (and re-run from [replaceRoute] when a reroute changes the geometry).
     */
    private fun observeZoneLayers() {
        zonesJob?.cancel()
        zonesJob = scope.launch {
            combine(zoneLayerStore.zones, zoneLayerStore.danger) { _, _ -> }.collect {
                applyZoneFilter()
            }
        }
    }

    private fun applyZoneFilter() {
        val bounds = ZoneFilter.boundsAround(geometry, ZoneFilter.ROUTE_PAD_DEG) ?: return
        val zones = ZoneFilter.zonesIn(zoneLayerStore.zones.value, bounds)
        val danger = ZoneFilter.dangerIn(zoneLayerStore.danger.value, bounds)
        _state.value = _state.value.copy(zones = zones, dangerZones = danger)
        tripwire = buildTripwire(zones, danger)
    }

    /**
     * The tripwire covers the same corridor the map draws (the route padded by ~13 km), which is
     * wide enough for any realistic detour. Danger zones take part only when the driver avoids
     * them, matching what the route itself was computed to avoid.
     */
    private fun buildTripwire(
        zones: List<ZoneFeature>,
        danger: List<DangerZoneFeature>,
    ): ZoneTripwire<ManeuverZone> {
        val stringency = _state.value.stringency
        val halachic = zones.map { feature ->
            feature.geometry to ManeuverZone(
                name = feature.properties.name,
                nameHe = feature.properties.nameHe,
                stringency = feature.properties.stringency,
                description = feature.properties.description,
                city = feature.properties.city,
                danger = false,
            )
        }
        val areaA = if (avoidDangerZones) {
            danger.map { feature ->
                feature.geometry to ManeuverZone(
                    name = feature.properties.name,
                    nameHe = feature.properties.name,
                    stringency = stringency,
                    description = feature.properties.description,
                    city = feature.properties.city,
                    danger = true,
                )
            }
        } else {
            emptyList()
        }
        return ZoneTripwire(halachic + areaA)
    }

    private fun prefetchRouteClips(route: RouteResult) {
        scope.launch {
            // The route's own clips plus every fixed cue (zone, camera, GPS, return-to-route), so
            // the alerts that matter most when the network drops are already on the device.
            voicePackManager.ensureRouteClips(
                (route.requiredVoiceKeys + VoiceCue.ALL_CLIP_KEYS + exitClipKeys(route)).distinct(),
            )
            _state.value = _state.value.copy(offlineReady = true)
        }
    }

    private fun startGpsWatchdog() {
        gpsWatchdog?.cancel()
        lastFixMs = SystemClock.elapsedRealtime()
        gpsWatchdog = scope.launch {
            while (true) {
                val stale = SystemClock.elapsedRealtime() - lastFixMs > GPS_STALE_MS
                if (_state.value.noGps != stale) {
                    _state.value = _state.value.copy(noGps = stale)
                    if (stale) announceGps(VoiceCue.GPS_LOST)
                }
                delay(GPS_CHECK_MS)
            }
        }
    }

    private fun startLoop() {
        loop?.cancel()
        loop = scope.launch {
            while (progressIndex < geometry.lastIndex) {
                if (!_state.value.simulationPaused) {
                    progressIndex++
                    tick()
                }
                delay(TICK_MS / _state.value.simulationSpeed.coerceAtLeast(1))
            }
            onArrived()
        }
    }

    private fun startRealFeed() {
        loop?.cancel()
        startGpsWatchdog()
        loop = scope.launch {
            deviceLocation.updates().collect { fix ->
                lastFixMs = SystemClock.elapsedRealtime()
                if (_state.value.noGps) {
                    _state.value = _state.value.copy(noGps = false)
                    announceGps(VoiceCue.GPS_RECOVERED)
                }
                val previous = _state.value.currentLocation
                progressIndex = nearestIndex(fix.location, progressIndex)

                val heading = fix.bearingDegrees
                    ?: previous
                        ?.takeIf { Geo.distanceMeters(it, fix.location) >= MIN_HEADING_MOVE_METERS }
                        ?.let { Geo.bearingDegrees(it, fix.location) }
                    ?: _state.value.headingDegrees
                val speed = fix.speedMps ?: 0.0

                trackOffRoute(fix.location, heading)
                applyProgress(fix.location, heading, speed)

                if (progressIndex >= geometry.lastIndex) onArrived()
            }
        }
    }

    /**
     * Off-route handling. Online, leaving the route reroutes as before. Offline (stage A of the
     * offline plan) no new route can be computed, so the driver is guided back to the closest
     * point of the stored route instead, and told when they are back on it. The distance is the
     * true distance to the route line (projected onto its segments), not to the nearest vertex.
     */
    private fun trackOffRoute(location: LatLng, heading: Float) {
        if (_state.value.rerouting || geometry.isEmpty()) return
        val foot = RouteProjection.nearest(location, geometry) ?: return
        offRouteNow = foot.distanceMeters > OFF_ROUTE_THRESHOLD_METERS
        if (!offRouteNow) {
            offRouteFixes = 0
            if (_state.value.returnToRoute != null) {
                _state.value = _state.value.copy(returnToRoute = null)
                emitCue(VoiceCue.BACK_ON_ROUTE, bucketMeters = 0, urgent = false)
            }
            return
        }
        if (_state.value.returnToRoute != null) {
            updateReturnGuidance(location, heading, foot)
            return
        }
        offRouteFixes++
        if (offRouteFixes < OFF_ROUTE_MIN_FIXES) return
        offRouteFixes = 0
        if (connectivity.isOnline()) reroute(location) else updateReturnGuidance(location, heading, foot)
    }

    private fun updateReturnGuidance(location: LatLng, heading: Float, foot: RouteProjection.Foot) {
        val bearing = Geo.bearingDegrees(location, foot.point)
        val relative = (((bearing - heading) % 360f) + 540f) % 360f - 180f
        _state.value = _state.value.copy(
            returnToRoute = ReturnGuidance(
                distanceMeters = foot.distanceMeters,
                bearingDegrees = bearing,
                relativeDegrees = relative,
            ),
        )
        if (returnThrottle.tryFire("return", location)) {
            emitCue(VoiceCue.RETURN_TO_ROUTE, bucketMeters = 0, urgent = false)
        }
    }

    private fun announceGps(cue: VoiceCue) {
        if (gpsThrottle.tryFire(cue.name, null)) emitCue(cue, bucketMeters = 0, urgent = false)
    }

    private fun reroute(origin: LatLng) {
        if (rerouteJob?.isActive == true) return
        val destination = tripStore.destination.value?.location ?: geometry.lastOrNull() ?: return
        _state.value = _state.value.copy(rerouting = true, status = NavigationStatus.REROUTING)
        _voicePrompts.tryEmit(R.string.nav_rerouting)
        rerouteJob = scope.launch {
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
                _state.value = _state.value.copy(rerouting = false, status = NavigationStatus.NAVIGATING)
                // The server could not be reached after all: fall back to guiding back to the
                // stored route, exactly as when the network was known to be down.
                RouteProjection.nearest(origin, geometry)?.let { foot ->
                    if (foot.distanceMeters > OFF_ROUTE_THRESHOLD_METERS) {
                        updateReturnGuidance(origin, _state.value.headingDegrees, foot)
                    }
                }
            }
        }
    }

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
            returnToRoute = null,
            status = NavigationStatus.NAVIGATING,
            currentManeuverIndex = 0,
            currentLocation = geometry.firstOrNull(),
            remainingRoutePoints = geometry,
            remainingTraffic = bundle.traffic,
            distanceRemainingMeters = route.distanceMeters,
            durationRemainingSeconds = route.durationSeconds + bundle.delaySeconds,
            trafficDelaySeconds = bundle.delaySeconds,
        )
        // The route geometry changed: rewrite the durable record so a kill mid-reroute resumes
        // onto the new route, not the abandoned one.
        persistNow()
        // Re-filter the layers to the new corridor (the store collector keys off its own emissions,
        // not the geometry, so a reroute must ask for a fresh filter explicitly).
        applyZoneFilter()
    }

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

    private fun remainingFrom(location: LatLng): List<LatLng> {
        if (geometry.isEmpty()) return listOf(location)
        val start = (progressIndex + 1).coerceIn(0, geometry.size)
        return buildList {
            add(location)
            if (start < geometry.size) addAll(geometry.subList(start, geometry.size))
        }
    }

    private fun remainingTrafficFrom(): List<TrafficSegment> =
        _state.value.events.traffic.mapNotNull { segment ->
            if (segment.toIndex <= progressIndex) return@mapNotNull null
            val from = (segment.fromIndex - progressIndex).coerceAtLeast(0)
            val to = (segment.toIndex - progressIndex).coerceAtLeast(from)
            segment.copy(fromIndex = from, toIndex = to)
        }

    private fun tick() {
        val previous = _state.value.currentLocation
        val location = geometry.getOrNull(progressIndex) ?: return
        val heading = previous?.let { Geo.bearingDegrees(it, location) } ?: _state.value.headingDegrees
        val speed = previous?.let { Geo.distanceMeters(it, location) / (TICK_MS / 1000.0) } ?: 0.0
        applyProgress(location, heading, speed)
    }

    private fun applyProgress(location: LatLng, heading: Float, speed: Double) {
        val route = _state.value.route ?: return
        val fraction = progressIndex.toDouble() / geometry.lastIndex.coerceAtLeast(1)

        val upcomingIndex = route.maneuvers.indexOfFirst { it.geometryIndex >= progressIndex }
            .let { if (it < 0) route.maneuvers.lastIndex else it }
        val toManeuver = route.maneuvers.getOrNull(upcomingIndex)
        val distanceToManeuver = toManeuver?.let { Geo.distanceMeters(location, it.point) } ?: 0.0

        if (lastUpcomingIndex in 0 until upcomingIndex) {
            voicePlayer.dropStale()
        }
        lastUpcomingIndex = upcomingIndex

        val trafficAhead = if (_state.value.events.traffic.any { it.toIndex > progressIndex }) {
            _state.value.events.delaySeconds
        } else {
            0.0
        }

        val limit = route.speedLimitAt(progressIndex)?.kmh
        val speeding = updateSpeeding(speedKmh(speed), limit, location)

        _state.value = _state.value.copy(
            currentLocation = location,
            speedLimitKmh = limit,
            speeding = speeding,
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
        checkTripwire(location, heading)
    }

    /**
     * The local safety net. Inside a zone: an urgent "entering a forbidden zone" (once per entry,
     * throttled) and a banner that stays up while inside; on leaving, a short "left the zone".
     * Otherwise it looks ahead - along the remaining route while on it, or straight along the
     * heading while off it - and warns of the first zone within [ZONE_LOOKAHEAD_METERS]. Runs on
     * every progress update with no network: the polygons come from the on-disk layer cache.
     */
    private fun checkTripwire(location: LatLng, heading: Float) {
        val wire = tripwire ?: return
        if (wire.size == 0) return

        val inside = wire.containing(location)
        if (inside != null) {
            _state.value = _state.value.copy(activeAlert = zoneAlert(inside, 0.0, inside = true))
            if (insideZone == null && zoneThrottle.tryFire("enter:${inside.nameHe}", location)) {
                emitCue(VoiceCue.ZONE_ENTER, bucketMeters = 0, urgent = true)
            }
            insideZone = inside
            return
        }
        if (insideZone != null) {
            insideZone = null
            emitCue(VoiceCue.ZONE_EXIT, bucketMeters = 0, urgent = false)
        }

        val path = if (offRouteNow) {
            listOf(location, Geo.destination(location, heading, ZONE_LOOKAHEAD_METERS))
        } else {
            remainingFrom(location)
        }
        val hit = wire.firstAlong(path, ZONE_LOOKAHEAD_METERS) ?: return
        // A server-side zone event already on screen keeps its banner; the tripwire adds its own
        // only when nothing zone-related is showing yet.
        if (_state.value.activeAlert?.isZone != true) {
            _state.value = _state.value.copy(activeAlert = zoneAlert(hit.zone, hit.distanceMeters, inside = false))
        }
        if (zoneThrottle.tryFire("ahead:${hit.zone.nameHe}", location)) {
            emitCue(VoiceCue.ZONE_AHEAD, VoicePhrase.snapToRung(hit.distanceMeters), urgent = true)
        }
    }

    private fun zoneAlert(zone: ManeuverZone, distance: Double, inside: Boolean) = RouteAlert(
        type = RouteEventType.ZONE,
        title = zone.nameHe.ifBlank { zone.name },
        distanceMeters = distance,
        city = zone.city,
        description = zone.description,
        inside = inside,
    )

    private fun emitCue(cue: VoiceCue, bucketMeters: Int, urgent: Boolean) {
        _announcements.tryEmit(
            Announcement(
                id = announcementId++,
                urgent = urgent,
                bucketMeters = bucketMeters,
                cue = cue,
            )
        )
    }

    /**
     * Over the limit by more than [SPEEDING_MARGIN_KMH] for [SPEEDING_SUSTAIN_MS] counts as
     * speeding (a brief overshoot or a GPS spike does not); it ends once back within
     * [SPEEDING_CLEAR_MARGIN_KMH]. The spoken warning is throttled. A simulated drive has no real
     * speed, so it never speeds.
     */
    private fun updateSpeeding(kmh: Int, limit: Int?, location: LatLng): Boolean {
        if (limit == null || _state.value.simulated) {
            overLimitSinceMs = 0L
            return false
        }
        val now = SystemClock.elapsedRealtime()
        return when {
            kmh > limit + SPEEDING_MARGIN_KMH -> {
                if (overLimitSinceMs == 0L) overLimitSinceMs = now
                val sustained = now - overLimitSinceMs >= SPEEDING_SUSTAIN_MS
                if (sustained && !_state.value.speeding && speedingThrottle.tryFire("speeding", location)) {
                    emitCue(VoiceCue.SPEEDING, bucketMeters = 0, urgent = false)
                }
                sustained || _state.value.speeding
            }
            kmh <= limit + SPEEDING_CLEAR_MARGIN_KMH -> {
                overLimitSinceMs = 0L
                false
            }
            // Between the two margins: keep whatever state we were in (hysteresis).
            else -> _state.value.speeding
        }
    }

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
                exitNumber = maneuver.exitNumber,
            )
        )
    }

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
                maxSpeedKmh = event.maxSpeedKmh,
            ),
        )
        if (event.isRoadAlert) {
            // Map features from OSM: a fixed cue ("speed camera ahead"), once per feature. The
            // throttle keeps a reroute (which resets announcedEvents) from repeating it at once.
            if (announcedEvents.add(event.id) &&
                roadAlertThrottle.tryFire(event.id, _state.value.currentLocation)
            ) {
                val cue = if (event.type == RouteEventType.SPEED_CAMERA) {
                    VoiceCue.SPEED_CAMERA
                } else {
                    VoiceCue.RAIL_CROSSING
                }
                emitCue(cue, VoicePhrase.snapToRung(remaining), urgent = false)
            }
            return
        }
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

    private fun triggerFor(type: RouteEventType): Double = when (type) {
        RouteEventType.ZONE -> ZONE_TRIGGER_METERS
        RouteEventType.SPEED_CAMERA -> CAMERA_TRIGGER_METERS
        RouteEventType.RAIL_CROSSING -> RAIL_CROSSING_TRIGGER_METERS
        else -> REPORT_TRIGGER_METERS
    }

    // -- user actions (forwarded from the view model) --------------------------------------------

    fun speak(instruction: SpokenInstruction) {
        voicePlayer.submit(instruction)
    }

    fun toggleMute() {
        val muted = !_state.value.muted
        _state.value = _state.value.copy(muted = muted)
        voicePlayer.setMuted(muted)
        scope.launch { sessionStore.setVoiceEnabled(!muted) }
    }

    fun togglePause() {
        _state.value = _state.value.copy(simulationPaused = !_state.value.simulationPaused)
    }

    fun setSimulationSpeed(speed: Int) {
        val clamped = if (speed in SIMULATION_SPEEDS) speed else 1
        _state.value = _state.value.copy(simulationSpeed = clamped)
    }

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
        scope.launch {
            routeEventsRepository.report(CreateReportRequest(routeId, type, location))
        }
    }

    fun submitFeedback(rating: NavFeedbackRating, text: String? = null) {
        val routeId = _state.value.route?.id ?: tripStore.route.value?.id ?: return
        analytics.track("feedback_submitted", mapOf("rating" to rating.name.lowercase()))
        scope.launch {
            routingRepository.submitFeedback(
                NavFeedbackRequest(
                    routeId = routeId,
                    rating = rating,
                    text = text?.trim()?.ifBlank { null },
                )
            )
        }
    }

    /** Arrival: the drive is complete. The persistent record and the service are torn down so a
     *  later kill never offers a resume for a finished trip, but [state] stays ARRIVED so the
     *  screen can show its arrival panel until the user leaves. */
    private fun onArrived() {
        if (_state.value.status == NavigationStatus.ARRIVED) return
        _state.value = _state.value.copy(status = NavigationStatus.ARRIVED)
        analytics.track("navigation_arrived")
        cancelLoops()
        stopForegroundService()
        clearPersisted()
    }

    /** Full teardown on an explicit stop/cancel/finish: everything is released and the engine is
     *  free for the next drive. */
    fun stop() {
        if (isActive) analytics.track("navigation_stopped")
        cancelLoops()
        connectivityJob?.cancel()
        _state.value = _state.value.copy(status = NavigationStatus.CANCELLED)
        stopForegroundService()
        clearPersisted()
        tripStore.clear()
        voicePlayer.release()
        isActive = false
    }

    private fun cancelLoops() {
        loop?.cancel()
        gpsWatchdog?.cancel()
        rerouteJob?.cancel()
        persistJob?.cancel()
        zonesJob?.cancel()
    }

    private fun exitClipKeys(route: RouteResult): List<String> =
        route.maneuvers.mapNotNull { VoiceInstruction.exitKey(it.action, it.exitNumber) }

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
        /** Cameras are announced early enough to settle to the limit; crossings a bit later. */
        const val CAMERA_TRIGGER_METERS = 500.0
        const val RAIL_CROSSING_TRIGGER_METERS = 300.0
        const val ROAD_ALERT_REPEAT_MS = 5 * 60_000L
        const val ROAD_ALERT_REPEAT_METERS = 1_000.0
        const val SPEEDING_MARGIN_KMH = 5
        const val SPEEDING_CLEAR_MARGIN_KMH = 2
        const val SPEEDING_SUSTAIN_MS = 3_000L
        const val SPEEDING_REPEAT_MS = 60_000L
        const val SPEEDING_REPEAT_METERS = 500.0
        const val MIN_HEADING_MOVE_METERS = 3.0
        const val LOOK_BACK_STEPS = 2
        const val OFF_ROUTE_THRESHOLD_METERS = 45.0
        const val OFF_ROUTE_MIN_FIXES = 2
        /** How far ahead the tripwire looks for a zone, along the route or the heading. */
        const val ZONE_LOOKAHEAD_METERS = 400.0
        /** A warning for the same zone repeats only after this long AND this far. */
        const val ZONE_REPEAT_MS = 90_000L
        const val ZONE_REPEAT_METERS = 300.0
        /** "Return to the route" repeats only after this long AND this far. */
        const val RETURN_REPEAT_MS = 60_000L
        const val RETURN_REPEAT_METERS = 150.0
        /** GPS lost / recovered are each said at most once in this window, against flapping. */
        const val GPS_REPEAT_MS = 60_000L
        const val GPS_STALE_MS = 10_000L
        const val GPS_CHECK_MS = 2_000L
        // How often the fast-moving progress index is written to durable storage while driving.
        const val PERSIST_INTERVAL_MS = 4_000L
    }
}

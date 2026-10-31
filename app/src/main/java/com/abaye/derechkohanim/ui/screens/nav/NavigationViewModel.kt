package com.abaye.derechkohanim.ui.screens.nav

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.Maneuver
import com.abaye.derechkohanim.data.model.NavFeedbackRating
import com.abaye.derechkohanim.data.model.NavigationStatus
import com.abaye.derechkohanim.data.model.RouteEventBundle
import com.abaye.derechkohanim.data.model.RouteEventType
import com.abaye.derechkohanim.data.model.RouteResult
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.nav.NavigationEngine
import com.abaye.derechkohanim.voice.SpokenInstruction
import com.abaye.derechkohanim.voice.VoiceCue
import javax.inject.Inject

/**
 * Guidance back to the route when the driver left it and no new route can be computed (no
 * network). Points at the closest point of the stored route.
 */
data class ReturnGuidance(
    val distanceMeters: Double,
    /** Bearing from the driver to the target, degrees clockwise from north. */
    val bearingDegrees: Float,
    /** The same bearing relative to the driving direction: 0 is straight ahead, 90 is right. */
    val relativeDegrees: Float,
)

/** The banner state for an approaching event, distinct from the on-screen maneuver panel. */
data class RouteAlert(
    val type: RouteEventType,
    val title: String?,
    val distanceMeters: Double,
    /** The reason and locality for a zone, surfaced with the alert (short) - not just a name. */
    val city: String? = null,
    val description: String? = null,
    /** True when the driver is already inside the zone (the on-device tripwire fired). */
    val inside: Boolean = false,
    /** Enforced limit of a speed camera, when the map records it. */
    val maxSpeedKmh: Int? = null,
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
    /** Roundabout exit for a roundabout manoeuvre, spoken as "ביציאה השנייה". */
    val exitNumber: Int? = null,
    /** A fixed alert or status message; takes precedence over the manoeuvre and event fields. */
    val cue: VoiceCue? = null,
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
     * [NavigationEngine]'s progress index onward. The map draws only this, so the portion
     * already driven is not painted over. Empty until progress has been applied.
     */
    val remainingRoutePoints: List<LatLng> = emptyList(),
    /** Traffic segments re-based onto [remainingRoutePoints] indices; passed ones are dropped. */
    val remainingTraffic: List<com.abaye.derechkohanim.data.model.TrafficSegment> = emptyList(),
    /** Zone polygons near the route, from the local layer store (filtered for performance). */
    val zones: List<com.abaye.derechkohanim.data.model.ZoneFeature> = emptyList(),
    /** Danger-zone (Area A) polygons near the route, from the local layer store. */
    val dangerZones: List<com.abaye.derechkohanim.data.model.DangerZoneFeature> = emptyList(),
    val headingDegrees: Float = 0f,
    val speedMps: Double = 0.0,
    /** The speed limit where the driver is now, from the route's limit segments; null if unknown. */
    val speedLimitKmh: Int? = null,
    /** True while the driver has been over [speedLimitKmh] by the margin for a few seconds. */
    val speeding: Boolean = false,
    val stringency: Stringency = Stringency.BASIC,
    val simulated: Boolean = false,
    val muted: Boolean = false,
    /** True when the feminine Hebrew voice is chosen; the spoken texts follow it. */
    val voiceFemale: Boolean = false,
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
    /** Set while off the route with no network: guidance back to the stored route. */
    val returnToRoute: ReturnGuidance? = null,
) {
    val currentManeuver: Maneuver?
        get() = route?.maneuvers?.getOrNull(currentManeuverIndex)
    val nextManeuver: Maneuver?
        get() = route?.maneuvers?.getOrNull(currentManeuverIndex + 1)

    /** The manoeuvre after [nextManeuver], for the "then" box. */
    val secondManeuver: Maneuver?
        get() = route?.maneuvers?.getOrNull(currentManeuverIndex + 2)
    val verifiedClean: Boolean get() = route?.verifiedClean == true
    val hasZoneNearby: Boolean get() = route?.maneuvers?.any { it.zone != null } == true
}

/**
 * A thin, screen-scoped facade over the process-durable [NavigationEngine] (spec 15.4). The whole
 * driving state machine - real GPS feed, simulator, announcements, zone alerts, ETA, rerouting and
 * offline continuity - now lives in the engine so it keeps running while the app is backgrounded
 * and survives the process being killed. This view model just observes the engine's [state] and
 * event streams and forwards user actions, so a config change or leaving and returning to the
 * screen never restarts or duplicates the drive.
 */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val engine: NavigationEngine,
) : ViewModel() {

    val state: StateFlow<NavigationState> = engine.state
    val announcements: SharedFlow<Announcement> = engine.announcements
    val voicePrompts: SharedFlow<Int> = engine.voicePrompts
    val reportAdded: SharedFlow<Unit> = engine.reportAdded

    init {
        // Attach to the running drive, or start one from the trip if none is running yet.
        engine.attach()
    }

    fun speak(instruction: SpokenInstruction) = engine.speak(instruction)

    fun toggleMute() = engine.toggleMute()

    fun repeatInstruction() = engine.repeatInstruction()

    fun togglePause() = engine.togglePause()

    fun setSimulationSpeed(speed: Int) = engine.setSimulationSpeed(speed)

    fun jumpToNextManeuver() = engine.jumpToNextManeuver()

    fun jumpToPreviousManeuver() = engine.jumpToPreviousManeuver()

    fun restart() = engine.restart()

    fun addReport(type: RouteEventType) = engine.addReport(type)

    fun submitFeedback(rating: NavFeedbackRating, text: String? = null) =
        engine.submitFeedback(rating, text)

    /** Ends the drive for good (user stop, cancel or finish). The engine tears down the service,
     *  clears the durable state and frees itself; the screen leaves navigation afterwards. */
    fun stop() = engine.stop()

    // The engine is a singleton that intentionally outlives this screen, so onCleared must not
    // touch the running drive: leaving or recreating the screen keeps navigation alive.
}

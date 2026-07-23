package com.abaye.derechkohanim.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors derech-kohanim-shared/src/types/routing.ts. */

@Serializable
enum class RoutingEngine {
    @SerialName("here") HERE,
    @SerialName("google") GOOGLE,
    @SerialName("graphhopper") GRAPHHOPPER,
    @SerialName("hybrid") HYBRID,
    @SerialName("mock") MOCK,
}

@Serializable
data class ZoneProperties(
    val id: Int,
    val name: String,
    val nameHe: String,
    val stringency: Stringency,
)

@Serializable
data class ZoneFeature(
    val type: String = "Feature",
    val properties: ZoneProperties,
    val geometry: GeoJsonPolygon,
)

@Serializable
data class ZoneCollection(
    val type: String = "FeatureCollection",
    val features: List<ZoneFeature> = emptyList(),
    /** Layer version, so a client can skip downloading unchanged data. */
    val version: Int,
)

@Serializable
enum class ZoneManeuverType {
    @SerialName("zone_enter") ZONE_ENTER,
    @SerialName("zone_exit") ZONE_EXIT,
    @SerialName("zone_origin") ZONE_ORIGIN,
    @SerialName("zone_destination") ZONE_DESTINATION,
}

@Serializable
data class ManeuverZone(
    val name: String,
    val nameHe: String,
    val stringency: Stringency,
)

/**
 * One step. `voiceKeys` is the ordered clip list needed to speak it, so the client can
 * preload exactly what this route requires before departure.
 */
@Serializable
data class Maneuver(
    val id: String,
    /** Provider prose, already localised. Display only, never used for voice assembly. */
    val instruction: String,
    /** Stable action key, e.g. "turn_right". Drives the icon and the voice clip. */
    val action: String,
    val point: LatLng,
    val distanceMeters: Double,
    val geometryIndex: Int,
    val street: String? = null,
    val zone: ManeuverZone? = null,
    val voiceKeys: List<String> = emptyList(),
)

@Serializable
data class RouteRequest(
    val origin: LatLng,
    val destination: LatLng,
    val stringency: Stringency,
    val mode: TravelMode,
    val trafficAware: Boolean,
    val language: Language,
    val engine: RoutingEngine? = null,
    /** Avoid toll roads. Passed through to the provider. */
    val avoidTolls: Boolean? = null,
    /** Also return the plain provider route without zone avoidance, for comparison. */
    val includeUnavoided: Boolean? = null,
)

@Serializable
data class RouteDebug(
    val naiveGeometry: GeoJsonLineString? = null,
    val attempts: List<String>? = null,
    val naiveDistanceMeters: Double? = null,
)

@Serializable
data class RouteResult(
    val id: String,
    val geometry: GeoJsonLineString,
    val maneuvers: List<Maneuver> = emptyList(),
    val distanceMeters: Double,
    val durationSeconds: Double,
    /** True only when the server proved the geometry touches no active zone. */
    val verifiedClean: Boolean,
    /** Stable code when not clean, e.g. "avoidance_failed". */
    val reason: String? = null,
    val attribution: String,
    val engine: RoutingEngine,
    /** True when this route avoids toll roads. */
    val avoidsTolls: Boolean? = null,
    /** Short label for an alternative, e.g. "fastest" | "shortest" | "alt". */
    val label: String? = null,
    /** Returned distance divided by the direct route. 1 means avoidance cost nothing. */
    val detourRatio: Double? = null,
    val requiredVoiceKeys: List<String> = emptyList(),
    val debug: RouteDebug? = null,
)

/**
 * Result of a compute: the chosen route plus up to two alternatives, plus optionally the
 * plain provider route with no zone avoidance, for comparison. Mirrors the shared RoutePlan.
 */
@Serializable
data class RoutePlan(
    /** 1..3 zone-verified routes, best first. */
    val routes: List<RouteResult> = emptyList(),
    /** Plain provider route without avoidance. Always verifiedClean=false, never navigable. */
    val unavoidedRoute: RouteResult? = null,
)

@Serializable
data class ProviderInfo(
    val name: RoutingEngine,
    val available: Boolean,
    val isDefault: Boolean,
    val attribution: String,
    val supportsNativeAvoidance: Boolean,
    val supportsLiveTraffic: Boolean,
)

@Serializable
enum class NavigationStatus {
    @SerialName("idle") IDLE,
    @SerialName("navigating") NAVIGATING,
    @SerialName("rerouting") REROUTING,
    @SerialName("arrived") ARRIVED,
    @SerialName("cancelled") CANCELLED,
}

@Serializable
data class NavigationSession(
    val id: String,
    val routeId: String,
    val status: NavigationStatus,
    /** True when positions come from the simulator rather than the device. */
    val simulated: Boolean,
    val startedAt: String,
)

@Serializable
data class NavigationSessionRequest(
    val routeId: String,
    val simulated: Boolean = false,
)

@Serializable
data class NavigationProgressRequest(
    val sessionId: String,
    val location: LatLng,
    val headingDegrees: Double? = null,
    val speedMps: Double? = null,
    val timestamp: String,
)

/**
 * The union in the contract is `ZoneManeuverType | 'zone_ahead'`, so the extra member lives
 * here rather than in [ZoneManeuverType].
 */
@Serializable
enum class ZoneAlertType {
    @SerialName("zone_enter") ZONE_ENTER,
    @SerialName("zone_exit") ZONE_EXIT,
    @SerialName("zone_origin") ZONE_ORIGIN,
    @SerialName("zone_destination") ZONE_DESTINATION,
    @SerialName("zone_ahead") ZONE_AHEAD,
}

@Serializable
data class ZoneAlert(
    val type: ZoneAlertType,
    val zone: ManeuverZone,
    val distanceMeters: Double,
)

@Serializable
data class NavigationProgressResponse(
    val status: NavigationStatus,
    /** Index of the manoeuvre the driver is heading towards. */
    val currentManeuverIndex: Int,
    val distanceToManeuverMeters: Double,
    val distanceRemainingMeters: Double,
    val durationRemainingSeconds: Double,
    /** Set when the driver left the route and a new one was computed. */
    val route: RouteResult? = null,
    val zoneAlert: ZoneAlert? = null,
)

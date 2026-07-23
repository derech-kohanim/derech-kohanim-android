package com.abaye.derechkohanim.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors derech-kohanim-shared/src/types/events.ts. */

@Serializable
enum class RouteEventType {
    @SerialName("zone") ZONE,
    @SerialName("traffic") TRAFFIC,
    @SerialName("police") POLICE,
    @SerialName("accident") ACCIDENT,
    @SerialName("hazard") HAZARD,
    @SerialName("closure") CLOSURE,
}

@Serializable
enum class TrafficLevel {
    @SerialName("free") FREE,
    @SerialName("moderate") MODERATE,
    @SerialName("heavy") HEAVY,
}

@Serializable
data class RouteEvent(
    val id: String,
    val type: RouteEventType,
    val location: LatLng,
    /** Distance from the route origin, measured along the route. Drives approach detection. */
    val routeDistanceMeters: Double,
    /** Zone layer name or a short free label. */
    val label: String? = null,
    val labelHe: String? = null,
    /** True when a driver raised this report rather than it being seeded. */
    val reportedByUser: Boolean? = null,
    val createdAt: String? = null,
) {
    val isZone: Boolean get() = type == RouteEventType.ZONE
}

@Serializable
data class TrafficSegment(
    /** Geometry vertex indices the segment spans. */
    val fromIndex: Int,
    val toIndex: Int,
    val level: TrafficLevel,
)

@Serializable
data class RouteEventBundle(
    val routeId: String,
    val events: List<RouteEvent> = emptyList(),
    val traffic: List<TrafficSegment> = emptyList(),
    /** Extra seconds the congestion adds to the trip. */
    val delaySeconds: Double = 0.0,
) {
    companion object {
        fun empty(routeId: String) = RouteEventBundle(routeId = routeId)
    }
}

@Serializable
data class CreateReportRequest(
    val routeId: String,
    val type: RouteEventType,
    val location: LatLng,
)

/** Report types a driver can raise, in the order the picker shows them (shared REPORT_TYPES). */
val REPORT_TYPES = listOf(
    RouteEventType.POLICE,
    RouteEventType.ACCIDENT,
    RouteEventType.HAZARD,
    RouteEventType.CLOSURE,
    RouteEventType.TRAFFIC,
)

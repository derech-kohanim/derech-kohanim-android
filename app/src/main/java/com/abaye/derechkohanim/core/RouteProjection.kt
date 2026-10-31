package com.abaye.derechkohanim.core

import com.abaye.derechkohanim.data.model.LatLng
import kotlin.math.cos

/**
 * Finds the closest point of a polyline to a position, projecting onto each segment rather than
 * only checking vertices, so a long straight segment still yields the true perpendicular foot.
 * Used to guide a driver who left the route back to it when no new route can be computed
 * (offline): the foot is the target, its distance and bearing drive the "return to route" panel.
 *
 * Works in a local flat projection around the query point, which is accurate to well under a
 * metre at the distances a driver wanders off a route.
 */
object RouteProjection {

    data class Foot(
        val point: LatLng,
        /** Index of the segment's first vertex, i.e. the route continues from [segmentIndex] + 1. */
        val segmentIndex: Int,
        val distanceMeters: Double,
    )

    fun nearest(position: LatLng, polyline: List<LatLng>): Foot? {
        if (polyline.isEmpty()) return null
        if (polyline.size == 1) {
            return Foot(polyline[0], 0, Geo.distanceMeters(position, polyline[0]))
        }
        val metersPerLng = METERS_PER_DEGREE * cos(Math.toRadians(position.lat))
        var best: Foot? = null
        for (i in 0 until polyline.lastIndex) {
            val a = polyline[i]
            val b = polyline[i + 1]
            // Local metres relative to the position.
            val ax = (a.lng - position.lng) * metersPerLng
            val ay = (a.lat - position.lat) * METERS_PER_DEGREE
            val bx = (b.lng - position.lng) * metersPerLng
            val by = (b.lat - position.lat) * METERS_PER_DEGREE
            val dx = bx - ax
            val dy = by - ay
            val lengthSq = dx * dx + dy * dy
            val t = if (lengthSq == 0.0) 0.0 else (-(ax * dx + ay * dy) / lengthSq).coerceIn(0.0, 1.0)
            val px = ax + t * dx
            val py = ay + t * dy
            val distance = kotlin.math.sqrt(px * px + py * py)
            if (best == null || distance < best.distanceMeters) {
                best = Foot(
                    point = LatLng(a.lat + (b.lat - a.lat) * t, a.lng + (b.lng - a.lng) * t),
                    segmentIndex = i,
                    distanceMeters = distance,
                )
            }
        }
        return best
    }

    private const val METERS_PER_DEGREE = 111_320.0
}

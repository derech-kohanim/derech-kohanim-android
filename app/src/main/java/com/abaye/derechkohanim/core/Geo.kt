package com.abaye.derechkohanim.core

import com.abaye.derechkohanim.data.model.LatLng
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun distanceMeters(a: LatLng, b: LatLng): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) *
            sin(dLng / 2) * sin(dLng / 2)
        return EARTH_RADIUS_METERS * 2 * asin(sqrt(h))
    }

    /**
     * The point [meters] away from [from] along [bearingDegrees]. A flat-earth step, accurate to
     * well under a metre over the few hundred metres it is used for (look-ahead projection).
     */
    fun destination(from: LatLng, bearingDegrees: Float, meters: Double): LatLng {
        val bearing = Math.toRadians(bearingDegrees.toDouble())
        val dLat = meters * cos(bearing) / METERS_PER_DEGREE_LAT
        val dLng = meters * sin(bearing) / (METERS_PER_DEGREE_LAT * cos(Math.toRadians(from.lat)))
        return LatLng(from.lat + dLat, from.lng + dLng)
    }

    private const val METERS_PER_DEGREE_LAT = 111_320.0

    /** Initial bearing from a to b, in degrees clockwise from north, 0 to 360. */
    fun bearingDegrees(a: LatLng, b: LatLng): Float {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360.0) % 360.0).toFloat()
    }
}

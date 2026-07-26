package com.abaye.derechkohanim.core

import com.abaye.derechkohanim.data.model.DangerZoneFeature
import com.abaye.derechkohanim.data.model.GeoJsonPolygon
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.ZoneFeature

/**
 * Keeps the map fast with Israel-wide layer data (the danger polygons alone are ~1.4 MB): only the
 * features whose bounding box intersects a padded box around the current point or route are drawn,
 * mirroring the web, which never paints the whole country at once.
 *
 * Everything works in degrees. The padding is a rough radius in degrees of latitude (about 111 km
 * per degree), generous enough that a zone just off-screen still appears as the camera moves.
 */
object ZoneFilter {

    /** A padded box around a single reference point, e.g. the home-screen origin. ~40 km each way. */
    const val HOME_PAD_DEG = 0.35

    /** A padded box around a route or the driver's position. Tighter, since the corridor is known. */
    const val ROUTE_PAD_DEG = 0.12

    /** An axis-aligned lat/lng box. */
    data class GeoBounds(
        val minLat: Double,
        val maxLat: Double,
        val minLng: Double,
        val maxLng: Double,
    ) {
        fun intersects(other: GeoBounds): Boolean =
            minLat <= other.maxLat && maxLat >= other.minLat &&
                minLng <= other.maxLng && maxLng >= other.minLng
    }

    /** The box formed by padding a single point on every side. */
    fun boundsAround(point: LatLng, padDeg: Double): GeoBounds =
        GeoBounds(point.lat - padDeg, point.lat + padDeg, point.lng - padDeg, point.lng + padDeg)

    /** The box that encloses all [points], padded on every side. Null when there are no points. */
    fun boundsAround(points: List<LatLng>, padDeg: Double): GeoBounds? {
        if (points.isEmpty()) return null
        var minLat = Double.POSITIVE_INFINITY
        var maxLat = Double.NEGATIVE_INFINITY
        var minLng = Double.POSITIVE_INFINITY
        var maxLng = Double.NEGATIVE_INFINITY
        for (p in points) {
            if (p.lat < minLat) minLat = p.lat
            if (p.lat > maxLat) maxLat = p.lat
            if (p.lng < minLng) minLng = p.lng
            if (p.lng > maxLng) maxLng = p.lng
        }
        return GeoBounds(minLat - padDeg, maxLat + padDeg, minLng - padDeg, maxLng + padDeg)
    }

    /** The bounding box of a GeoJSON polygon, or null when it has no coordinates. */
    fun GeoJsonPolygon.bounds(): GeoBounds? {
        var minLat = Double.POSITIVE_INFINITY
        var maxLat = Double.NEGATIVE_INFINITY
        var minLng = Double.POSITIVE_INFINITY
        var maxLng = Double.NEGATIVE_INFINITY
        var seen = false
        for (ring in coordinates) {
            for (pt in ring) {
                if (pt.size < 2) continue
                seen = true
                val lng = pt[0]
                val lat = pt[1]
                if (lat < minLat) minLat = lat
                if (lat > maxLat) maxLat = lat
                if (lng < minLng) minLng = lng
                if (lng > maxLng) maxLng = lng
            }
        }
        return if (seen) GeoBounds(minLat, maxLat, minLng, maxLng) else null
    }

    fun zonesIn(features: List<ZoneFeature>, bounds: GeoBounds): List<ZoneFeature> =
        features.filter { it.geometry.bounds()?.intersects(bounds) == true }

    fun dangerIn(features: List<DangerZoneFeature>, bounds: GeoBounds): List<DangerZoneFeature> =
        features.filter { it.geometry.bounds()?.intersects(bounds) == true }
}

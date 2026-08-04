package com.abaye.derechkohanim.core

import com.abaye.derechkohanim.data.model.DangerZoneFeature
import com.abaye.derechkohanim.data.model.GeoJsonPolygon
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.ZoneFeature

/**
 * Keeps the map fast with Israel-wide layer data (the danger polygons alone are ~1.4 MB): only the
 * features whose bounding box intersects the box the map is actually looking at are drawn, so the
 * whole country is never painted at once.
 *
 * The box normally comes from the live camera ([viewport]); a reference box around a point or a
 * route ([boundsAround]) is the fallback for the moment before the camera has reported, and for the
 * non-interactive maps (route review, navigation) whose camera never leaves that reference anyway.
 *
 * Everything works in degrees. Padding is a rough radius in degrees of latitude (about 111 km per
 * degree), generous enough that a zone just off-screen still appears as the camera moves.
 */
object ZoneFilter {

    /** A padded box around a single reference point, e.g. the home-screen origin. ~40 km each way. */
    const val HOME_PAD_DEG = 0.35

    /** A padded box around a route or the driver's position. Tighter, since the corridor is known. */
    const val ROUTE_PAD_DEG = 0.12

    /**
     * How much of its own span a camera viewport is grown by on each side, so a polygon just off
     * screen is already drawn by the time the user pans onto it.
     */
    private const val VIEWPORT_PAD_FRACTION = 0.25

    /** A floor on that padding (about 100 m), so it stays useful at street-level zoom. */
    private const val VIEWPORT_MIN_PAD_DEG = 0.001

    /**
     * A feature smaller than this fraction of the viewport's own span in BOTH directions lands on
     * roughly one pixel of a phone-sized map, so at country-wide zoom it is skipped rather than
     * drawn invisibly. At any zoom the user actually browses zones at, real polygons are orders of
     * magnitude larger than this, so nothing legible is ever dropped.
     */
    private const val MIN_VISIBLE_SPAN_FRACTION = 0.0015

    /**
     * Ceiling on how many features one pass hands a renderer. Only reachable when zoomed out far
     * enough that the polygons are specks anyway, and it is what keeps a full zoom-out from stalling
     * the UI thread building thousands of polygon overlays.
     */
    const val MAX_DRAWN_FEATURES = 1500

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

    /**
     * The box a map camera currently shows, grown by [VIEWPORT_PAD_FRACTION] so a small pan does not
     * expose an empty margin. Returns null for a region the projection cannot describe yet (not laid
     * out, or a degenerate box), which the caller reads as "keep the previous bounds".
     */
    fun viewport(south: Double, north: Double, west: Double, east: Double): GeoBounds? {
        if (!south.isFinite() || !north.isFinite() || !west.isFinite() || !east.isFinite()) return null
        if (north < south) return null
        // A camera wide enough to wrap the antimeridian reports east < west. Israel-only data never
        // sits near it, so the cheap and correct answer there is simply "every longitude".
        val wraps = east < west
        val latPad = ((north - south) * VIEWPORT_PAD_FRACTION).coerceAtLeast(VIEWPORT_MIN_PAD_DEG)
        val lngPad = ((east - west) * VIEWPORT_PAD_FRACTION).coerceAtLeast(VIEWPORT_MIN_PAD_DEG)
        return GeoBounds(
            minLat = south - latPad,
            maxLat = north + latPad,
            minLng = if (wraps) -180.0 else west - lngPad,
            maxLng = if (wraps) 180.0 else east + lngPad,
        )
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

    // -- camera-driven filtering ------------------------------------------------------------------

    /**
     * A feature paired with the bounding box of its geometry. The box is computed once, when the
     * layer is loaded, so a camera move only compares numbers instead of walking every ring of
     * every polygon in the country.
     */
    data class Bounded<T>(val feature: T, val bounds: GeoBounds)

    /** Indexes a zone layer for [inBounds]. Features with no coordinates are dropped. */
    fun indexZones(features: List<ZoneFeature>): List<Bounded<ZoneFeature>> =
        features.mapNotNull { feature -> feature.geometry.bounds()?.let { Bounded(feature, it) } }

    /** Indexes a danger-zone layer for [inBounds]. Features with no coordinates are dropped. */
    fun indexDanger(features: List<DangerZoneFeature>): List<Bounded<DangerZoneFeature>> =
        features.mapNotNull { feature -> feature.geometry.bounds()?.let { Bounded(feature, it) } }

    /**
     * The features of an indexed layer that [bounds] shows, at most [limit] of them. Set
     * [fromCamera] only when [bounds] really is the camera's viewport: the sub-pixel skip measures a
     * feature against the box's span, which is meaningful for a viewport and misleading for a
     * reference box that is far larger than the screen. Both guards only ever bite at country-wide
     * zoom; see [MIN_VISIBLE_SPAN_FRACTION] and [MAX_DRAWN_FEATURES].
     */
    fun <T> inBounds(
        indexed: List<Bounded<T>>,
        bounds: GeoBounds,
        fromCamera: Boolean = true,
        limit: Int = MAX_DRAWN_FEATURES,
    ): List<T> {
        val viewSpan = maxOf(bounds.maxLat - bounds.minLat, bounds.maxLng - bounds.minLng)
        val minSpan = if (fromCamera) viewSpan * MIN_VISIBLE_SPAN_FRACTION else 0.0
        val visible = ArrayList<T>()
        for (entry in indexed) {
            if (visible.size >= limit) break
            val box = entry.bounds
            if (!box.intersects(bounds)) continue
            if (box.maxLat - box.minLat < minSpan && box.maxLng - box.minLng < minSpan) continue
            visible.add(entry.feature)
        }
        return visible
    }
}

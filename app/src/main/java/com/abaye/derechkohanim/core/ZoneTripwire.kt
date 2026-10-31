package com.abaye.derechkohanim.core

import com.abaye.derechkohanim.data.model.GeoJsonPolygon
import com.abaye.derechkohanim.data.model.LatLng
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The on-device tripwire (spec section 2): answers "is this point inside a forbidden zone?" and
 * "does the path ahead enter one, and how far away?" with no network at all, from the polygons
 * the app already holds in its local layer store.
 *
 * The technique follows DirGo's urban-area check (net.osmand.plus.views.mapwidgets.WazeUrban,
 * GPLv3): every polygon is converted ONCE to fixed-point integers (degrees * 100,000, about 1 m
 * resolution) with its bounding box precomputed, so a query is a few integer comparisons per
 * polygon to reject everything far away, then an even-odd ray cast done entirely in longs (the
 * crossing test is cross-multiplied instead of divided, so there is no floating point and no
 * division by zero). A query within [CACHE_EPSILON_DEG] (about 50 m) of the previous one reuses its
 * answer, which is what a 1 Hz GPS feed at walking or crawling speed mostly asks.
 *
 * All rings of a polygon take part in the ray cast, so holes are respected by the even-odd rule.
 *
 * Instances are immutable apart from the single-entry cache and are cheap to rebuild: build a new
 * one whenever the zone layers or the route corridor change.
 *
 * @param T what a hit reports back, e.g. the zone's name and stringency.
 */
class ZoneTripwire<T>(polygons: List<Pair<GeoJsonPolygon, T>>) {

    /** One polygon in fixed point. Ring arrays hold latitude and longitude separately. */
    private class FixedPolygon<T>(
        val ringLat: Array<IntArray>,
        val ringLng: Array<IntArray>,
        val minLat: Int,
        val maxLat: Int,
        val minLng: Int,
        val maxLng: Int,
        val payload: T,
    )

    private val polygons: List<FixedPolygon<T>> = polygons.mapNotNull { (geometry, payload) ->
        toFixed(geometry, payload)
    }

    /** Number of usable polygons (degenerate rings are dropped at construction). */
    val size: Int get() = polygons.size

    // Single-entry result cache, as in WazeUrban.
    private var cacheValid = false
    private var cacheLat = 0.0
    private var cacheLng = 0.0
    private var cacheResult: T? = null

    /** Cache hits since construction, exposed for tests. */
    var cacheHits: Int = 0
        private set

    /** The zone containing [point], or null. Uses the ~50 m cache. */
    fun containing(point: LatLng): T? {
        if (cacheValid &&
            abs(point.lat - cacheLat) < CACHE_EPSILON_DEG &&
            abs(point.lng - cacheLng) < CACHE_EPSILON_DEG
        ) {
            cacheHits++
            return cacheResult
        }
        val result = lookup(toFixed(point.lat), toFixed(point.lng))
        cacheLat = point.lat
        cacheLng = point.lng
        cacheResult = result
        cacheValid = true
        return result
    }

    /** The zone containing [point], bypassing the cache. */
    fun containingExact(point: LatLng): T? = lookup(toFixed(point.lat), toFixed(point.lng))

    /** A zone met while walking a path, with the distance along the path to the first point inside. */
    data class Hit<T>(val zone: T, val distanceMeters: Double, val point: LatLng)

    /**
     * Walks [path] from its first point for at most [maxMeters], sampling every [stepMeters], and
     * returns the first zone entered with its distance along the path. The cache is not used: the
     * samples are all different points.
     */
    fun firstAlong(path: List<LatLng>, maxMeters: Double, stepMeters: Double = DEFAULT_STEP_METERS): Hit<T>? {
        if (path.isEmpty() || polygons.isEmpty()) return null
        lookupPoint(path[0])?.let { return Hit(it, 0.0, path[0]) }
        var travelled = 0.0
        for (i in 1 until path.size) {
            val a = path[i - 1]
            val b = path[i]
            val segment = Geo.distanceMeters(a, b)
            if (segment <= 0.0) continue
            val samples = (segment / stepMeters).roundToInt().coerceAtLeast(1)
            for (s in 1..samples) {
                val t = s.toDouble() / samples
                val along = travelled + segment * t
                if (along > maxMeters) return null
                val point = LatLng(a.lat + (b.lat - a.lat) * t, a.lng + (b.lng - a.lng) * t)
                lookupPoint(point)?.let { return Hit(it, along, point) }
            }
            travelled += segment
            if (travelled >= maxMeters) return null
        }
        return null
    }

    private fun lookupPoint(point: LatLng): T? = lookup(toFixed(point.lat), toFixed(point.lng))

    private fun lookup(lat: Int, lng: Int): T? {
        for (polygon in polygons) {
            // Bounding-box rejection: four integer comparisons rule out almost every polygon.
            if (lat < polygon.minLat || lat > polygon.maxLat || lng < polygon.minLng || lng > polygon.maxLng) {
                continue
            }
            if (inside(polygon, lat, lng)) return polygon.payload
        }
        return null
    }

    private fun inside(polygon: FixedPolygon<T>, lat: Int, lng: Int): Boolean {
        var inside = false
        for (r in polygon.ringLat.indices) {
            val ys = polygon.ringLat[r]
            val xs = polygon.ringLng[r]
            var j = ys.size - 1
            for (i in ys.indices) {
                val yi = ys[i]
                val yj = ys[j]
                // The edge straddles the horizontal line through the point.
                if ((yi > lat) != (yj > lat)) {
                    // Crossing x = xi + (xj - xi) * (lat - yi) / (yj - yi). The point is left of it
                    // when (lng - xi) * (yj - yi) < (xj - xi) * (lat - yi), with the inequality
                    // flipped when (yj - yi) is negative. Done in longs: no division, no overflow.
                    val dy = (yj - yi).toLong()
                    val lhs = (lng - xs[i]).toLong() * dy
                    val rhs = (xs[j] - xs[i]).toLong() * (lat - yi).toLong()
                    val left = if (dy > 0) lhs < rhs else lhs > rhs
                    if (left) inside = !inside
                }
                j = i
            }
        }
        return inside
    }

    companion object {
        /** Fixed-point scale: degrees * 100,000 is about 1.1 m of latitude per unit. */
        const val SCALE = 100_000.0

        /** A query this close to the previous one (both axes) reuses its answer: about 50 m. */
        const val CACHE_EPSILON_DEG = 0.0005

        /** Sample spacing when walking a path. Smaller than any real zone, so none is skipped. */
        const val DEFAULT_STEP_METERS = 10.0

        fun toFixed(degrees: Double): Int = (degrees * SCALE).roundToInt()

        private fun <T> toFixed(geometry: GeoJsonPolygon, payload: T): FixedPolygon<T>? {
            val rings = geometry.coordinates
                .map { ring -> ring.filter { it.size >= 2 && it[0].isFinite() && it[1].isFinite() } }
                .filter { it.size >= 3 }
            if (rings.isEmpty()) return null
            val ringLat = Array(rings.size) { r -> IntArray(rings[r].size) { toFixed(rings[r][it][1]) } }
            val ringLng = Array(rings.size) { r -> IntArray(rings[r].size) { toFixed(rings[r][it][0]) } }
            // The outer ring alone bounds the polygon; holes lie inside it.
            val outerLat = ringLat[0]
            val outerLng = ringLng[0]
            return FixedPolygon(
                ringLat = ringLat,
                ringLng = ringLng,
                minLat = outerLat.min(),
                maxLat = outerLat.max(),
                minLng = outerLng.min(),
                maxLng = outerLng.max(),
                payload = payload,
            )
        }
    }
}

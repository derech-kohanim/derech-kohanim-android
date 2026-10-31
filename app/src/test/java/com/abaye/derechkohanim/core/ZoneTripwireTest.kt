package com.abaye.derechkohanim.core

import com.abaye.derechkohanim.data.model.GeoJsonPolygon
import com.abaye.derechkohanim.data.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoneTripwireTest {

    /** A square around (lat, lng) with the given half side in degrees, as a closed GeoJSON ring. */
    private fun square(lat: Double, lng: Double, half: Double): GeoJsonPolygon = GeoJsonPolygon(
        coordinates = listOf(
            listOf(
                listOf(lng - half, lat - half),
                listOf(lng + half, lat - half),
                listOf(lng + half, lat + half),
                listOf(lng - half, lat + half),
                listOf(lng - half, lat - half),
            ),
        ),
    )

    private val center = LatLng(31.7780, 35.2350)
    private val tripwire = ZoneTripwire(listOf(square(center.lat, center.lng, 0.001) to "A"))

    @Test
    fun pointInsideIsFound() {
        assertEquals("A", tripwire.containingExact(center))
        assertEquals("A", tripwire.containingExact(LatLng(center.lat + 0.0009, center.lng - 0.0009)))
    }

    @Test
    fun pointOutsideIsNotFound() {
        assertNull(tripwire.containingExact(LatLng(center.lat + 0.01, center.lng)))
    }

    @Test
    fun pointJustAcrossTheEdgeIsDistinguished() {
        // About 1 m either side of the eastern edge (fixed point resolution is ~1.1 m).
        val edgeLng = center.lng + 0.001
        assertEquals("A", tripwire.containingExact(LatLng(center.lat, edgeLng - 0.00001)))
        assertNull(tripwire.containingExact(LatLng(center.lat, edgeLng + 0.00001)))
    }

    @Test
    fun boundingBoxRejectsAPointInsideTheBoxButOutsideTheShape() {
        // A triangle: its bounding box contains the north-east corner, the triangle does not.
        val triangle = GeoJsonPolygon(
            coordinates = listOf(
                listOf(
                    listOf(35.0, 31.0),
                    listOf(35.01, 31.0),
                    listOf(35.0, 31.01),
                    listOf(35.0, 31.0),
                ),
            ),
        )
        val wire = ZoneTripwire(listOf(triangle to "T"))
        assertEquals("T", wire.containingExact(LatLng(31.002, 35.002)))
        assertNull(wire.containingExact(LatLng(31.009, 35.009)))
    }

    @Test
    fun holeIsRespected() {
        val outer = square(center.lat, center.lng, 0.002).coordinates[0]
        val hole = square(center.lat, center.lng, 0.0005).coordinates[0]
        val wire = ZoneTripwire(listOf(GeoJsonPolygon(coordinates = listOf(outer, hole)) to "H"))
        assertNull(wire.containingExact(center))
        assertEquals("H", wire.containingExact(LatLng(center.lat + 0.0015, center.lng)))
    }

    @Test
    fun cacheReusesTheAnswerWithinFiftyMeters() {
        val wire = ZoneTripwire(listOf(square(center.lat, center.lng, 0.001) to "A"))
        assertEquals("A", wire.containing(center))
        assertEquals(0, wire.cacheHits)
        // 0.0003 deg away on both axes: within the 0.0005 deg window, answered from the cache.
        assertEquals("A", wire.containing(LatLng(center.lat + 0.0003, center.lng + 0.0003)))
        assertEquals(1, wire.cacheHits)
        // Far away: a fresh lookup.
        assertNull(wire.containing(LatLng(center.lat + 0.01, center.lng)))
        assertEquals(1, wire.cacheHits)
    }

    @Test
    fun degeneratePolygonsAreDropped() {
        val line = GeoJsonPolygon(coordinates = listOf(listOf(listOf(35.0, 31.0), listOf(35.1, 31.1))))
        val wire = ZoneTripwire(listOf(line to "X"))
        assertEquals(0, wire.size)
        assertNull(wire.containingExact(LatLng(31.05, 35.05)))
    }

    @Test
    fun firstAlongFindsTheZoneAheadWithItsDistance() {
        // A path heading north into the square, starting ~555 m south of its southern edge.
        val start = LatLng(center.lat - 0.006, center.lng)
        val end = LatLng(center.lat + 0.006, center.lng)
        val hit = tripwire.firstAlong(listOf(start, end), maxMeters = 1_000.0)
        assertNotNull(hit)
        hit!!
        assertEquals("A", hit.zone)
        // Southern edge is 0.005 deg (~556 m) north of the start; sampling every 10 m.
        assertTrue("distance was ${hit.distanceMeters}", hit.distanceMeters in 545.0..570.0)
    }

    @Test
    fun firstAlongStopsAtTheLookAheadLimit() {
        val start = LatLng(center.lat - 0.006, center.lng)
        val end = LatLng(center.lat + 0.006, center.lng)
        assertNull(tripwire.firstAlong(listOf(start, end), maxMeters = 300.0))
    }

    @Test
    fun firstAlongReportsZeroWhenAlreadyInside() {
        val hit = tripwire.firstAlong(listOf(center, LatLng(center.lat + 0.01, center.lng)), 500.0)
        assertEquals(0.0, hit!!.distanceMeters, 0.0)
    }
}

package com.abaye.derechkohanim.core

import com.abaye.derechkohanim.data.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteProjectionTest {

    // A straight west-east segment about 1.9 km long, then a turn north.
    private val route = listOf(
        LatLng(31.77, 35.20),
        LatLng(31.77, 35.22),
        LatLng(31.79, 35.22),
    )

    @Test
    fun projectsOntoTheMiddleOfALongSegment() {
        // 0.001 deg (~111 m) north of the middle of the first segment.
        val foot = RouteProjection.nearest(LatLng(31.771, 35.21), route)!!
        assertEquals(0, foot.segmentIndex)
        assertEquals(31.77, foot.point.lat, 1e-6)
        assertEquals(35.21, foot.point.lng, 1e-6)
        assertEquals(111.0, foot.distanceMeters, 2.0)
    }

    @Test
    fun clampsToTheVertexBeyondTheEnd() {
        val foot = RouteProjection.nearest(LatLng(31.80, 35.22), route)!!
        assertEquals(1, foot.segmentIndex)
        assertEquals(31.79, foot.point.lat, 1e-6)
    }

    @Test
    fun picksTheCloserSegment() {
        val foot = RouteProjection.nearest(LatLng(31.78, 35.221), route)!!
        assertEquals(1, foot.segmentIndex)
    }

    @Test
    fun emptyRouteHasNoFoot() {
        assertNull(RouteProjection.nearest(LatLng(31.0, 35.0), emptyList()))
    }
}

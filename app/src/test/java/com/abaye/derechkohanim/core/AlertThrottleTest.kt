package com.abaye.derechkohanim.core

import com.abaye.derechkohanim.data.model.LatLng
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertThrottleTest {

    private var now = 0L
    private val throttle = AlertThrottle(minIntervalMs = 60_000, minDistanceMeters = 300.0, clock = { now })
    private val here = LatLng(31.77, 35.23)
    private val far = LatLng(31.78, 35.23) // ~1.1 km north

    @Test
    fun firstFiringIsAllowed() {
        assertTrue(throttle.tryFire("cam-1", here))
    }

    @Test
    fun repeatNeedsBothTimeAndDistance() {
        assertTrue(throttle.tryFire("cam-1", here))
        now = 120_000
        assertFalse("enough time but no movement", throttle.tryFire("cam-1", here))
        now = 10_000
        assertFalse("movement but not enough time", throttle.tryFire("cam-1", far))
        now = 200_000
        assertTrue(throttle.tryFire("cam-1", far))
    }

    @Test
    fun keysAreIndependent() {
        assertTrue(throttle.tryFire("cam-1", here))
        assertTrue(throttle.tryFire("cam-2", here))
    }

    @Test
    fun resetForgets() {
        assertTrue(throttle.tryFire("cam-1", here))
        throttle.reset()
        assertTrue(throttle.tryFire("cam-1", here))
    }
}

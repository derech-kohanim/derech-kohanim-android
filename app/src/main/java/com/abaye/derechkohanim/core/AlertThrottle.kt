package com.abaye.derechkohanim.core

import com.abaye.derechkohanim.data.model.LatLng

/**
 * Keeps a repeated alert from nagging. An alert identified by a key may fire again only once
 * BOTH the minimum time has passed and the vehicle has moved the minimum distance since it last
 * fired, so a driver stuck at a light next to a camera hears it once, and a driver circling back
 * past the same zone hears it again. Per-key, so two different cameras never mute each other.
 *
 * DirGo throttles its camera and tunnel voice alerts the same way (a minimum time and distance
 * per alert type).
 */
class AlertThrottle(
    private val minIntervalMs: Long,
    private val minDistanceMeters: Double,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private data class Fired(val atMs: Long, val location: LatLng?)

    private val fired = HashMap<String, Fired>()

    /**
     * True when [key] may fire now at [location]; records the firing when it does. A null
     * location (unknown position) is judged on time alone.
     */
    fun tryFire(key: String, location: LatLng?): Boolean {
        val now = clock()
        val last = fired[key]
        if (last != null) {
            val waited = now - last.atMs >= minIntervalMs
            val moved = if (location == null || last.location == null) {
                true
            } else {
                Geo.distanceMeters(location, last.location) >= minDistanceMeters
            }
            if (!(waited && moved)) return false
        }
        fired[key] = Fired(now, location)
        return true
    }

    /** Forgets everything, e.g. when a new route starts. */
    fun reset() = fired.clear()
}

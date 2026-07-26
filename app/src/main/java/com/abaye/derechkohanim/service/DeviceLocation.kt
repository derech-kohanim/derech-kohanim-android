package com.abaye.derechkohanim.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import com.abaye.derechkohanim.data.model.LatLng
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * One real GPS fix. Carries the sensor's own bearing and speed when the fix reports them, so a
 * consumer can orient a heading-up map from real motion instead of inferring everything.
 * [bearingDegrees] and [speedMps] are null when the fix does not carry them (e.g. standing still).
 */
data class DeviceFix(
    val location: LatLng,
    val bearingDegrees: Float?,
    val speedMps: Double?,
)

/**
 * Current position for one-shot needs like route planning, plus a continuous stream for live
 * navigation. The stream is fused-provider updates at roughly [updates]'s interval; it belongs
 * to the driving screen's lifetime and stops as soon as the collector goes away.
 */
@Singleton
class DeviceLocation @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    /**
     * A continuous feed of real device fixes for live navigation. High accuracy, about one fix a
     * second. The flow is cold and permission-guarded: with no foreground location permission it
     * completes immediately with no emissions, so a caller never crashes and never silently
     * falls back to a simulator. `removeLocationUpdates` runs when the collector stops.
     */
    @SuppressLint("MissingPermission")
    fun updates(intervalMs: Long = 1_000L): Flow<DeviceFix> = callbackFlow {
        if (!LocationPermissions.hasForeground(context)) {
            close()
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs)
            .setWaitForAccurateLocation(false)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                trySend(
                    DeviceFix(
                        location = LatLng(location.latitude, location.longitude),
                        bearingDegrees = if (location.hasBearing()) location.bearing else null,
                        speedMps = if (location.hasSpeed()) location.speed.toDouble() else null,
                    )
                )
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    /**
     * The cached fix first because it is instant, then an active fix as a fallback. `lastLocation`
     * is frequently null - right after boot, when GPS was just enabled, or when no app has asked
     * for location recently - and returning null there made route planning fail with no origin
     * even though the device clearly has a fix. The active request forces one.
     */
    @SuppressLint("MissingPermission")
    suspend fun lastKnown(): LatLng? {
        if (!LocationPermissions.hasForeground(context)) return null
        return cachedLocation() ?: currentLocation()
    }

    @SuppressLint("MissingPermission")
    private suspend fun cachedLocation(): LatLng? =
        suspendCancellableCoroutine { continuation ->
            client.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location?.let { LatLng(it.latitude, it.longitude) })
                }
                .addOnFailureListener { continuation.resume(null) }
        }

    /**
     * An active fix. The normal path is BALANCED accuracy, which is fast and network-assisted (wifi
     * cell) and works indoors for most users - unchanged behaviour. ONLY if that yields nothing do
     * we escalate: a null BALANCED fix is the symptom of a filtered network (e.g. NetFree) where
     * Google's network-location service is blocked, so we retry with HIGH_ACCURACY (GPS/GNSS, fully
     * on-device, no Google servers) and finally warm the GNSS via the continuous feed. This keeps
     * the extra GPS cost off the common path and only pays it when the standard method is blocked.
     * Can still be null with no sky view; the caller must handle it.
     */
    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): LatLng? =
        activeFix(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            ?: activeFix(Priority.PRIORITY_HIGH_ACCURACY)
            ?: withTimeoutOrNull(FEED_FIX_TIMEOUT_MS) {
                updates().map { it.location }.firstOrNull()
            }

    @SuppressLint("MissingPermission")
    private suspend fun activeFix(priority: Int): LatLng? =
        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            client.getCurrentLocation(priority, cancellation.token)
                .addOnSuccessListener { location ->
                    continuation.resume(location?.let { LatLng(it.latitude, it.longitude) })
                }
                .addOnFailureListener { continuation.resume(null) }
            continuation.invokeOnCancellation { cancellation.cancel() }
        }

    private companion object {
        /** How long to wait on the live GNSS feed for a first fix before giving up. */
        const val FEED_FIX_TIMEOUT_MS = 12_000L
    }
}

package com.abaye.derechkohanim.service

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import com.abaye.derechkohanim.data.model.LatLng
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Last known position only. Continuous updates belong to the foreground service and are not
 * wired in yet.
 */
@Singleton
class DeviceLocation @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission")
    suspend fun lastKnown(): LatLng? {
        if (!LocationPermissions.hasForeground(context)) return null
        return suspendCancellableCoroutine { continuation ->
            client.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location?.let { LatLng(it.latitude, it.longitude) })
                }
                .addOnFailureListener { continuation.resume(null) }
        }
    }
}

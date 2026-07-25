package com.abaye.derechkohanim.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A tiny connectivity check for offline-aware decisions (spec: reroute only when online). It
 * reports whether the device currently has an internet-capable, validated network. Requires
 * ACCESS_NETWORK_STATE, already declared in the manifest.
 */
@Singleton
class Connectivity @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun isOnline(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

package com.abaye.derechkohanim.data.model

import kotlinx.serialization.Serializable

/**
 * Small request bodies the endpoints need where the shared TypeScript contract does not
 * define a named type. Kept here, clearly separated from the mirrored contract types, and
 * called out in the README so they can be reconciled when the contract names them.
 */

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

@Serializable
data class AcceptTermsRequest(val version: String)

/** POST body for saving a place. The server assigns id and createdAt. */
@Serializable
data class CreateSavedPlaceRequest(
    val label: String,
    val category: PlaceCategory,
    val location: LatLng,
    val address: String? = null,
)

/** POST body for recording a recently used destination. */
@Serializable
data class CreateRecentPlaceRequest(
    val title: String,
    val subtitle: String? = null,
    val location: LatLng,
)

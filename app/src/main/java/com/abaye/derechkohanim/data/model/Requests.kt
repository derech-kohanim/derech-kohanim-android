package com.abaye.derechkohanim.data.model

import kotlinx.serialization.SerialName
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

/** The two-way end-of-trip rating. Serialised as "good"/"bad" to match the server contract. */
@Serializable
enum class NavFeedbackRating {
    @SerialName("good") GOOD,
    @SerialName("bad") BAD,
}

/**
 * POST body for end-of-trip feedback (POST /api/nav/feedback). Guarded by OptionalAuth on the
 * server, so it works anonymously for guests and is auto-associated when signed in. The optional
 * [text] carries the free-text "what went wrong?" note, typically only sent with a BAD rating.
 */
@Serializable
data class NavFeedbackRequest(
    val routeId: String,
    val rating: NavFeedbackRating,
    val text: String? = null,
)

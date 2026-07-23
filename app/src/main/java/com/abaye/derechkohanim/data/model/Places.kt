package com.abaye.derechkohanim.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors derech-kohanim-shared/src/types/places.ts. */

@Serializable
enum class PlaceQueryKind {
    @SerialName("text") TEXT,
    @SerialName("coordinates") COORDINATES,
    @SerialName("saved") SAVED,
    @SerialName("recent") RECENT,
}

@Serializable
enum class PlaceCategory {
    @SerialName("address") ADDRESS,
    @SerialName("city") CITY,
    @SerialName("poi") POI,
    @SerialName("synagogue") SYNAGOGUE,
    @SerialName("coordinates") COORDINATES,
    @SerialName("home") HOME,
    @SerialName("work") WORK,
    @SerialName("other") OTHER,
}

/** Character range in `title` that matched the query, for highlighting. */
@Serializable
data class MatchedRange(val start: Int, val length: Int)

/**
 * One row in the suggestion list. `title` must stand alone, `subtitle` disambiguates.
 */
@Serializable
data class PlaceSuggestion(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val category: PlaceCategory,
    val kind: PlaceQueryKind,
    /** Absent means a details lookup is needed before routing. */
    val location: LatLng? = null,
    val externalId: String? = null,
    /** Metres from the search origin, when one was supplied. */
    val distanceMeters: Double? = null,
    val matchedRanges: List<MatchedRange> = emptyList(),
)

@Serializable
data class PlaceSearchRequest(
    val query: String,
    val language: Language,
    /** Biases and sorts results. Usually the current location. */
    val near: LatLng? = null,
    /** Client-generated, kept stable while the user types one destination. */
    val sessionToken: String? = null,
    val limit: Int? = null,
)

@Serializable
data class PlaceSearchResponse(
    val query: String,
    val suggestions: List<PlaceSuggestion> = emptyList(),
    /** True when no upstream call was made. */
    val cached: Boolean = false,
)

@Serializable
data class PlaceDetailsRequest(
    val id: String,
    val language: Language,
    val sessionToken: String? = null,
)

@Serializable
data class SavedPlace(
    val id: String,
    val label: String,
    val category: PlaceCategory,
    val location: LatLng,
    val address: String? = null,
    val createdAt: String,
)

@Serializable
data class RecentPlace(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val location: LatLng,
    val lastUsedAt: String,
    val useCount: Int = 1,
)

fun SavedPlace.toSuggestion(): PlaceSuggestion = PlaceSuggestion(
    id = id,
    title = label,
    subtitle = address,
    category = category,
    kind = PlaceQueryKind.SAVED,
    location = location,
)

fun RecentPlace.toSuggestion(): PlaceSuggestion = PlaceSuggestion(
    id = id,
    title = title,
    subtitle = subtitle,
    category = PlaceCategory.OTHER,
    kind = PlaceQueryKind.RECENT,
    location = location,
)

package com.abaye.derechkohanim.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Mirrors derech-kohanim-shared/src/types/common.ts. */

@Serializable
data class LatLng(val lat: Double, val lng: Double)

@Serializable
enum class Language {
    @SerialName("he") HE,
    @SerialName("en") EN;

    val tag: String get() = if (this == HE) "he" else "en"
}

@Serializable
enum class ThemePreference {
    @SerialName("system") SYSTEM,
    @SerialName("light") LIGHT,
    @SerialName("dark") DARK,
}

/** Cumulative: MACHMIRIM includes TAHARAT includes BASIC. */
@Serializable
enum class Stringency {
    @SerialName("basic") BASIC,
    @SerialName("taharat") TAHARAT,
    @SerialName("machmirim") MACHMIRIM;

    val wire: String get() = name.lowercase()
}

@Serializable
enum class TravelMode {
    @SerialName("drive") DRIVE,
    @SerialName("walk") WALK,
}

/** [lng, lat] pairs, per RFC 7946. */
@Serializable
data class GeoJsonLineString(
    val type: String = "LineString",
    val coordinates: List<List<Double>>,
)

@Serializable
data class GeoJsonPolygon(
    val type: String = "Polygon",
    val coordinates: List<List<List<Double>>>,
)

/** Errors carry a stable code so the client translates; `message` is for logs only. */
@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, JsonElement>? = null,
)

/** GeoJSON stores longitude first, so the swap happens in exactly one place. */
fun List<Double>.toLatLng(): LatLng = LatLng(lat = this[1], lng = this[0])

fun GeoJsonLineString.points(): List<LatLng> = coordinates.map { it.toLatLng() }

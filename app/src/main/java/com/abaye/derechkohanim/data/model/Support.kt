package com.abaye.derechkohanim.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What a report is about. Mirrors SupportKind in the shared contract. */
@Serializable
enum class SupportKind {
    @SerialName("bug") BUG,
    @SerialName("route") ROUTE,
    @SerialName("data") DATA,
    @SerialName("suggestion") SUGGESTION,
}

/**
 * A report or feedback from a signed-in user, POST /api/support/reports. The server files it as
 * an issue in the private support repository. Mirrors SupportReportRequest.
 */
@Serializable
data class SupportReportRequest(
    val kind: SupportKind,
    /** What the user wrote, 1 to 4000 characters. */
    val message: String,
    val platform: String = "android",
    val appVersion: String,
    /** "<manufacturer> <model>, Android <release>". */
    val device: String? = null,
    val routeId: String? = null,
    val zoneId: Int? = null,
    val location: LatLng? = null,
    /** Only when true does the server include the account's email in the issue. */
    val replyByEmail: Boolean? = null,
)

@Serializable
data class SupportReportResponse(
    val id: String,
    /** The issue number to quote when writing again; null when no issue was filed. */
    val reference: Int? = null,
    /** True when the report was added to an open issue about the same zone. */
    val merged: Boolean = false,
)

/** The longest message the server accepts. */
const val SUPPORT_MESSAGE_MAX = 4000

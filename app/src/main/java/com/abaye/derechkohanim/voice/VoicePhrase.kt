package com.abaye.derechkohanim.voice

import android.content.Context
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.core.Formatters
import com.abaye.derechkohanim.data.model.RouteEventType
import com.abaye.derechkohanim.ui.icons.maneuverLabelRes
import kotlin.math.abs

/**
 * Assembles the SPOKEN instruction from structured parts, exactly as a real navigator does.
 * This is not the on-screen banner and must never be derived from it - the banner and the
 * phrase are two independent renderings of the same manoeuvre (spec 15.1).
 *
 * Shape, per language:
 *   "בעוד 200 מטר, פנה ימינה אל ז'בוטינסקי"
 *   "פנה ימינה עכשיו"
 *   "In 200 m, turn right onto Jabotinsky"
 *
 * Commas separate the segments so the device speech engine inserts a natural pause; the
 * recorded-clip path gets the same pauses from the clip boundaries.
 */
object VoicePhrase {

    /**
     * Distances at which a manoeuvre is announced. A closed ladder, not a continuous read
     * (spec 15.2). 0 stands for "now". Kept in sync with DISTANCE_LADDER_METERS in the
     * shared voice contract.
     */
    val ANNOUNCE_RUNGS_METERS = listOf(800, 500, 300, 200, 100, 50, 0)

    private const val NOW_THRESHOLD_METERS = 25.0

    /** Rounds a real distance to the nearest rung so the phrase uses a spoken value. */
    fun snapToRung(meters: Double): Int {
        if (meters <= NOW_THRESHOLD_METERS) return 0
        return ANNOUNCE_RUNGS_METERS.minByOrNull { abs(it - meters) } ?: 0
    }

    /** The spoken string for a driving manoeuvre. */
    fun maneuver(context: Context, action: String, distanceBucketMeters: Int, street: String?): String {
        val parts = mutableListOf<String>()
        if (distanceBucketMeters > 0) {
            parts += context.getString(
                R.string.nav_prefix_in,
            ).let { prefix ->
                "$prefix ${Formatters.distance(context, distanceBucketMeters.toDouble())}"
            }
        }

        val core = buildString {
            append(context.getString(maneuverLabelRes(action)))
            if (!street.isNullOrBlank() && speaksStreet(action)) {
                append(' ')
                append(context.getString(R.string.maneuver_onto_street, street))
            }
            if (distanceBucketMeters == 0) {
                append(' ')
                append(context.getString(R.string.nav_now))
            }
        }
        parts += core
        return parts.joinToString(", ")
    }

    /** The spoken string for an approaching event: a zone, or a user report. */
    fun event(context: Context, type: RouteEventType, title: String?, distanceBucketMeters: Int): String {
        val name = title?.takeIf { it.isNotBlank() && type == RouteEventType.ZONE }
            ?: context.getString(eventNameRes(type))
        return if (distanceBucketMeters == 0) {
            name
        } else {
            context.getString(
                R.string.event_ahead_announcement,
                name,
                Formatters.distance(context, distanceBucketMeters.toDouble()),
            )
        }
    }

    /** Arrival and departure never carry a street clause. */
    private fun speaksStreet(action: String): Boolean =
        action != "arrive" && action != "depart"

    private fun eventNameRes(type: RouteEventType): Int = when (type) {
        RouteEventType.ZONE -> R.string.event_zone
        RouteEventType.TRAFFIC -> R.string.report_traffic
        RouteEventType.POLICE -> R.string.report_police
        RouteEventType.ACCIDENT -> R.string.report_accident
        RouteEventType.HAZARD -> R.string.report_hazard
        RouteEventType.CLOSURE -> R.string.report_road_closure
        RouteEventType.SPEED_CAMERA -> R.string.event_speed_camera
        RouteEventType.RAIL_CROSSING -> R.string.event_rail_crossing
    }
}

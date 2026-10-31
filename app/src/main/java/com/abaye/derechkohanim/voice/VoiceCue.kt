package com.abaye.derechkohanim.voice

import com.abaye.derechkohanim.R

/**
 * A fixed spoken alert or status message: a zone warning, a road alert, a GPS or route-state
 * change. Each has one recorded clip key from the shared voice contract and a masculine and a
 * feminine text for the device speech fallback (identical where Hebrew needs no gender).
 *
 * [takesDistance] cues are spoken as "in N metres, <cue>" when a distance is known.
 */
enum class VoiceCue(
    val clipKey: String,
    val textRes: Int,
    val textResFemale: Int,
    val takesDistance: Boolean = false,
) {
    ZONE_ENTER("zone.enter", R.string.voice_cue_zone_enter, R.string.voice_cue_zone_enter_f),
    ZONE_EXIT("zone.exit", R.string.voice_cue_zone_exit, R.string.voice_cue_zone_exit_f),
    ZONE_AHEAD("zone.ahead", R.string.voice_cue_zone_ahead, R.string.voice_cue_zone_ahead_f, takesDistance = true),
    SPEED_CAMERA(
        "alert.speed_camera",
        R.string.voice_cue_speed_camera,
        R.string.voice_cue_speed_camera_f,
        takesDistance = true,
    ),
    RAIL_CROSSING(
        "alert.rail_crossing",
        R.string.voice_cue_rail_crossing,
        R.string.voice_cue_rail_crossing_f,
        takesDistance = true,
    ),
    SPEEDING("alert.speeding", R.string.voice_cue_speeding, R.string.voice_cue_speeding_f),
    GPS_LOST("misc.gps_lost", R.string.voice_cue_gps_lost, R.string.voice_cue_gps_lost_f),
    GPS_RECOVERED("misc.gps_recovered", R.string.voice_cue_gps_recovered, R.string.voice_cue_gps_recovered_f),
    BACK_ON_ROUTE("misc.back_on_route", R.string.voice_cue_back_on_route, R.string.voice_cue_back_on_route_f),
    RETURN_TO_ROUTE(
        "misc.return_to_route",
        R.string.voice_cue_return_to_route,
        R.string.voice_cue_return_to_route_f,
    ),
    ;

    fun textFor(female: Boolean): Int = if (female) textResFemale else textRes

    companion object {
        /** Every cue clip, so a drive can prefetch them all before it might lose connectivity. */
        val ALL_CLIP_KEYS: List<String> = entries.map { it.clipKey }
    }
}

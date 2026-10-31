package com.abaye.derechkohanim.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.data.model.PlaceCategory

fun categoryIcon(category: PlaceCategory): ImageVector = when (category) {
    PlaceCategory.ADDRESS -> Icons.Filled.Place
    PlaceCategory.CITY -> Icons.Filled.LocationCity
    PlaceCategory.POI -> Icons.Filled.Star
    PlaceCategory.SYNAGOGUE -> Icons.Filled.AccountBalance
    PlaceCategory.COORDINATES -> Icons.Filled.MyLocation
    PlaceCategory.HOME -> Icons.Filled.Home
    PlaceCategory.WORK -> Icons.Filled.Work
    PlaceCategory.OTHER -> Icons.Filled.Place
}

@Composable
fun categoryLabel(category: PlaceCategory): String = stringResource(
    when (category) {
        PlaceCategory.ADDRESS -> R.string.category_address
        PlaceCategory.CITY -> R.string.category_city
        PlaceCategory.POI -> R.string.category_poi
        PlaceCategory.SYNAGOGUE -> R.string.category_synagogue
        PlaceCategory.COORDINATES -> R.string.category_coordinates
        PlaceCategory.HOME -> R.string.category_home
        PlaceCategory.WORK -> R.string.category_work
        PlaceCategory.OTHER -> R.string.category_other
    }
)

/**
 * A manoeuvre icon is one arrow rotated by the turn angle rather than a separate asset per
 * action. Rotation is unaffected by layout direction, which is exactly what is needed:
 * "turn right" stays a right turn in an RTL interface (spec 14.1).
 */
data class ManeuverGlyph(val icon: ImageVector, val rotationDegrees: Float)

fun maneuverGlyph(action: String): ManeuverGlyph = when (action) {
    "turn_right" -> ManeuverGlyph(Icons.Filled.ArrowUpward, 90f)
    "turn_left" -> ManeuverGlyph(Icons.Filled.ArrowUpward, -90f)
    "slight_right" -> ManeuverGlyph(Icons.Filled.ArrowUpward, 45f)
    "slight_left" -> ManeuverGlyph(Icons.Filled.ArrowUpward, -45f)
    "sharp_right" -> ManeuverGlyph(Icons.Filled.ArrowUpward, 135f)
    "sharp_left" -> ManeuverGlyph(Icons.Filled.ArrowUpward, -135f)
    "keep_right" -> ManeuverGlyph(Icons.Filled.ArrowUpward, 25f)
    "keep_left" -> ManeuverGlyph(Icons.Filled.ArrowUpward, -25f)
    "merge" -> ManeuverGlyph(Icons.Filled.ArrowUpward, 30f)
    "uturn" -> ManeuverGlyph(Icons.Filled.Refresh, 0f)
    "roundabout", "roundabout_enter", "roundabout_exit" -> ManeuverGlyph(Icons.Filled.Refresh, 0f)
    "ramp_right" -> ManeuverGlyph(Icons.Filled.ArrowUpward, 30f)
    "ramp_left" -> ManeuverGlyph(Icons.Filled.ArrowUpward, -30f)
    "depart" -> ManeuverGlyph(Icons.Filled.MyLocation, 0f)
    "arrive" -> ManeuverGlyph(Icons.Filled.Flag, 0f)
    "continue" -> ManeuverGlyph(Icons.Filled.ArrowUpward, 0f)
    else -> ManeuverGlyph(Icons.Filled.ArrowUpward, 0f)
}

fun maneuverLabelRes(action: String): Int = when (action) {
    "depart" -> R.string.maneuver_depart
    "turn_left" -> R.string.maneuver_turn_left
    "turn_right" -> R.string.maneuver_turn_right
    "slight_left" -> R.string.maneuver_slight_left
    "slight_right" -> R.string.maneuver_slight_right
    "sharp_left" -> R.string.maneuver_sharp_left
    "sharp_right" -> R.string.maneuver_sharp_right
    "keep_left" -> R.string.maneuver_keep_left
    "keep_right" -> R.string.maneuver_keep_right
    "continue" -> R.string.maneuver_continue
    "uturn" -> R.string.maneuver_uturn
    "roundabout", "roundabout_enter" -> R.string.maneuver_roundabout
    "merge" -> R.string.maneuver_merge
    "arrive" -> R.string.maneuver_arrive
    else -> R.string.maneuver_unknown
}

val zoneWarningIcon: ImageVector = Icons.Filled.Warning

fun eventIcon(type: com.abaye.derechkohanim.data.model.RouteEventType): ImageVector = when (type) {
    com.abaye.derechkohanim.data.model.RouteEventType.ZONE -> Icons.Filled.Warning
    com.abaye.derechkohanim.data.model.RouteEventType.TRAFFIC -> Icons.Filled.Traffic
    com.abaye.derechkohanim.data.model.RouteEventType.POLICE -> Icons.Filled.LocalPolice
    com.abaye.derechkohanim.data.model.RouteEventType.ACCIDENT -> Icons.Filled.CarCrash
    com.abaye.derechkohanim.data.model.RouteEventType.HAZARD -> Icons.Filled.Warning
    com.abaye.derechkohanim.data.model.RouteEventType.CLOSURE -> Icons.Filled.Block
    com.abaye.derechkohanim.data.model.RouteEventType.SPEED_CAMERA -> Icons.Filled.PhotoCamera
    com.abaye.derechkohanim.data.model.RouteEventType.RAIL_CROSSING -> Icons.Filled.Train
}

fun eventLabelRes(type: com.abaye.derechkohanim.data.model.RouteEventType): Int = when (type) {
    com.abaye.derechkohanim.data.model.RouteEventType.ZONE -> R.string.event_zone
    com.abaye.derechkohanim.data.model.RouteEventType.TRAFFIC -> R.string.report_traffic
    com.abaye.derechkohanim.data.model.RouteEventType.POLICE -> R.string.report_police
    com.abaye.derechkohanim.data.model.RouteEventType.ACCIDENT -> R.string.report_accident
    com.abaye.derechkohanim.data.model.RouteEventType.HAZARD -> R.string.report_hazard
    com.abaye.derechkohanim.data.model.RouteEventType.CLOSURE -> R.string.report_road_closure
    com.abaye.derechkohanim.data.model.RouteEventType.SPEED_CAMERA -> R.string.event_speed_camera
    com.abaye.derechkohanim.data.model.RouteEventType.RAIL_CROSSING -> R.string.event_rail_crossing
}

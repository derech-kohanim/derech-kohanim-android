package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.RouteEvent
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.TrafficLevel
import com.abaye.derechkohanim.data.model.TrafficSegment
import com.abaye.derechkohanim.data.model.ZoneFeature
import com.abaye.derechkohanim.ui.theme.DkTheme
import com.google.android.gms.maps.model.LatLng as GmsLatLng

/**
 * Owns the map camera so the surrounding screen can drive it: follow the driver, toggle
 * heading-up, zoom, or pull back to an overview. Camera moves are instant `move` calls
 * wrapped defensively, since a move before the map has a size would otherwise throw.
 */
class DkMapController(val cameraPositionState: CameraPositionState) {

    /** Heading-up rotates the map with the drive; north-up keeps north at the top. */
    var headingUp by mutableStateOf(true)

    fun follow(location: LatLng, bearingDegrees: Float) {
        val bearing = if (headingUp) bearingDegrees else 0f
        // Preserve whatever zoom the driver set with the zoom buttons.
        val currentZoom = cameraPositionState.position.zoom.takeIf { it > 2f } ?: FOLLOW_ZOOM
        move(
            CameraPosition.Builder()
                .target(GmsLatLng(location.lat, location.lng))
                .zoom(currentZoom)
                .bearing(bearing)
                .tilt(if (headingUp) FOLLOW_TILT else 0f)
                .build()
        )
    }

    fun center(location: LatLng, zoom: Float = FOLLOW_ZOOM) {
        move(CameraPosition.fromLatLngZoom(GmsLatLng(location.lat, location.lng), zoom))
    }

    fun zoomBy(delta: Float) {
        val current = cameraPositionState.position
        move(
            CameraPosition.Builder(current)
                .zoom((current.zoom + delta).coerceIn(MIN_ZOOM, MAX_ZOOM))
                .build()
        )
    }

    /** Pulls the camera back to frame the whole set of points, Waze's route overview. */
    fun overview(points: List<LatLng>) {
        if (points.isEmpty()) return
        val builder = LatLngBounds.builder()
        points.forEach { builder.include(GmsLatLng(it.lat, it.lng)) }
        runCatching {
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngBounds(builder.build(), OVERVIEW_PADDING_PX)
            )
        }
    }

    private fun move(position: CameraPosition) {
        runCatching { cameraPositionState.position = position }
    }

    companion object {
        const val FOLLOW_ZOOM = 16.5f
        const val FOLLOW_TILT = 45f
        private const val MIN_ZOOM = 5f
        private const val MAX_ZOOM = 20f
        private const val OVERVIEW_PADDING_PX = 160
    }
}

@Composable
fun rememberDkMapController(): DkMapController {
    val cameraPositionState = rememberCameraPositionState()
    return remember { DkMapController(cameraPositionState) }
}

@Composable
fun DkMap(
    center: LatLng,
    modifier: Modifier = Modifier,
    controller: DkMapController? = null,
    zoom: Float = DEFAULT_ZOOM,
    routePoints: List<LatLng> = emptyList(),
    naiveRoutePoints: List<LatLng> = emptyList(),
    trafficSegments: List<TrafficSegment> = emptyList(),
    eventMarkers: List<RouteEvent> = emptyList(),
    zones: List<ZoneFeature> = emptyList(),
    currentLocation: LatLng? = null,
    routeIsClean: Boolean = true,
    interactive: Boolean = true,
) {
    val colors = DkTheme.colors

    if (LocalInspectionMode.current) {
        // Play services do not run inside a @Preview, so a token-coloured stand-in is drawn.
        Box(
            modifier = modifier.background(colors.surfaceMuted),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "map",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
            )
        }
        return
    }

    val cameraPositionState = controller?.cameraPositionState ?: rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(GmsLatLng(center.lat, center.lng), zoom)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = false),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false,
            scrollGesturesEnabled = interactive,
            rotationGesturesEnabled = interactive,
            zoomGesturesEnabled = interactive,
            tiltGesturesEnabled = interactive,
        ),
    ) {
        // Zones sit beneath the route. Each feature is coloured by its own stringency layer
        // (spec: pink basic, brown taharat, orange machmirim), translucent fill, opaque line.
        zones.forEach { feature ->
            val outer = feature.geometry.coordinates.firstOrNull().orEmpty().map {
                GmsLatLng(it[1], it[0])
            }
            if (outer.size >= 3) {
                val layerColor = zoneColor(feature.properties.stringency, colors)
                Polygon(
                    points = outer,
                    fillColor = layerColor.copy(alpha = ZONE_FILL_ALPHA),
                    strokeColor = layerColor,
                    strokeWidth = ZONE_STROKE_WIDTH,
                )
            }
        }
        if (naiveRoutePoints.isNotEmpty()) {
            Polyline(
                points = naiveRoutePoints.map { GmsLatLng(it.lat, it.lng) },
                color = colors.routeNaive,
                width = ROUTE_WIDTH * 0.7f,
            )
        }
        if (routePoints.isNotEmpty()) {
            Polyline(
                points = routePoints.map { GmsLatLng(it.lat, it.lng) },
                color = if (routeIsClean) colors.primary else colors.danger,
                width = ROUTE_WIDTH,
            )
        }
        // Traffic is drawn over the base route so a loaded stretch stands out, Waze style.
        trafficSegments.forEach { segment ->
            val last = routePoints.lastIndex.coerceAtLeast(0)
            val from = segment.fromIndex.coerceIn(0, last)
            val to = segment.toIndex.coerceIn(from, last)
            val slice = routePoints.slice(from..to)
            if (slice.size >= 2) {
                Polyline(
                    points = slice.map { GmsLatLng(it.lat, it.lng) },
                    color = trafficColor(segment.level, colors),
                    width = ROUTE_WIDTH,
                )
            }
        }
        eventMarkers.forEach { event ->
            Marker(
                state = rememberMarkerState(
                    key = event.id,
                    position = GmsLatLng(event.location.lat, event.location.lng),
                ),
                icon = BitmapDescriptorFactory.defaultMarker(markerHue(event)),
                title = event.labelHe ?: event.label,
            )
        }
        currentLocation?.let {
            Marker(
                state = rememberMarkerState(
                    key = "me",
                    position = GmsLatLng(it.lat, it.lng),
                ),
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
            )
        }
    }
}

/** Per-layer zone colour, so the three stringency layers are visually distinct. */
fun zoneColor(stringency: Stringency, colors: com.abaye.derechkohanim.ui.theme.DkColors): Color =
    when (stringency) {
        Stringency.BASIC -> colors.zoneBasic
        Stringency.TAHARAT -> colors.zoneTaharat
        Stringency.MACHMIRIM -> colors.zoneMachmirim
    }

private fun trafficColor(level: TrafficLevel, colors: com.abaye.derechkohanim.ui.theme.DkColors): Color =
    when (level) {
        TrafficLevel.FREE -> colors.success
        TrafficLevel.MODERATE -> colors.warning
        TrafficLevel.HEAVY -> colors.danger
    }

private fun markerHue(event: RouteEvent): Float = when (event.type) {
    com.abaye.derechkohanim.data.model.RouteEventType.ZONE -> BitmapDescriptorFactory.HUE_RED
    com.abaye.derechkohanim.data.model.RouteEventType.TRAFFIC -> BitmapDescriptorFactory.HUE_ORANGE
    com.abaye.derechkohanim.data.model.RouteEventType.POLICE -> BitmapDescriptorFactory.HUE_BLUE
    com.abaye.derechkohanim.data.model.RouteEventType.ACCIDENT -> BitmapDescriptorFactory.HUE_ORANGE
    com.abaye.derechkohanim.data.model.RouteEventType.HAZARD -> BitmapDescriptorFactory.HUE_YELLOW
    com.abaye.derechkohanim.data.model.RouteEventType.CLOSURE -> BitmapDescriptorFactory.HUE_VIOLET
}

private const val DEFAULT_ZOOM = 14f
private const val ROUTE_WIDTH = 16f
private const val ZONE_FILL_ALPHA = 0.28f
private const val ZONE_STROKE_WIDTH = 4f

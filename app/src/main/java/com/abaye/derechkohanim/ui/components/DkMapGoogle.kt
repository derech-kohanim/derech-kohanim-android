package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.abaye.derechkohanim.core.ZoneFilter
import com.abaye.derechkohanim.data.model.DangerZoneFeature
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.RouteEvent
import com.abaye.derechkohanim.data.model.RouteEventType
import com.abaye.derechkohanim.data.model.TrafficSegment
import com.abaye.derechkohanim.data.model.ZoneFeature
import com.abaye.derechkohanim.service.MapProviderState
import com.abaye.derechkohanim.ui.theme.DkColors
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
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
import kotlinx.coroutines.delay
import com.google.android.gms.maps.model.LatLng as GmsLatLng

/** Wraps a maps-compose [CameraPositionState] as a provider-agnostic camera backend. */
private class GoogleCameraBackend(val cameraPositionState: CameraPositionState) : MapCameraBackend {
    override fun snapshot(): CameraSnapshot? {
        val p = cameraPositionState.position
        return CameraSnapshot(
            target = LatLng(p.target.latitude, p.target.longitude),
            zoom = p.zoom,
            bearing = p.bearing,
            tilt = p.tilt,
        )
    }

    override fun move(target: LatLng, zoom: Float, bearing: Float, tilt: Float) {
        runCatching {
            cameraPositionState.position = CameraPosition.Builder()
                .target(GmsLatLng(target.lat, target.lng))
                .zoom(zoom)
                .bearing(bearing)
                .tilt(tilt)
                .build()
        }
    }

    override fun overview(points: List<LatLng>, paddingPx: Int) {
        val builder = LatLngBounds.builder()
        points.forEach { builder.include(GmsLatLng(it.lat, it.lng)) }
        runCatching {
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(builder.build(), paddingPx))
        }
    }
}

/**
 * The Google (maps-compose) renderer - the original DkMap body. In addition to drawing every
 * overlay it wires the camera to the shared [controller] and detects a Google-Maps load failure:
 * if `onMapLoaded` does not fire within [GOOGLE_LOAD_TIMEOUT_MS] (typical on NetFree, where Play
 * Services rejects the intercepting cert), it tells [MapProviderState] so AUTOMATIC can fall back
 * to OpenFreeMap.
 */
@Composable
internal fun GoogleDkMap(
    controller: DkMapController,
    modifier: Modifier,
    routePoints: List<LatLng>,
    naiveRoutePoints: List<LatLng>,
    alternativeRoutes: List<List<LatLng>>,
    unavoidedRoutePoints: List<LatLng>,
    trafficSegments: List<TrafficSegment>,
    eventMarkers: List<RouteEvent>,
    zones: List<ZoneFeature>,
    dangerZones: List<DangerZoneFeature>,
    onZoneClick: ((ZoneTapInfo) -> Unit)?,
    onMapClick: ((LatLng) -> Unit)?,
    onVisibleBoundsChange: ((ZoneFilter.GeoBounds) -> Unit)?,
    currentLocation: LatLng?,
    routeIsClean: Boolean,
    interactive: Boolean,
    providerState: MapProviderState?,
    colors: DkColors,
) {
    val cameraPositionState = rememberCameraPositionState()
    val backend = remember(cameraPositionState) { GoogleCameraBackend(cameraPositionState) }
    DisposableEffect(controller, backend) {
        controller.attach(backend)
        onDispose { controller.detach(backend) }
    }

    // Google-Maps load-failure detection. If the map has not reported loaded within the timeout
    // while it is the active provider, mark Google as failed for the session so AUTOMATIC can
    // switch. Runs only when a provider state is present (i.e. not in isolation/preview).
    var mapLoaded by remember { mutableStateOf(false) }
    if (providerState != null) {
        LaunchedEffect(Unit) {
            delay(GOOGLE_LOAD_TIMEOUT_MS)
            if (!mapLoaded) providerState.reportGoogleMapLoadFailed()
        }
    }

    // Report what the camera actually shows, so the caller can clip the zone layers to it. The
    // projection only describes a real region once the map has laid out, hence keying on mapLoaded;
    // from there every camera change feeds the throttle, which keeps the filtering off the per-frame
    // path of a drag or a fling without waiting for the whole gesture to end.
    if (onVisibleBoundsChange != null) {
        val report by rememberUpdatedState(onVisibleBoundsChange)
        LaunchedEffect(cameraPositionState, mapLoaded) {
            if (!mapLoaded) return@LaunchedEffect
            snapshotFlow { cameraPositionState.position }.collectThrottled(CAMERA_REPORT_MS) {
                val box = cameraPositionState.projection?.visibleRegion?.latLngBounds
                    ?: return@collectThrottled
                ZoneFilter.viewport(
                    south = box.southwest.latitude,
                    north = box.northeast.latitude,
                    west = box.southwest.longitude,
                    east = box.northeast.longitude,
                )?.let(report)
            }
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = false),
        onMapLoaded = {
            mapLoaded = true
            providerState?.reportGoogleMapLoaded()
        },
        // A tap on open map (a Polygon click is consumed by the polygon, so this fires only off a
        // zone) reports the point, driving the pick-start flow.
        onMapClick = onMapClick?.let { cb -> { latLng -> cb(LatLng(latLng.latitude, latLng.longitude)) } },
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
        // (pink basic, brown taharat, orange machmirim), translucent fill, opaque line, and is
        // tappable to show its full detail.
        zones.forEach { feature ->
            val outer = feature.geometry.coordinates.firstOrNull().orEmpty().map {
                GmsLatLng(it[1], it[0])
            }
            if (outer.size >= 3) {
                val layerColor = zoneColor(feature.properties.stringency, colors)
                val info = feature.properties.toTapInfo()
                Polygon(
                    points = outer,
                    fillColor = layerColor.copy(alpha = ZONE_FILL_ALPHA),
                    strokeColor = layerColor,
                    strokeWidth = ZONE_STROKE_WIDTH,
                    clickable = onZoneClick != null,
                    onClick = { onZoneClick?.invoke(info) },
                )
            }
        }
        // Danger zones (Area A) are a separate layer, drawn in the distinct danger colour with
        // a heavy dashed outline so they never read as one of the halachic stringency layers.
        dangerZones.forEach { feature ->
            val outer = feature.geometry.coordinates.firstOrNull().orEmpty().map {
                GmsLatLng(it[1], it[0])
            }
            if (outer.size >= 3) {
                val info = feature.properties.toTapInfo()
                Polygon(
                    points = outer,
                    fillColor = colors.zoneDanger.copy(alpha = ZONE_FILL_ALPHA),
                    strokeColor = colors.zoneDanger,
                    strokeWidth = ZONE_STROKE_WIDTH + 1f,
                    strokePattern = listOf(Dash(24f), Gap(14f)),
                    clickable = onZoneClick != null,
                    onClick = { onZoneClick?.invoke(info) },
                )
            }
        }
        if (naiveRoutePoints.isNotEmpty()) {
            Polyline(
                points = naiveRoutePoints.map { GmsLatLng(it.lat, it.lng) },
                color = colors.routeNaive,
                width = ROUTE_WIDTH * 0.7f,
                zIndex = ROUTE_Z_ALT,
            )
        }
        // The unavoided route (no zone avoidance) is drawn in a warning style, dashed, so it
        // reads clearly as "for comparison, crosses zones, not navigable".
        if (unavoidedRoutePoints.isNotEmpty()) {
            Polyline(
                points = unavoidedRoutePoints.map { GmsLatLng(it.lat, it.lng) },
                color = colors.warning,
                width = ROUTE_WIDTH * 0.8f,
                pattern = listOf(Dash(28f), Gap(18f)),
                zIndex = ROUTE_Z_ALT,
            )
        }
        // Alternatives sit under the selected route, faded and grey, so the choice stands out.
        alternativeRoutes.forEach { alt ->
            if (alt.isNotEmpty()) {
                Polyline(
                    points = alt.map { GmsLatLng(it.lat, it.lng) },
                    color = ROUTE_ALT_COLOR.copy(alpha = ALT_ROUTE_ALPHA),
                    width = ROUTE_WIDTH,
                    zIndex = ROUTE_Z_ALT,
                )
            }
        }
        // The selected route is drawn as two stacked lines - a darker, wider casing beneath a
        // brighter, narrower active line - so it reads with depth, Waze style. zIndex keeps the
        // casing below the active line and both above the alternatives. An unverified route keeps
        // its warning treatment: a danger active over a darker danger casing.
        if (routePoints.isNotEmpty()) {
            val pts = routePoints.map { GmsLatLng(it.lat, it.lng) }
            val casingColor = if (routeIsClean) ROUTE_CASING_COLOR else ROUTE_CASING_DANGER
            val activeColor = if (routeIsClean) ROUTE_ACTIVE_COLOR else colors.danger
            Polyline(
                points = pts,
                color = casingColor,
                width = ROUTE_CASING_WIDTH,
                zIndex = ROUTE_Z_CASING,
            )
            Polyline(
                points = pts,
                color = activeColor,
                width = ROUTE_WIDTH,
                zIndex = ROUTE_Z_ACTIVE,
            )
        }
        // Traffic is drawn over the active route so a loaded stretch stands out, Waze style.
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
                    zIndex = ROUTE_Z_TRAFFIC,
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

private fun markerHue(event: RouteEvent): Float = when (event.type) {
    RouteEventType.ZONE -> BitmapDescriptorFactory.HUE_RED
    RouteEventType.TRAFFIC -> BitmapDescriptorFactory.HUE_ORANGE
    RouteEventType.POLICE -> BitmapDescriptorFactory.HUE_BLUE
    RouteEventType.ACCIDENT -> BitmapDescriptorFactory.HUE_ORANGE
    RouteEventType.HAZARD -> BitmapDescriptorFactory.HUE_YELLOW
    RouteEventType.CLOSURE -> BitmapDescriptorFactory.HUE_VIOLET
    RouteEventType.SPEED_CAMERA -> BitmapDescriptorFactory.HUE_AZURE
    RouteEventType.RAIL_CROSSING -> BitmapDescriptorFactory.HUE_ROSE
}

/** How long Google Maps has to report loaded before it is judged failed for the session. */
private const val GOOGLE_LOAD_TIMEOUT_MS = 7_000L

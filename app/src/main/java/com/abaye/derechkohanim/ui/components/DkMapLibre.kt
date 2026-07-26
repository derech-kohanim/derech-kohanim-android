package com.abaye.derechkohanim.ui.components

import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.abaye.derechkohanim.BuildConfig
import com.abaye.derechkohanim.data.model.DangerZoneFeature
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.RouteEvent
import com.abaye.derechkohanim.data.model.RouteEventType
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.TrafficSegment
import com.abaye.derechkohanim.data.model.ZoneFeature
import com.abaye.derechkohanim.ui.theme.DkColors
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition as MlCameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MlLatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.roundToInt

/** Wraps a [MapLibreMap] camera as a provider-agnostic backend. */
private class MapLibreCameraBackend(val map: MapLibreMap) : MapCameraBackend {
    override fun snapshot(): CameraSnapshot? {
        val p = map.cameraPosition
        val t = p.target ?: return null
        return CameraSnapshot(
            target = LatLng(t.latitude, t.longitude),
            zoom = p.zoom.toFloat(),
            bearing = p.bearing.toFloat(),
            tilt = p.tilt.toFloat(),
        )
    }

    override fun move(target: LatLng, zoom: Float, bearing: Float, tilt: Float) {
        runCatching {
            val pos = MlCameraPosition.Builder()
                .target(MlLatLng(target.lat, target.lng))
                .zoom(zoom.toDouble())
                .bearing(bearing.toDouble())
                .tilt(tilt.toDouble())
                .build()
            map.moveCamera(CameraUpdateFactory.newCameraPosition(pos))
        }
    }

    override fun overview(points: List<LatLng>, paddingPx: Int) {
        if (points.isEmpty()) return
        runCatching {
            val builder = LatLngBounds.Builder()
            points.forEach { builder.include(MlLatLng(it.lat, it.lng)) }
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), paddingPx))
        }
    }
}

/**
 * The MapLibre / OpenFreeMap renderer - the NetFree fallback. It wraps a MapLibre [MapView] in an
 * [AndroidView] with correct Compose lifecycle handling, loads the style from
 * [BuildConfig.MAPLIBRE_STYLE_URL], forces Hebrew labels, and re-implements every overlay the
 * Google renderer draws with GeoJSON sources + fill/line/circle layers. It draws through the
 * app's own OkHttp/TLS stack, so it honours network_security_config.xml and works on NetFree.
 *
 * Overlay parity notes vs the Google renderer:
 *  - zones/danger/route(two-layer casing+active)/alternatives/unavoided/traffic and the camera
 *    (follow with bearing+tilt, center, zoomBy, overview) match.
 *  - event markers and the current-location marker are drawn as coloured circles rather than
 *    Google's teardrop pins (same colour coding), which keeps the renderer dependency-light.
 */
@Composable
internal fun MapLibreDkMap(
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
    currentLocation: LatLng?,
    routeIsClean: Boolean,
    interactive: Boolean,
    colors: DkColors,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialise MapLibre before the first MapView is created. getInstance is idempotent.
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    var style by remember { mutableStateOf<Style?>(null) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    // Drive the MapView through the Android lifecycle. Adding the observer while the host is
    // already STARTED/RESUMED replays those events, so onStart/onResume arrive. onDispose covers
    // a removal while the host is still resumed (e.g. a provider switch), which the lifecycle
    // itself would not signal.
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { mapView.onStop() }
            runCatching { mapView.onDestroy() }
        }
    }

    // Load the map and style once, then create the (empty) sources and layers.
    LaunchedEffect(mapView) {
        mapView.getMapAsync { ready ->
            ready.uiSettings.apply {
                isCompassEnabled = false
                isLogoEnabled = false
                isAttributionEnabled = true // OSM/OpenFreeMap attribution is required.
                setAllGesturesEnabled(interactive)
                isRotateGesturesEnabled = interactive
                isScrollGesturesEnabled = interactive
                isZoomGesturesEnabled = interactive
                isTiltGesturesEnabled = interactive
            }
            ready.setStyle(Style.Builder().fromUri(BuildConfig.MAPLIBRE_STYLE_URL)) { loaded ->
                forceHebrewLabels(loaded)
                addOverlayLayers(loaded)
                map = ready
                style = loaded
                controller.attach(MapLibreCameraBackend(ready))

                if (onZoneClick != null) {
                    ready.addOnMapClickListener { point ->
                        val screen: PointF = ready.projection.toScreenLocation(point)
                        val hits = ready.queryRenderedFeatures(screen, LAYER_ZONE_FILL, LAYER_DANGER_FILL)
                        hits.firstOrNull()?.let { onZoneClick(it.toZoneTapInfo()); true } ?: false
                    }
                }
            }
        }
    }

    // Push overlay data to the sources whenever it changes (once the style is ready).
    val readyStyle = style
    LaunchedEffect(
        readyStyle, zones, dangerZones, routePoints, naiveRoutePoints,
        alternativeRoutes, unavoidedRoutePoints, trafficSegments, eventMarkers,
        currentLocation, routeIsClean,
    ) {
        val s = readyStyle ?: return@LaunchedEffect
        updateOverlays(
            style = s,
            colors = colors,
            zones = zones,
            dangerZones = dangerZones,
            routePoints = routePoints,
            naiveRoutePoints = naiveRoutePoints,
            alternativeRoutes = alternativeRoutes,
            unavoidedRoutePoints = unavoidedRoutePoints,
            trafficSegments = trafficSegments,
            eventMarkers = eventMarkers,
            currentLocation = currentLocation,
            routeIsClean = routeIsClean,
        )
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/**
 * OpenMapTiles-based styles default their label text-field to `name` / `name:latin`, which shows
 * Latin names. Force every symbol layer to prefer the Hebrew name, falling back to the default.
 * Requires the style's glyph endpoint to serve Hebrew (OpenFreeMap's Noto glyphs do); if boxes
 * appear, bundle Noto Sans Hebrew glyph PBFs and point the style's `glyphs` URL at them.
 */
private fun forceHebrewLabels(style: Style) {
    val hebrewFirst = Expression.coalesce(
        Expression.get("name:he"),
        Expression.get("name:latin"),
        Expression.get("name"),
    )
    style.layers.filterIsInstance<SymbolLayer>().forEach { layer ->
        runCatching { layer.setProperties(PropertyFactory.textField(hebrewFirst)) }
    }
}

// --- Source and layer ids -------------------------------------------------------------------

private const val SRC_ZONE = "dk-zone-src"
private const val SRC_DANGER = "dk-danger-src"
private const val SRC_NAIVE = "dk-naive-src"
private const val SRC_UNAVOIDED = "dk-unavoided-src"
private const val SRC_ALT = "dk-alt-src"
private const val SRC_ROUTE = "dk-route-src"
private const val SRC_TRAFFIC = "dk-traffic-src"
private const val SRC_EVENTS = "dk-events-src"
private const val SRC_ME = "dk-me-src"

private const val LAYER_ZONE_FILL = "dk-zone-fill"
private const val LAYER_ZONE_LINE = "dk-zone-line"
private const val LAYER_DANGER_FILL = "dk-danger-fill"
private const val LAYER_DANGER_LINE = "dk-danger-line"
private const val LAYER_NAIVE = "dk-naive-line"
private const val LAYER_UNAVOIDED = "dk-unavoided-line"
private const val LAYER_ALT = "dk-alt-line"
private const val LAYER_ROUTE_CASING = "dk-route-casing"
private const val LAYER_ROUTE_ACTIVE = "dk-route-active"
private const val LAYER_TRAFFIC = "dk-traffic-line"
private const val LAYER_EVENTS = "dk-events-circle"
private const val LAYER_ME = "dk-me-circle"

private const val PROP_COLOR = "dk-color"

/**
 * Adds every overlay source (initially empty) and its layer, in the correct stacking order:
 * zones < danger < naive < unavoided < alternatives < route casing < route active < traffic <
 * events < current location. Colours are data-driven from a per-feature [PROP_COLOR] where the
 * palette varies per feature; route/current-location use constant colours set here or in
 * [updateOverlays].
 */
private fun addOverlayLayers(style: Style) {
    // Sources.
    listOf(
        SRC_ZONE, SRC_DANGER, SRC_NAIVE, SRC_UNAVOIDED, SRC_ALT,
        SRC_ROUTE, SRC_TRAFFIC, SRC_EVENTS, SRC_ME,
    ).forEach { id -> style.addSource(GeoJsonSource(id, FeatureCollection.fromFeatures(emptyList()))) }

    val featureColor = Expression.toColor(Expression.get(PROP_COLOR))

    // Zones: translucent fill + opaque outline, coloured per stringency.
    style.addLayer(
        FillLayer(LAYER_ZONE_FILL, SRC_ZONE).withProperties(
            PropertyFactory.fillColor(featureColor),
            PropertyFactory.fillOpacity(ZONE_FILL_ALPHA),
        )
    )
    style.addLayer(
        LineLayer(LAYER_ZONE_LINE, SRC_ZONE).withProperties(
            PropertyFactory.lineColor(featureColor),
            PropertyFactory.lineWidth(ZONE_STROKE_WIDTH),
        )
    )
    // Danger zones (Area A): distinct colour, heavy dashed outline.
    style.addLayer(
        FillLayer(LAYER_DANGER_FILL, SRC_DANGER).withProperties(
            PropertyFactory.fillColor(featureColor),
            PropertyFactory.fillOpacity(ZONE_FILL_ALPHA),
        )
    )
    style.addLayer(
        LineLayer(LAYER_DANGER_LINE, SRC_DANGER).withProperties(
            PropertyFactory.lineColor(featureColor),
            PropertyFactory.lineWidth(ZONE_STROKE_WIDTH + 1f),
            PropertyFactory.lineDasharray(arrayOf(2f, 1.2f)),
        )
    )
    // Naive comparison route.
    style.addLayer(
        LineLayer(LAYER_NAIVE, SRC_NAIVE).withProperties(
            PropertyFactory.lineColor(featureColor),
            PropertyFactory.lineWidth(ROUTE_WIDTH * 0.7f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        )
    )
    // Unavoided route: dashed warning line.
    style.addLayer(
        LineLayer(LAYER_UNAVOIDED, SRC_UNAVOIDED).withProperties(
            PropertyFactory.lineColor(featureColor),
            PropertyFactory.lineWidth(ROUTE_WIDTH * 0.8f),
            PropertyFactory.lineDasharray(arrayOf(2f, 1.4f)),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        )
    )
    // Alternatives.
    style.addLayer(
        LineLayer(LAYER_ALT, SRC_ALT).withProperties(
            PropertyFactory.lineColor(ROUTE_ALT_COLOR.copy(alpha = ALT_ROUTE_ALPHA).toArgb()),
            PropertyFactory.lineWidth(ROUTE_WIDTH),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        )
    )
    // Selected route: casing under active. Colours set in updateOverlays (depend on routeIsClean).
    style.addLayer(
        LineLayer(LAYER_ROUTE_CASING, SRC_ROUTE).withProperties(
            PropertyFactory.lineColor(ROUTE_CASING_COLOR.toArgb()),
            PropertyFactory.lineWidth(ROUTE_CASING_WIDTH),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        )
    )
    style.addLayer(
        LineLayer(LAYER_ROUTE_ACTIVE, SRC_ROUTE).withProperties(
            PropertyFactory.lineColor(ROUTE_ACTIVE_COLOR.toArgb()),
            PropertyFactory.lineWidth(ROUTE_WIDTH),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        )
    )
    // Traffic overlay on the route, coloured per level.
    style.addLayer(
        LineLayer(LAYER_TRAFFIC, SRC_TRAFFIC).withProperties(
            PropertyFactory.lineColor(featureColor),
            PropertyFactory.lineWidth(ROUTE_WIDTH),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        )
    )
    // Event markers (coloured circles).
    style.addLayer(
        CircleLayer(LAYER_EVENTS, SRC_EVENTS).withProperties(
            PropertyFactory.circleColor(featureColor),
            PropertyFactory.circleRadius(7f),
            PropertyFactory.circleStrokeColor(Color.White.toArgb()),
            PropertyFactory.circleStrokeWidth(2f),
        )
    )
    // Current-location marker (azure dot with white ring).
    style.addLayer(
        CircleLayer(LAYER_ME, SRC_ME).withProperties(
            PropertyFactory.circleColor(ME_COLOR.toArgb()),
            PropertyFactory.circleRadius(8f),
            PropertyFactory.circleStrokeColor(Color.White.toArgb()),
            PropertyFactory.circleStrokeWidth(3f),
        )
    )
}

private fun updateOverlays(
    style: Style,
    colors: DkColors,
    zones: List<ZoneFeature>,
    dangerZones: List<DangerZoneFeature>,
    routePoints: List<LatLng>,
    naiveRoutePoints: List<LatLng>,
    alternativeRoutes: List<List<LatLng>>,
    unavoidedRoutePoints: List<LatLng>,
    trafficSegments: List<TrafficSegment>,
    eventMarkers: List<RouteEvent>,
    currentLocation: LatLng?,
    routeIsClean: Boolean,
) {
    // Zones.
    style.setGeoJson(SRC_ZONE, zones.mapNotNull { feature ->
        val ring = feature.geometry.coordinates.firstOrNull().orEmpty()
        if (ring.size < 3) return@mapNotNull null
        polygonFeature(ring, rgba(zoneColor(feature.properties.stringency, colors))).apply {
            addStringProperty("name", feature.properties.nameHe)
            feature.properties.city?.let { addStringProperty("city", it) }
            feature.properties.street?.let { addStringProperty("street", it) }
            feature.properties.description?.let { addStringProperty("description", it) }
            addStringProperty("stringency", feature.properties.stringency.name)
            addBooleanProperty("danger", false)
        }
    })
    // Danger zones.
    style.setGeoJson(SRC_DANGER, dangerZones.mapNotNull { feature ->
        val ring = feature.geometry.coordinates.firstOrNull().orEmpty()
        if (ring.size < 3) return@mapNotNull null
        polygonFeature(ring, rgba(colors.zoneDanger)).apply {
            addStringProperty("name", feature.properties.name)
            feature.properties.city?.let { addStringProperty("city", it) }
            feature.properties.description?.let { addStringProperty("description", it) }
            addBooleanProperty("danger", true)
        }
    })
    // Naive route.
    style.setGeoJson(SRC_NAIVE, lineFeatures(naiveRoutePoints, rgba(colors.routeNaive)))
    // Unavoided route.
    style.setGeoJson(SRC_UNAVOIDED, lineFeatures(unavoidedRoutePoints, rgba(colors.warning)))
    // Alternatives (colour is constant on the layer, so no per-feature colour needed).
    style.setGeoJson(
        SRC_ALT,
        alternativeRoutes.filter { it.size >= 2 }.map { Feature.fromGeometry(lineString(it)) },
    )
    // Selected route: casing + active colours depend on cleanliness.
    style.setGeoJson(SRC_ROUTE, lineFeatures(routePoints, ""))
    val casing = if (routeIsClean) ROUTE_CASING_COLOR else ROUTE_CASING_DANGER
    val active = if (routeIsClean) ROUTE_ACTIVE_COLOR else colors.danger
    style.getLayerAs<LineLayer>(LAYER_ROUTE_CASING)?.setProperties(PropertyFactory.lineColor(casing.toArgb()))
    style.getLayerAs<LineLayer>(LAYER_ROUTE_ACTIVE)?.setProperties(PropertyFactory.lineColor(active.toArgb()))
    // Traffic segments sliced out of the route geometry.
    style.setGeoJson(SRC_TRAFFIC, trafficSegments.mapNotNull { segment ->
        val last = routePoints.lastIndex.coerceAtLeast(0)
        val from = segment.fromIndex.coerceIn(0, last)
        val to = segment.toIndex.coerceIn(from, last)
        val slice = routePoints.slice(from..to)
        if (slice.size < 2) return@mapNotNull null
        Feature.fromGeometry(lineString(slice)).apply {
            addStringProperty(PROP_COLOR, rgba(trafficColor(segment.level, colors)))
        }
    })
    // Event markers.
    style.setGeoJson(SRC_EVENTS, eventMarkers.map { event ->
        Feature.fromGeometry(Point.fromLngLat(event.location.lng, event.location.lat)).apply {
            addStringProperty(PROP_COLOR, rgba(eventColor(event, colors)))
        }
    })
    // Current location.
    style.setGeoJson(SRC_ME, currentLocation?.let {
        listOf(Feature.fromGeometry(Point.fromLngLat(it.lng, it.lat)))
    } ?: emptyList())
}

// --- Small builders -------------------------------------------------------------------------

private fun Style.setGeoJson(sourceId: String, features: List<Feature>) {
    getSourceAs<GeoJsonSource>(sourceId)?.setGeoJson(FeatureCollection.fromFeatures(features))
}

private fun polygonFeature(ring: List<List<Double>>, colorRgba: String): Feature {
    val points = ring.map { Point.fromLngLat(it[0], it[1]) }
    return Feature.fromGeometry(Polygon.fromLngLats(listOf(points))).apply {
        addStringProperty(PROP_COLOR, colorRgba)
    }
}

private fun lineString(points: List<LatLng>): LineString =
    LineString.fromLngLats(points.map { Point.fromLngLat(it.lng, it.lat) })

private fun lineFeatures(points: List<LatLng>, colorRgba: String): List<Feature> {
    if (points.size < 2) return emptyList()
    return listOf(
        Feature.fromGeometry(lineString(points)).apply {
            if (colorRgba.isNotEmpty()) addStringProperty(PROP_COLOR, colorRgba)
        }
    )
}

private fun Feature.toZoneTapInfo(): ZoneTapInfo {
    val danger = runCatching { getBooleanProperty("danger") }.getOrNull() == true
    val stringency = if (hasProperty("stringency")) {
        runCatching { Stringency.valueOf(getStringProperty("stringency")) }.getOrNull()
    } else null
    return ZoneTapInfo(
        name = if (hasProperty("name")) getStringProperty("name") else "",
        city = if (hasProperty("city")) getStringProperty("city") else null,
        street = if (hasProperty("street")) getStringProperty("street") else null,
        description = if (hasProperty("description")) getStringProperty("description") else null,
        stringency = stringency,
        danger = danger,
    )
}

private fun eventColor(event: RouteEvent, colors: DkColors): Color = when (event.type) {
    RouteEventType.ZONE -> colors.danger
    RouteEventType.TRAFFIC -> colors.warning
    RouteEventType.POLICE -> colors.primary
    RouteEventType.ACCIDENT -> colors.warning
    RouteEventType.HAZARD -> colors.zoneMachmirim
    RouteEventType.CLOSURE -> colors.zoneDanger
}

/** MapLibre parses an "rgba(r,g,b,a)" string via Expression.toColor; keeps per-feature alpha. */
private fun rgba(c: Color): String =
    "rgba(${(c.red * 255).roundToInt()},${(c.green * 255).roundToInt()},${(c.blue * 255).roundToInt()},${c.alpha})"

private val ME_COLOR = Color(0xFF2196F3)

package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaye.derechkohanim.data.model.DangerZoneFeature
import com.abaye.derechkohanim.data.model.DangerZoneProperties
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.MapProvider
import com.abaye.derechkohanim.data.model.RouteEvent
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.TrafficLevel
import com.abaye.derechkohanim.data.model.TrafficSegment
import com.abaye.derechkohanim.data.model.ZoneFeature
import com.abaye.derechkohanim.data.model.ZoneProperties
import com.abaye.derechkohanim.service.MapProviderState
import com.abaye.derechkohanim.ui.theme.DkColors
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * Supplies the effective map provider to [DkMap], set once at the app root (MainActivity) from
 * the Hilt [MapProviderState]. Null in a @Preview or any tree that did not provide it, in which
 * case [DkMap] falls back to Google.
 */
val LocalMapProviderState = staticCompositionLocalOf<MapProviderState?> { null }

/**
 * The detail a tapped zone or danger polygon reports, so the surrounding screen can show a
 * card. `stringency` is null for a danger zone (Area A), which is not a halachic layer.
 */
data class ZoneTapInfo(
    val name: String,
    val city: String?,
    val street: String?,
    val description: String?,
    val stringency: Stringency?,
    val danger: Boolean,
)

fun ZoneProperties.toTapInfo() = ZoneTapInfo(
    name = nameHe,
    city = city,
    street = street,
    description = description,
    stringency = stringency,
    danger = false,
)

fun DangerZoneProperties.toTapInfo() = ZoneTapInfo(
    name = name,
    city = city,
    street = null,
    description = description,
    stringency = null,
    danger = true,
)

/** An immutable camera pose, provider-agnostic, so the same intent drives either backend. */
data class CameraSnapshot(
    val target: LatLng,
    val zoom: Float,
    val bearing: Float,
    val tilt: Float,
)

/**
 * The actual map's camera, implemented once per provider (Google maps-compose, MapLibre). The
 * [DkMapController] talks only to this, so it never depends on which renderer is live and a
 * provider switch is transparent.
 */
interface MapCameraBackend {
    /** The camera right now, or null before the map has a valid position. */
    fun snapshot(): CameraSnapshot?

    /** Jump the camera to this pose (instant move, no animation), guarded against early calls. */
    fun move(target: LatLng, zoom: Float, bearing: Float, tilt: Float)

    /** Frame the whole set of points with padding, Waze's route overview. */
    fun overview(points: List<LatLng>, paddingPx: Int)
}

/**
 * Owns the map camera so the surrounding screen can drive it: follow the driver, toggle
 * heading-up, zoom, or pull back to an overview - exactly the same API regardless of provider.
 *
 * The controller is provider-agnostic: it holds the shared UI state (heading-up, auto-zoom) and
 * the last camera intent, and forwards moves to whichever [MapCameraBackend] is currently
 * attached. When the effective provider switches, the new backend attaches and the last intent
 * is replayed, so the map stays put across the swap.
 */
class DkMapController {

    /** Heading-up rotates the map with the drive; north-up keeps north at the top. */
    var headingUp by mutableStateOf(true)

    /**
     * While on, the driving camera picks its own zoom from the current speed instead of sitting
     * at the overview zoom the screen opened with. The zoom buttons turn it off so a deliberate
     * zoom sticks; recentring turns it back on.
     */
    var autoZoom by mutableStateOf(true)
        private set

    private var backend: MapCameraBackend? = null

    /** The last requested pose, replayed when a backend attaches (first bind or provider swap). */
    private var lastCamera: CameraSnapshot? = null

    /** Called by the active renderer when its map is ready. */
    internal fun attach(backend: MapCameraBackend) {
        this.backend = backend
        lastCamera?.let { backend.move(it.target, it.zoom, it.bearing, it.tilt) }
    }

    /** Called by a renderer as it leaves composition, so a stale backend is never driven. */
    internal fun detach(backend: MapCameraBackend) {
        if (this.backend === backend) this.backend = null
    }

    /** Seeds the opening camera once, so the map opens zoomed in on `center` like Waze. */
    internal fun seedIfUnset(location: LatLng, zoom: Float) {
        if (lastCamera == null) moveTo(location, zoom, 0f, 0f)
    }

    fun follow(location: LatLng, bearingDegrees: Float, speedMps: Double = 0.0) {
        val bearing = if (headingUp) bearingDegrees else 0f
        // Auto zoom drives itself off the speed; otherwise keep whatever zoom the driver set.
        val zoom = if (autoZoom) drivingZoom(speedMps) else currentZoom()
        // Always a 3D perspective while driving, regardless of the heading-up toggle.
        moveTo(location, zoom, bearing, FOLLOW_TILT)
    }

    /** Hands the zoom back to the speed-driven camera, e.g. when the driver recentres. */
    fun resumeAutoZoom() {
        autoZoom = true
    }

    fun center(location: LatLng, zoom: Float = FOLLOW_ZOOM) {
        moveTo(location, zoom, 0f, 0f)
    }

    fun zoomBy(delta: Float) {
        // A deliberate zoom wins over the automatic one until the driver recentres.
        autoZoom = false
        val base = backend?.snapshot() ?: lastCamera ?: return
        moveTo(base.target, (base.zoom + delta).coerceIn(MIN_ZOOM, MAX_ZOOM), base.bearing, base.tilt)
    }

    /** Pulls the camera back to frame the whole set of points, Waze's route overview. */
    fun overview(points: List<LatLng>) {
        if (points.isEmpty()) return
        backend?.overview(points, OVERVIEW_PADDING_PX)
    }

    private fun currentZoom(): Float =
        (backend?.snapshot()?.zoom ?: lastCamera?.zoom)?.takeIf { it > 2f } ?: FOLLOW_ZOOM

    private fun moveTo(location: LatLng, zoom: Float, bearing: Float, tilt: Float) {
        lastCamera = CameraSnapshot(location, zoom, bearing, tilt)
        backend?.move(location, zoom, bearing, tilt)
    }

    companion object {
        const val FOLLOW_ZOOM = 17f
        const val FOLLOW_TILT = 45f
        private const val MIN_ZOOM = 5f
        private const val MAX_ZOOM = 20f
        const val OVERVIEW_PADDING_PX = 160

        /**
         * The zoom the driving camera holds at a given speed: close in when crawling through
         * streets, a step wider at motorway speed so the next manoeuvre is still on screen.
         * Quantised to half steps, so a speed that wobbles does not nudge the camera every fix.
         */
        fun drivingZoom(speedMps: Double): Float {
            val span = (FAST_MPS - SLOW_MPS)
            val fraction = ((speedMps - SLOW_MPS) / span).coerceIn(0.0, 1.0)
            val zoom = CLOSE_ZOOM - fraction * (CLOSE_ZOOM - WIDE_ZOOM)
            return (Math.round(zoom * 2.0) / 2.0).toFloat()
        }

        /** Below this (about 14 km/h) the camera is at its closest. */
        private const val SLOW_MPS = 4.0
        /** At this (about 100 km/h) it has pulled back to [WIDE_ZOOM]. */
        private const val FAST_MPS = 28.0
        private const val CLOSE_ZOOM = 18.0
        private const val WIDE_ZOOM = 16.0
    }
}

@Composable
fun rememberDkMapController(): DkMapController = remember { DkMapController() }

/**
 * The one map composable the screens use. It renders with the EFFECTIVE provider - the existing
 * Google (maps-compose) renderer, or the MapLibre / OpenFreeMap renderer used as a NetFree
 * fallback - behind an unchanged public signature and an unchanged [DkMapController] API. The
 * effective provider comes from [LocalMapProviderState]; call sites need no changes.
 */
@Composable
fun DkMap(
    center: LatLng,
    modifier: Modifier = Modifier,
    controller: DkMapController? = null,
    zoom: Float = DEFAULT_ZOOM,
    routePoints: List<LatLng> = emptyList(),
    naiveRoutePoints: List<LatLng> = emptyList(),
    /** Non-selected alternatives, drawn faded beneath the selected route. */
    alternativeRoutes: List<List<LatLng>> = emptyList(),
    /** The unavoided (zone-crossing) route, drawn dashed in the warning colour. */
    unavoidedRoutePoints: List<LatLng> = emptyList(),
    trafficSegments: List<TrafficSegment> = emptyList(),
    eventMarkers: List<RouteEvent> = emptyList(),
    zones: List<ZoneFeature> = emptyList(),
    dangerZones: List<DangerZoneFeature> = emptyList(),
    /** When set, tapping a zone or danger polygon reports its detail for a tap card. */
    onZoneClick: ((ZoneTapInfo) -> Unit)? = null,
    /** When set, tapping the map anywhere that is not a zone reports the point (e.g. pick-start). */
    onMapClick: ((LatLng) -> Unit)? = null,
    currentLocation: LatLng? = null,
    routeIsClean: Boolean = true,
    interactive: Boolean = true,
) {
    val colors = DkTheme.colors

    if (LocalInspectionMode.current) {
        // Neither Play services nor GL run inside a @Preview, so a token-coloured stand-in is
        // drawn for both providers.
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

    val actualController = controller ?: rememberDkMapController()
    val providerState = LocalMapProviderState.current
    val provider = providerState?.effective?.collectAsStateWithLifecycle()?.value ?: MapProvider.GOOGLE

    // Open zoomed in on `center` like Waze; the screen's own follow()/center() take over.
    LaunchedEffect(actualController) { actualController.seedIfUnset(center, zoom) }

    when (provider) {
        MapProvider.OPENFREEMAP -> MapLibreDkMap(
            controller = actualController,
            modifier = modifier,
            routePoints = routePoints,
            naiveRoutePoints = naiveRoutePoints,
            alternativeRoutes = alternativeRoutes,
            unavoidedRoutePoints = unavoidedRoutePoints,
            trafficSegments = trafficSegments,
            eventMarkers = eventMarkers,
            zones = zones,
            dangerZones = dangerZones,
            onZoneClick = onZoneClick,
            onMapClick = onMapClick,
            currentLocation = currentLocation,
            routeIsClean = routeIsClean,
            interactive = interactive,
            colors = colors,
        )
        // AUTOMATIC never reaches DkMap (MapProviderState resolves it); GOOGLE is the default.
        else -> GoogleDkMap(
            controller = actualController,
            modifier = modifier,
            routePoints = routePoints,
            naiveRoutePoints = naiveRoutePoints,
            alternativeRoutes = alternativeRoutes,
            unavoidedRoutePoints = unavoidedRoutePoints,
            trafficSegments = trafficSegments,
            eventMarkers = eventMarkers,
            zones = zones,
            dangerZones = dangerZones,
            onZoneClick = onZoneClick,
            onMapClick = onMapClick,
            currentLocation = currentLocation,
            routeIsClean = routeIsClean,
            interactive = interactive,
            providerState = providerState,
            colors = colors,
        )
    }
}

/** Per-layer zone colour, so the three stringency layers are visually distinct. */
fun zoneColor(stringency: Stringency, colors: DkColors): Color =
    when (stringency) {
        Stringency.BASIC -> colors.zoneBasic
        Stringency.TAHARAT -> colors.zoneTaharat
        Stringency.MACHMIRIM -> colors.zoneMachmirim
    }

internal fun trafficColor(level: TrafficLevel, colors: DkColors): Color =
    when (level) {
        TrafficLevel.FREE -> colors.success
        TrafficLevel.MODERATE -> colors.warning
        TrafficLevel.HEAVY -> colors.danger
    }

internal const val DEFAULT_ZOOM = 14f
/** The bright active route line (Waze "weight 8"). */
internal const val ROUTE_WIDTH = 16f
/** The darker casing drawn under the active line (Waze "weight 12", i.e. ~1.5x). */
internal const val ROUTE_CASING_WIDTH = 24f
internal const val ZONE_FILL_ALPHA = 0.28f
internal const val ZONE_STROKE_WIDTH = 4f
internal const val ALT_ROUTE_ALPHA = 0.9f

// Route layering, low to high: alternatives < casing < active < traffic.
internal const val ROUTE_Z_ALT = 0f
internal const val ROUTE_Z_CASING = 1f
internal const val ROUTE_Z_ACTIVE = 2f
internal const val ROUTE_Z_TRAFFIC = 3f

// Waze-style dimensional route colours (explicit values from the design).
internal val ROUTE_CASING_COLOR = Color(0xFF1565C0)
internal val ROUTE_ACTIVE_COLOR = Color(0xFF2D8CFF)
internal val ROUTE_ALT_COLOR = Color(0xFF9AA5B1)
/** Darker casing under the danger-coloured active line for an unverified route. */
internal val ROUTE_CASING_DANGER = Color(0xFF7F1D1D)

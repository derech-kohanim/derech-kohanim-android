package com.abaye.derechkohanim.data.repository

import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.core.ErrorCode
import com.abaye.derechkohanim.core.map
import com.abaye.derechkohanim.data.model.DangerZoneCollection
import com.abaye.derechkohanim.data.model.NavFeedbackRequest
import com.abaye.derechkohanim.data.model.NavigationProgressRequest
import com.abaye.derechkohanim.data.model.NavigationProgressResponse
import com.abaye.derechkohanim.data.model.NavigationSession
import com.abaye.derechkohanim.data.model.NavigationSessionRequest
import com.abaye.derechkohanim.data.model.ProviderInfo
import com.abaye.derechkohanim.data.model.RoutePlan
import com.abaye.derechkohanim.data.model.RouteRequest
import com.abaye.derechkohanim.data.model.RouteResult
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.ZoneCollection
import com.abaye.derechkohanim.data.remote.DerechKohanimApi
import com.abaye.derechkohanim.data.remote.apiCall
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.io.IOException
import javax.inject.Inject

/**
 * The outcome of a version-aware layer fetch. The layer endpoints answer a request whose
 * `sinceVersion` is already current with HTTP 304 ([Unchanged]); otherwise they return the full,
 * cumulative collection ([Changed]). [Failed] is any transport error, so the caller keeps its cache.
 */
sealed interface LayerFetch<out T> {
    data class Changed<T>(val value: T) : LayerFetch<T>
    data object Unchanged : LayerFetch<Nothing>
    data class Failed(val code: String) : LayerFetch<Nothing>
}

interface RoutingRepository {
    suspend fun computeRoute(request: RouteRequest): DkResult<RoutePlan>
    suspend fun providers(): DkResult<List<ProviderInfo>>
    suspend fun zones(stringency: Stringency, sinceVersion: Int?): DkResult<ZoneCollection>
    suspend fun dangerZones(sinceVersion: Int?): DkResult<DangerZoneCollection>
    /** Version-aware zone fetch for the layer store; distinguishes 304 (unchanged) from a full body. */
    suspend fun zonesSince(stringency: Stringency, sinceVersion: Int?): LayerFetch<ZoneCollection>
    /** Version-aware danger-zone fetch for the layer store; distinguishes 304 from a full body. */
    suspend fun dangerZonesSince(sinceVersion: Int?): LayerFetch<DangerZoneCollection>
    suspend fun createSession(routeId: String, simulated: Boolean): DkResult<NavigationSession>
    suspend fun reportProgress(request: NavigationProgressRequest): DkResult<NavigationProgressResponse>
    suspend fun endSession(sessionId: String): DkResult<NavigationSession>
    suspend fun submitFeedback(request: NavFeedbackRequest): DkResult<Unit>
}

class RemoteRoutingRepository @Inject constructor(
    private val api: DerechKohanimApi,
    private val json: Json,
) : RoutingRepository {

    /**
     * The server now returns a RoutePlan. Until it lands everywhere, we also accept the old
     * single RouteResult shape and wrap it, so the client keeps working through the rollout.
     */
    override suspend fun computeRoute(request: RouteRequest): DkResult<RoutePlan> =
        apiCall { api.computeRoute(request) }.map { element -> element.toRoutePlan(json) }

    override suspend fun providers(): DkResult<List<ProviderInfo>> =
        apiCall { api.providers() }

    override suspend fun zones(stringency: Stringency, sinceVersion: Int?): DkResult<ZoneCollection> =
        apiCall { api.zones(stringency.wire, sinceVersion) }

    override suspend fun dangerZones(sinceVersion: Int?): DkResult<DangerZoneCollection> =
        apiCall { api.dangerZones(sinceVersion) }

    override suspend fun zonesSince(
        stringency: Stringency,
        sinceVersion: Int?,
    ): LayerFetch<ZoneCollection> =
        layerFetch { api.zonesResponse(stringency.wire, sinceVersion) }

    override suspend fun dangerZonesSince(sinceVersion: Int?): LayerFetch<DangerZoneCollection> =
        layerFetch { api.dangerZonesResponse(sinceVersion) }

    /**
     * Runs a Response-returning layer call and folds it into a [LayerFetch]: a 304 is [Unchanged],
     * a 2xx with a body is [Changed], and anything else (non-2xx, empty body, or transport error) is
     * [Failed] so the store keeps its cache. Cancellation is rethrown so a superseded call is dropped.
     */
    private suspend fun <T> layerFetch(
        block: suspend () -> retrofit2.Response<T>,
    ): LayerFetch<T> = try {
        val response = block()
        when {
            response.code() == 304 -> LayerFetch.Unchanged
            response.isSuccessful -> response.body()
                ?.let { LayerFetch.Changed(it) }
                ?: LayerFetch.Failed(ErrorCode.UNKNOWN)
            else -> LayerFetch.Failed(ErrorCode.UNKNOWN)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (io: IOException) {
        LayerFetch.Failed(ErrorCode.NETWORK)
    } catch (other: Throwable) {
        LayerFetch.Failed(ErrorCode.UNKNOWN)
    }

    override suspend fun createSession(routeId: String, simulated: Boolean): DkResult<NavigationSession> =
        apiCall { api.createSession(NavigationSessionRequest(routeId, simulated)) }

    override suspend fun reportProgress(
        request: NavigationProgressRequest,
    ): DkResult<NavigationProgressResponse> =
        apiCall { api.reportProgress(request.sessionId, request) }

    override suspend fun endSession(sessionId: String): DkResult<NavigationSession> =
        apiCall { api.endSession(sessionId) }

    override suspend fun submitFeedback(request: NavFeedbackRequest): DkResult<Unit> =
        apiCall { api.submitFeedback(request) }
}

/**
 * Decodes the compute response tolerantly: a RoutePlan (has `routes`) is used as is; a bare
 * RouteResult (has `geometry`) is wrapped as a single-route plan.
 */
private fun JsonElement.toRoutePlan(json: Json): RoutePlan {
    val obj = this as? JsonObject ?: return RoutePlan()
    return if (obj.containsKey("routes")) {
        json.decodeFromJsonElement(RoutePlan.serializer(), obj)
    } else {
        val single = json.decodeFromJsonElement(RouteResult.serializer(), obj)
        RoutePlan(routes = listOf(single))
    }
}

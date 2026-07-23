package com.abaye.derechkohanim.data.repository

import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.core.map
import com.abaye.derechkohanim.data.model.DangerZoneCollection
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

interface RoutingRepository {
    suspend fun computeRoute(request: RouteRequest): DkResult<RoutePlan>
    suspend fun providers(): DkResult<List<ProviderInfo>>
    suspend fun zones(stringency: Stringency, sinceVersion: Int?): DkResult<ZoneCollection>
    suspend fun dangerZones(sinceVersion: Int?): DkResult<DangerZoneCollection>
    suspend fun createSession(routeId: String, simulated: Boolean): DkResult<NavigationSession>
    suspend fun reportProgress(request: NavigationProgressRequest): DkResult<NavigationProgressResponse>
    suspend fun endSession(sessionId: String): DkResult<NavigationSession>
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

    override suspend fun createSession(routeId: String, simulated: Boolean): DkResult<NavigationSession> =
        apiCall { api.createSession(NavigationSessionRequest(routeId, simulated)) }

    override suspend fun reportProgress(
        request: NavigationProgressRequest,
    ): DkResult<NavigationProgressResponse> =
        apiCall { api.reportProgress(request.sessionId, request) }

    override suspend fun endSession(sessionId: String): DkResult<NavigationSession> =
        apiCall { api.endSession(sessionId) }
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

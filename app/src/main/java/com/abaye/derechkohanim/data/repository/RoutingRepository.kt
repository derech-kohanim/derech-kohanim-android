package com.abaye.derechkohanim.data.repository

import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.data.model.NavigationProgressRequest
import com.abaye.derechkohanim.data.model.NavigationProgressResponse
import com.abaye.derechkohanim.data.model.NavigationSession
import com.abaye.derechkohanim.data.model.NavigationSessionRequest
import com.abaye.derechkohanim.data.model.ProviderInfo
import com.abaye.derechkohanim.data.model.RouteRequest
import com.abaye.derechkohanim.data.model.RouteResult
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.ZoneCollection
import com.abaye.derechkohanim.data.remote.DerechKohanimApi
import com.abaye.derechkohanim.data.remote.apiCall
import javax.inject.Inject

interface RoutingRepository {
    suspend fun computeRoute(request: RouteRequest): DkResult<RouteResult>
    suspend fun providers(): DkResult<List<ProviderInfo>>
    suspend fun zones(stringency: Stringency, sinceVersion: Int?): DkResult<ZoneCollection>
    suspend fun createSession(routeId: String, simulated: Boolean): DkResult<NavigationSession>
    suspend fun reportProgress(request: NavigationProgressRequest): DkResult<NavigationProgressResponse>
    suspend fun endSession(sessionId: String): DkResult<NavigationSession>
}

class RemoteRoutingRepository @Inject constructor(
    private val api: DerechKohanimApi,
) : RoutingRepository {

    override suspend fun computeRoute(request: RouteRequest): DkResult<RouteResult> =
        apiCall { api.computeRoute(request) }

    override suspend fun providers(): DkResult<List<ProviderInfo>> =
        apiCall { api.providers() }

    override suspend fun zones(stringency: Stringency, sinceVersion: Int?): DkResult<ZoneCollection> =
        apiCall { api.zones(stringency.wire, sinceVersion) }

    override suspend fun createSession(routeId: String, simulated: Boolean): DkResult<NavigationSession> =
        apiCall { api.createSession(NavigationSessionRequest(routeId, simulated)) }

    override suspend fun reportProgress(
        request: NavigationProgressRequest,
    ): DkResult<NavigationProgressResponse> =
        apiCall { api.reportProgress(request.sessionId, request) }

    override suspend fun endSession(sessionId: String): DkResult<NavigationSession> =
        apiCall { api.endSession(sessionId) }
}

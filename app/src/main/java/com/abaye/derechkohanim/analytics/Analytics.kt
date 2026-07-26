package com.abaye.derechkohanim.analytics

import com.abaye.derechkohanim.BuildConfig
import com.abaye.derechkohanim.data.local.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Analytics 4 client that speaks the **Measurement Protocol** directly - it POSTs events as
 * JSON to https://www.google-analytics.com/mp/collect over the app's own OkHttp/TLS stack, rather
 * than pulling in Firebase. That stack already trusts the NetFree roots (network_security_config),
 * so analytics keeps working on filtered networks where the Firebase SDK's own transport would fail.
 *
 * Config-driven and inert by default: unless BOTH [BuildConfig.GA_MEASUREMENT_ID] and
 * [BuildConfig.GA_API_SECRET] are set at build time, every call here is a complete no-op and no
 * network request is ever made. Events are fire-and-forget on [Dispatchers.IO] and every failure is
 * swallowed - analytics must never slow down, block or crash the app.
 *
 * Every event carries `app_platform=android`, because the configured measurement id belongs to a
 * web data stream: without it the app's events are indistinguishable from the website's.
 *
 * Privacy: the only identifier sent is [SessionStore.analyticsClientId], an anonymous random UUID.
 * No user id, address, coordinate or other PII is ever attached to an event.
 */
@Singleton
class Analytics @Inject constructor(
    private val sessionStore: SessionStore,
) {
    private val enabled: Boolean =
        BuildConfig.GA_MEASUREMENT_ID.isNotBlank() && BuildConfig.GA_API_SECRET.isNotBlank()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // A plain client on purpose: the DI OkHttpClient carries the AuthInterceptor, which would attach
    // the signed-in user's bearer token to requests bound for Google. This one still honours the
    // app's network_security_config (NetFree roots), so it works on filtered networks all the same.
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val endpoint: String by lazy {
        "https://www.google-analytics.com/mp/collect" +
            "?measurement_id=${BuildConfig.GA_MEASUREMENT_ID}" +
            "&api_secret=${BuildConfig.GA_API_SECRET}"
    }

    /** GA4 `screen_view`. [name] is the logical screen (e.g. "home", "route_review", "navigation"). */
    fun screen(name: String) = track("screen_view", mapOf("screen_name" to name))

    /**
     * Records a GA4 event. [params] values may be String / Boolean / Int / Long / Double; other
     * types are stringified. Returns immediately - the send happens on a background coroutine and
     * any failure is silently ignored. A no-op when analytics is not configured.
     */
    fun track(event: String, params: Map<String, Any?> = emptyMap()) {
        if (!enabled) return
        scope.launch {
            try {
                val clientId = sessionStore.analyticsClientId()
                val payload = buildJsonObject {
                    put("client_id", clientId)
                    putJsonArray("events") {
                        addJsonObject {
                            put("name", event)
                            putJsonObject("params") {
                                // The measurement id belongs to a web data stream, so GA's own
                                // platform dimension reports these events as web. This parameter
                                // is what separates app traffic from the website; it is written
                                // first so an event may still override it deliberately.
                                put(APP_PLATFORM_PARAM, APP_PLATFORM)
                                putParams(params)
                            }
                        }
                    }
                }
                val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
                val request = Request.Builder().url(endpoint).post(body).build()
                client.newCall(request).execute().use { /* response body ignored */ }
            } catch (_: Throwable) {
                // Analytics is best-effort: swallow everything (offline, TLS, cancellation, ...).
            }
        }
    }

    private fun JsonObjectBuilder.putParams(params: Map<String, Any?>) {
        for ((key, value) in params) {
            when (value) {
                null -> {}
                is Boolean -> put(key, value)
                is Int -> put(key, value)
                is Long -> put(key, value)
                is Double -> put(key, value)
                is Float -> put(key, value.toDouble())
                is String -> put(key, value)
                else -> put(key, value.toString())
            }
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()

        /**
         * Not plain "platform": GA4 already has a built-in dimension by that name (set from the
         * data stream type), and a custom parameter would collide with it in the reports.
         * Register this one under Admin > Custom definitions to use it as a report dimension.
         */
        const val APP_PLATFORM_PARAM = "app_platform"
        const val APP_PLATFORM = "android"
    }
}

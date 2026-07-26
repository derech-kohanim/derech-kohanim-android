package com.abaye.derechkohanim.service

import android.content.Context
import android.net.ConnectivityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

/**
 * Decides whether the current network is NetFree - the Israeli filtered-internet service that
 * MITMs HTTPS with its own root CA. Google Maps (Play Services, own pinned trust) rejects that
 * MITM cert, so on NetFree the map tiles usually fail; the MapLibre/OpenFreeMap renderer uses
 * the app's own TLS stack (which trusts the bundled NetFree roots) and works instead.
 *
 * Primary signal: connect to a stable HTTPS host and inspect the presented certificate chain.
 * On NetFree the chain is (re)issued by NetFree's CA, so a subject/issuer DN in the chain
 * contains "netfree". Secondary signal (only when the probe cannot connect at all): a NetFree
 * root present in the *user* trust store.
 *
 * Robust and fail-open: any error, timeout or ambiguity resolves to "not NetFree", so an
 * unknown network never forces the fallback renderer. Runs off the main thread and caches the
 * result per network so repeated calls on the same connection are free.
 */
@Singleton
class NetFreeDetector @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    // Keyed by the active network handle, so a new Wi-Fi/cellular network re-probes.
    private val cache = ConcurrentHashMap<Long, Boolean>()

    suspend fun isNetFree(): Boolean = withContext(Dispatchers.IO) {
        val handle = currentNetworkHandle()
        handle?.let { cache[it] }?.let { return@withContext it }

        val result = detect()
        if (handle != null) cache[handle] = result
        result
    }

    private fun detect(): Boolean {
        val probe = probeCertificateChain()
        return when (probe) {
            // The probe connected: its verdict is authoritative.
            true, false -> probe == true
            // The probe could not connect at all: fall back to the weaker user-store hint.
            null -> userStoreHasNetFreeRoot()
        }
    }

    /**
     * Connects to a stable HTTPS host and returns whether the presented chain looks NetFree, or
     * null when the connection could not be made (so the caller can fall back). The chain is
     * read via [HttpsURLConnection.getServerCertificates], which is exactly what the server sent.
     */
    private fun probeCertificateChain(): Boolean? = runCatching {
        val connection = (URL(PROBE_URL).openConnection() as HttpsURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "HEAD"
            instanceFollowRedirects = false
        }
        try {
            connection.connect()
            val chain = connection.serverCertificates
            chain.any { it is X509Certificate && it.looksNetFree() }
        } finally {
            connection.disconnect()
        }
    }.getOrElse {
        // A handshake that throws is inconclusive rather than "not NetFree".
        null
    }

    /**
     * Secondary signal: a NetFree root the user installed themselves shows up in the Android
     * user CA store. Weaker than the live probe (the root can be installed while the phone is on
     * a normal network), so it is only consulted when the probe could not connect.
     */
    private fun userStoreHasNetFreeRoot(): Boolean = runCatching {
        val store = KeyStore.getInstance("AndroidCAStore").apply { load(null) }
        store.aliases().toList().any { alias ->
            // User-installed anchors carry the "user:" alias prefix.
            if (!alias.startsWith("user:")) return@any false
            (store.getCertificate(alias) as? X509Certificate)?.looksNetFree() == true
        }
    }.getOrDefault(false)

    private fun currentNetworkHandle(): Long? = runCatching {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return null
        manager.activeNetwork?.networkHandle
    }.getOrNull()

    private fun X509Certificate.looksNetFree(): Boolean {
        val subject = subjectX500Principal.name.lowercase()
        val issuer = issuerX500Principal.name.lowercase()
        return NETFREE_MARKER in subject || NETFREE_MARKER in issuer
    }

    private companion object {
        // A stable, globally reachable HTTPS host. On NetFree even this is MITM'd.
        const val PROBE_URL = "https://www.google.com/generate_204"
        const val TIMEOUT_MS = 4000
        const val NETFREE_MARKER = "netfree"
    }
}

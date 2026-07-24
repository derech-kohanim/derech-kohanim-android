package com.abaye.derechkohanim.ui.screens.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Returns a Google ID token for the server to exchange, or null when the user cancels or
 * Google services are unavailable. The token is never trusted on the device.
 *
 * `serverClientId` is the backend OAuth client id and still has to be supplied - see README.
 */
object GoogleSignIn {

    suspend fun requestIdToken(context: Context, serverClientId: String): String? {
        if (serverClientId.isBlank()) return null
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        return try {
            val response = CredentialManager.create(context).getCredential(context, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                null
            }
        } catch (error: GetCredentialCancellationException) {
            // The user dismissed the sheet. Not an error worth surfacing.
            null
        } catch (error: GetCredentialException) {
            // Swallowed to a null result for the UI, but logged so the real cause is visible.
            // A NoCredentialException here after the account picker almost always means the
            // project has no Android OAuth client for this package + signing SHA-1, or the
            // OAuth consent screen is still in testing without this account as a test user.
            Log.w(
                "GoogleSignIn",
                "getCredential failed: ${error.javaClass.simpleName} type=${error.type} " +
                    "msg=${error.message}" +
                    if (error is NoCredentialException) " (no matching credential/OAuth client)" else "",
            )
            null
        }
    }
}

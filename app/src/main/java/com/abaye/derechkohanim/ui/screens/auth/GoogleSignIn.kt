package com.abaye.derechkohanim.ui.screens.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Returns a Google ID token for the server to exchange, or null when the user cancels or
 * Google services are unavailable. The token is never trusted on the device.
 *
 * This is the "Sign in with Google" button flow ([GetSignInWithGoogleOption]): Play Services
 * answers at once with its own account picker, and the user chooses there. The bottom-sheet flow
 * (GetGoogleIdOption) first asks Play Services to look the accounts up, and Android gives that
 * lookup only a few seconds; on a slow or filtered connection (NetFree) it often runs past the
 * limit, and the request then fails as "no credential" even though the account was found.
 *
 * `serverClientId` is the backend (Web) OAuth client id. The app's package and signing
 * certificate must also be registered as an Android OAuth client in the same project.
 */
object GoogleSignIn {

    suspend fun requestIdToken(context: Context, serverClientId: String): String? {
        if (serverClientId.isBlank()) return null
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
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
            // The user dismissed the picker. Not an error worth surfacing.
            null
        } catch (error: GetCredentialException) {
            // Swallowed to a null result for the UI, but logged so the real cause is visible.
            // A NoCredentialException here usually means the project has no Android OAuth client
            // for this package + signing SHA-1.
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

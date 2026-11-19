package com.mindvault.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.mindvault.BuildConfig
import java.security.MessageDigest
import java.util.UUID

sealed interface GoogleTokenOutcome {
    data class Token(val idToken: String, val rawNonce: String) : GoogleTokenOutcome
    data object Cancelled : GoogleTokenOutcome
    data object NoAccount : GoogleTokenOutcome
    data object Failed : GoogleTokenOutcome
}

/**
 * UI-edge helper: asks Android's Credential Manager for a Google ID token. It needs an Activity
 * context, so it lives next to the screen, not in a ViewModel. The token then goes through the
 * ViewModel and use case; no Google types leave this file.
 *
 * A fresh random nonce is used for every request: Google receives its SHA-256, Supabase receives the
 * raw value and checks they match, which blocks replay of a stolen token.
 */
class GoogleIdTokenFetcher {

    suspend fun fetch(activityContext: Context): GoogleTokenOutcome {
        val rawNonce = UUID.randomUUID().toString()
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false) // show every Google account on the device
            .setAutoSelectEnabled(false)
            .setNonce(sha256Hex(rawNonce))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val result = CredentialManager.create(activityContext).getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                GoogleTokenOutcome.Token(idToken = token, rawNonce = rawNonce)
            } else {
                GoogleTokenOutcome.Failed
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleTokenOutcome.Cancelled
        } catch (e: NoCredentialException) {
            GoogleTokenOutcome.NoAccount
        } catch (e: GetCredentialException) {
            GoogleTokenOutcome.Failed
        } catch (e: GoogleIdTokenParsingException) {
            GoogleTokenOutcome.Failed
        }
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
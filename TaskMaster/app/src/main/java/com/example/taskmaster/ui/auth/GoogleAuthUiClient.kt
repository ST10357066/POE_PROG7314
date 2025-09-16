package com.example.taskmaster.ui.auth

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

// A simple data class to hold our sign-in result
data class GoogleSignInResult(
    val data: GoogleSignInData?,
    val errorMessage: String?
)

data class GoogleSignInData(
    val idToken: String,
    val username: String?,
    val profilePictureUrl: String?
)

class GoogleAuthUiClient(
    private val context: Context,
    private val oneTapClient: SignInClient
) {
    // Get your Web Client ID from the google-services.json file
    // Or from the Google Cloud Console for your project.
    private val webClientId = "YOUR_WEB_CLIENT_ID"

    suspend fun signIn(): IntentSender? {
        val result = try {
            oneTapClient.beginSignIn(
                BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setServerClientId(webClientId)
                            .setFilterByAuthorizedAccounts(false) // Show all Google accounts
                            .build()
                    )
                    .setAutoSelectEnabled(true) // Automatically sign in if possible
                    .build()
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            null
        }
        return result?.pendingIntent?.intentSender
    }

    suspend fun signInWithIntent(intent: Intent): GoogleSignInResult {
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        return if (googleIdToken != null) {
            GoogleSignInResult(
                data = GoogleSignInData(
                    idToken = googleIdToken, // <-- RETURN THE TOKEN
                    username = credential.displayName,
                    profilePictureUrl = credential.profilePictureUri?.toString()
                ),
                errorMessage = null
            )
        } else {
            GoogleSignInResult(
                data = null,
                errorMessage = "No Google ID Token found."
            )
        }
    }
}
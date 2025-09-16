package com.example.taskmaster.services


import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Called when a new token for the device is generated.
     * This happens on first app install, when tokens are refreshed, etc.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM Token: $token")
        // When we get a new token, we need to save it to the user's profile
        sendTokenToFirestore(token)
    }

    /**
     * Called when a message is received while the app is in the foreground.
     * We can use this to show an in-app notification if we want.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Message received: ${message.notification?.title}")
        // Handle foreground messages here if needed
    }

    companion object {
        fun sendTokenToFirestore(token: String?) {
            if (token == null) return
            // We only save the token if a user is logged in
            val userId = Firebase.auth.currentUser?.uid ?: return

            // Save the token in a new collection "fcmTokens"
            // A user can have multiple devices, so we use the token as the document ID
            // to avoid duplicates for the same device.
            val tokenInfo = mapOf(
                "userId" to userId,
                "token" to token,
                "timestamp" to System.currentTimeMillis()
            )

            Firebase.firestore.collection("fcmTokens")
                .document(token) // Use token as ID to prevent duplicates
                .set(tokenInfo)
                .addOnSuccessListener { Log.d("FCM", "Token successfully saved to Firestore.") }
                .addOnFailureListener { e -> Log.w("FCM", "Error saving token", e) }
        }
    }
}
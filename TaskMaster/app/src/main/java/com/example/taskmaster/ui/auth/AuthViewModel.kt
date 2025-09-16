package com.example.taskmaster.ui.auth

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow() // The UI will observe this
    // A SharedFlow is better for one-time events like triggering a sign-in UI
    private val _signInIntentSender = MutableSharedFlow<IntentSender>()
    val signInIntentSender = _signInIntentSender.asSharedFlow()

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = getApplication(), // Use the safe application context
            oneTapClient = Identity.getSignInClient(getApplication())
        )
    }

    // --- Event Handlers ---

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun dismissErrorDialog() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // --- Authentication Logic ---

    fun signUpWithEmailPassword() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        if (!isEmailValid(email) || !isPasswordValid(password)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email and password (min. 6 characters).") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _uiState.update {
                            it.copy(isLoading = false, authenticationSuccess = true)
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = task.exception?.message ?: "An unknown error occurred."
                            )
                        }
                    }
                }
        }
    }

    fun signInWithEmailPassword() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        if (!isEmailValid(email) || !isPasswordValid(password)) {
            _uiState.update { it.copy(errorMessage = "Please check your email and password.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _uiState.update {
                            it.copy(isLoading = false, authenticationSuccess = true)
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = task.exception?.message ?: "Authentication failed."
                            )
                        }
                    }
                }
        }
    }

    // --- Add a function to trigger Google Sign In ---
    fun onSignInWithGoogleClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val signInIntentSender = googleAuthUiClient.signIn()
            if (signInIntentSender != null) {
                _signInIntentSender.emit(signInIntentSender)
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Could not start Google Sign-In.") }
            }
        }
    }
    fun firebaseSignInWithGoogle(intent: Intent) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val signInResult = googleAuthUiClient.signInWithIntent(intent)

            if (signInResult.data != null) {
                // CORRECTED: Use the idToken from the result to get the credential
                val credential = GoogleAuthProvider.getCredential(signInResult.data.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            _uiState.update {
                                it.copy(isLoading = false, authenticationSuccess = true)
                            }
                        } else {
                            _uiState.update {
                                it.copy(isLoading = false, errorMessage = task.exception?.message ?: "Firebase Auth failed.")
                            }
                        }
                    }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = signInResult.errorMessage ?: "Google Sign-In failed.") }
            }
        }
    }

    // --- Private Helper Functions ---
    private fun isEmailValid(email: String): Boolean {
        return email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotBlank() && password.length >= 6
    }
}

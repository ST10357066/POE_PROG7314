package com.example.taskmaster.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
data class ProfileUiState(
    val email: String? = null,
    val isSignedOut: Boolean = false
)

class ProfileViewModel : ViewModel() {

    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // When the ViewModel is created, load the current user's email
        loadUser()
    }

    private fun loadUser() {
        _uiState.update { it.copy(email = auth.currentUser?.email) }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            // Set a flag to signal to the UI that sign-out is complete
            _uiState.update { it.copy(isSignedOut = true) }
        }
    }
}
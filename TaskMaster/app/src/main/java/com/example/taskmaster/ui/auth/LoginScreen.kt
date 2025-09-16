package com.example.taskmaster.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskmaster.ui.theme.TaskMasterTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    // Launcher for the Google Sign In activity
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { intent ->
                    // --- CORRECTED: No need to pass context, just the intent ---
                    authViewModel.firebaseSignInWithGoogle(intent)
                }
            } else {
                // User cancelled the flow, so just stop the loading spinner
                authViewModel.dismissErrorDialog()
            }
        }
    )

    // Listen for the signal from the ViewModel to launch the sign-in flow
    LaunchedEffect(Unit) {
        authViewModel.signInIntentSender.collectLatest { intentSender -> // <-- This is now an IntentSender
            // --- CORRECTED: This line now works perfectly ---
            val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
            googleSignInLauncher.launch(intentSenderRequest)
        }
    }
    // Navigate away when authentication is successful
    LaunchedEffect(uiState.authenticationSuccess) {
        if (uiState.authenticationSuccess) {
            onLoginSuccess()
        }
    }

    // Show error dialog if there is an error message
    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = authViewModel::dismissErrorDialog,
            title = { Text("Authentication Error") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = authViewModel::dismissErrorDialog) {
                    Text("OK")
                }
            }
        )
    }

    // Call the "stateless" version, passing the state and event handlers
    AuthScreenContent(
        uiState = uiState,
        onEmailChange = authViewModel::onEmailChange,
        onPasswordChange = authViewModel::onPasswordChange,
        onSignInClick = authViewModel::signInWithEmailPassword,
        onSignUpClick = authViewModel::signUpWithEmailPassword,
        onGoogleSignInClick = {
            authViewModel.onSignInWithGoogleClick()
        }
    )
}

/**
 * The "stateless" version of the screen. It is dumb and only knows how
 * to display the state it's given. This makes it perfectly previewable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreenContent(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome to TaskMaster",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Sign in to continue",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !uiState.isLoading
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !uiState.isLoading
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onSignInClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Sign In")
            }

            TextButton(onClick = onSignUpClick, enabled = !uiState.isLoading) {
                Text("Don't have an account? Sign Up")
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Divider(Modifier.weight(1f))
                Text("OR", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.bodySmall)
                Divider(Modifier.weight(1f))
            }

            Button(
                onClick = onGoogleSignInClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Sign In with Google")
            }

            if (uiState.isLoading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    }
}


// --- PREVIEWS ---

@Preview(showBackground = true, name = "Default State")
@Composable
fun AuthScreenPreview() {
    TaskMasterTheme {
        AuthScreenContent(
            uiState = AuthUiState(), // Default empty state
            onEmailChange = {},
            onPasswordChange = {},
            onSignInClick = {},
            onSignUpClick = {},
            onGoogleSignInClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
fun AuthScreenLoadingPreview() {
    TaskMasterTheme {
        AuthScreenContent(
            uiState = AuthUiState(isLoading = true, email = "user@example.com"), // Loading state
            onEmailChange = {},
            onPasswordChange = {},
            onSignInClick = {},
            onSignUpClick = {},
            onGoogleSignInClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
fun AuthScreenErrorPreview() {
    TaskMasterTheme {
        // To preview the dialog, we'd need a different setup,
        // but we can preview the screen with filled-in data.
        AuthScreenContent(
            uiState = AuthUiState(email = "test@domain.com", password = "password123"),
            onEmailChange = {},
            onPasswordChange = {},
            onSignInClick = {},
            onSignUpClick = {},
            onGoogleSignInClick = {}
        )
    }
}
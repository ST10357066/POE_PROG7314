package com.example.taskmaster.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthenticator(private val context: Context) {

    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val biometricManager = BiometricManager.from(context)

    // This function does NOT need any changes.
    fun isBiometricAuthAvailable(): BiometricAuthStatus {
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAuthStatus.READY
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAuthStatus.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAuthStatus.TEMPORARILY_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAuthStatus.NOT_ENROLLED
            else -> BiometricAuthStatus.NOT_AVAILABLE
        }
    }

    fun promptBiometricAuth(
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onFailed: () -> Unit,
        onError: (Int, CharSequence) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        // --- 2. THIS IS THE ONLY LINE OF CODE THAT CHANGES ---
        val activity = context as FragmentActivity // Context must be a FragmentActivity

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )
        biometricPrompt.authenticate(promptInfo)
    }
}

// This enum does NOT need any changes.
enum class BiometricAuthStatus {
    READY,
    NOT_AVAILABLE,
    TEMPORARILY_UNAVAILABLE,
    NOT_ENROLLED
}
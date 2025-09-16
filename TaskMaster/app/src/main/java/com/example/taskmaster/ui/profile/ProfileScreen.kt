package com.example.taskmaster.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskmaster.ui.theme.TaskMasterTheme
import com.example.taskmaster.utils.SettingsManager
import com.example.taskmaster.utils.LocaleManager
import com.example.taskmaster.R
@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // State for the notification preference
    var notificationsEnabled by remember {
        mutableStateOf(settingsManager.areNotificationsEnabled())
    }

    LaunchedEffect(uiState.isSignedOut) {
        if (uiState.isSignedOut) {
            onSignedOut()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Account Section ---
        SettingsSection(title = stringResource(R.string.account_section)) {
            Text(
                text = uiState.email ?: stringResource(R.string.loading),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // --- Settings Section ---
        SettingsSection(title = stringResource(R.string.settings)) {
            // Notification Setting
            SettingItem(
                title = stringResource(R.string.due_date_reminders),
                description = stringResource(R.string.reminders_description)
            ) {
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { isChecked ->
                        notificationsEnabled = isChecked
                        settingsManager.setNotificationsEnabled(isChecked)
                        // TODO: Tell the backend if we need to register/unregister this device for notifications
                    }
                )
            }

            // Language Setting
//            SettingItem(
//                title = stringResource(R.string.language),
//                description = if (settingsManager.getLanguage() == "zu") "isiZulu" else "English"
//            ) {
//                TextButton(onClick = { showLanguageDialog = true }) {
//                    Text(stringResource(R.string.change))
//                }
//            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- Sign Out Button ---
        Button(
            onClick = { profileViewModel.signOut() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text(text = stringResource(R.string.sign_out))
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { languageCode ->
                LocaleManager.setLocale(context, languageCode)
                showLanguageDialog = false
                // The LocaleManager will handle the activity recreation
            }
        )
    }
}

// --- Reusable Composables for a clean layout ---

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingItem(title: String, description: String, control: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        control()
    }
}

@Composable
fun LanguageSelectionDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_language)) },
        text = {
            Column {
                TextButton(
                    onClick = {
                        onLanguageSelected("en")
                    }
                ) {
                    Text("English")
                }
                TextButton(
                    onClick = {
                        onLanguageSelected("zu")
                    }
                ) {
                    Text("isiZulu")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

// --- Preview ---

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    TaskMasterTheme {
        // We can't easily preview the onSignedOut callback, but we can see the UI
        ProfileScreen(onSignedOut = {})
    }
}
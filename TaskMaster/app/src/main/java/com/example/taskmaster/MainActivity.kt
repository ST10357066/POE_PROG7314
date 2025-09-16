package com.example.taskmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taskmaster.ui.auth.AuthScreen
import com.example.taskmaster.ui.theme.TaskMasterTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.taskmaster.utils.BiometricAuthStatus
import com.example.taskmaster.utils.BiometricAuthenticator
import com.example.taskmaster.utils.SettingsManager
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.example.taskmaster.ui.tasks.TaskListScreen
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.taskmaster.ui.BottomNavItem
import com.example.taskmaster.ui.profile.ProfileScreen
import com.example.taskmaster.ui.tasks.TaskDetailScreen
import com.example.taskmaster.ui.tasks.TaskListScreen
import com.example.taskmaster.ui.theme.TaskMasterTheme
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.taskmaster.services.MyFirebaseMessagingService
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Locale
import android.content.Context
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskmaster.data.TaskRepository
import com.example.taskmaster.data.local.TaskDatabase
import com.example.taskmaster.ui.tasks.TaskDetailViewModel
import com.example.taskmaster.ui.tasks.TaskDetailViewModelFactory
import com.example.taskmaster.ui.tasks.TaskListViewModel
import com.example.taskmaster.ui.tasks.TaskListViewModelFactory

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Get the saved language preference from SharedPreferences
        val settingsManager = SettingsManager(newBase)
        val languageCode = settingsManager.getLanguage()
        val localeToSet = Locale(languageCode)

        // Create a new context with the desired locale
        val newContext = newBase.createConfigurationContext(newBase.resources.configuration)
        newContext.resources.configuration.setLocale(localeToSet)

        // Provide this new, updated context to the Activity
        super.attachBaseContext(newContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()
        setContent {
            // TaskMasterTheme is the custom theme we created.
            // All our UI will be wrapped in this.
            TaskMasterTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // This function sets up our app's navigation
                    AppNavigation()
                }
            }
        }
    }

    // --- ADD THIS LAUNCHER ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. Get the token and save it.
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    MyFirebaseMessagingService.sendTokenToFirestore(task.result)
                }
            }
        } else {
            // Explain to the user that the feature is unavailable
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level 33+ (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "decision_screen" // <-- CHANGE THE START DESTINATION
    ) {
        // Add the new decision screen to the graph
        composable("decision_screen") {
            DecisionScreen(navController = navController)
        }

        // Auth screen remains the same
        composable("auth_screen") {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate("main_screen") {
                        popUpTo("auth_screen") { inclusive = true }
                    }
                }
            )
        }

        // Main screen remains the same
        composable("main_screen") {
            MainScreen(
                // We pass the outer NavController's navigate function down
                // so the inner screen can navigate to the detail screen.
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate("task_detail_screen/$taskId")
                },
                onNavigateToAuth = {
                    // Navigate to auth and clear the ENTIRE back stack
                    navController.navigate("auth_screen") {
                        popUpTo(0) // Pop up to the very start of the graph
                    }
                }
            )
        }

        composable(
            route = "task_detail_screen/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")
            // Ensure taskId is not null before proceeding
            if (taskId != null) {
                // --- CREATE THE VIEWMODEL WITH THE FACTORY HERE ---
                val context = LocalContext.current
                val repository = TaskRepository(TaskDatabase.getDatabase(context).taskDao())
                val detailViewModel: TaskDetailViewModel = viewModel(
                    factory = TaskDetailViewModelFactory(repository, taskId)
                )

                TaskDetailScreen(
                    onNavigateUp = { navController.popBackStack() },
                    taskDetailViewModel = detailViewModel // Pass the newly created ViewModel
                )
            }
        }
    }
}

// A simple placeholder for the screen after a successful login
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onNavigateToTaskDetail: (String) -> Unit,onNavigateToAuth: () -> Unit) {
    val innerNavController = rememberNavController()

    val context = LocalContext.current
    val repository = TaskRepository(TaskDatabase.getDatabase(context).taskDao())
    val taskListViewModel: TaskListViewModel = viewModel(
        factory = TaskListViewModelFactory(repository)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val items = listOf(
                    BottomNavItem.Tasks,
                    BottomNavItem.Profile,
                )

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            innerNavController.navigate(screen.route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNavController, // Use the inner controller
            startDestination = BottomNavItem.Tasks.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Tasks.route) {

                // We now pass BOTH required parameters to the TaskListScreen.
                TaskListScreen(
                    taskListViewModel = taskListViewModel, // Pass the created instance
                    onTaskClick = { task ->
                        onNavigateToTaskDetail(task.id)
                    }
                )

            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    // When ProfileScreen signs out, call the handler
                    onSignedOut = onNavigateToAuth
                )
            }
        }
    }
}

@Composable
fun DecisionScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val biometricAuthenticator = remember { BiometricAuthenticator(context) }

    // This effect runs only once when the screen is first composed
    LaunchedEffect(Unit) {
        val currentUser = Firebase.auth.currentUser
        val isBiometricEnabled = settingsManager.isBiometricEnabled()
        val canAuthenticate = biometricAuthenticator.isBiometricAuthAvailable() == BiometricAuthStatus.READY

        if (currentUser != null && isBiometricEnabled && canAuthenticate) {
            // If user is logged in AND has biometrics enabled, prompt them
            biometricAuthenticator.promptBiometricAuth(
                title = "Unlock TaskMaster",
                subtitle = "Confirm your identity to continue",
                negativeButtonText = "Cancel",
                onSuccess = {
                    // On success, go straight to the main screen
                    navController.navigate("main_screen") {
                        popUpTo("decision_screen") { inclusive = true }
                    }
                },
                onFailed = { /* Fallback to login screen */
                    navController.navigate("auth_screen") {
                        popUpTo("decision_screen") { inclusive = true }
                    }
                },
                onError = { _, _ -> /* User cancelled or other error, fallback to login */
                    navController.navigate("auth_screen") {
                        popUpTo("decision_screen") { inclusive = true }
                    }
                }
            )
        } else if (currentUser != null) {
            // User is logged in but doesn't have biometrics enabled, go to main screen
            navController.navigate("main_screen") {
                popUpTo("decision_screen") { inclusive = true }
            }
        } else {
            // No user is logged in, go to the auth screen
            navController.navigate("auth_screen") {
                popUpTo("decision_screen") { inclusive = true }
            }
        }
    }

    // Show a loading indicator while the decision is being made
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}


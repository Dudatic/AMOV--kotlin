package pt.isec.a2022136610.safetysec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import pt.isec.a2022136610.safetysec.ui.screens.*
import pt.isec.a2022136610.safetysec.ui.theme.SafetYSecTheme
import pt.isec.a2022136610.safetysec.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafetYSecTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()

                // Check if user is already logged in
                val startDestination = if (FirebaseAuth.getInstance().currentUser != null) "home" else "login"

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToRegister = {
                                navController.navigate("register")
                            },
                            viewModel = authViewModel
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = {
                                navController.navigate("login") {
                                    popUpTo("register") { inclusive = true }
                                }
                            },
                            onBackToLogin = {
                                navController.popBackStack()
                            },
                            viewModel = authViewModel
                        )
                    }
                    composable("home") {
                        HomeScreen(
                            navController = navController,
                            viewModel = authViewModel
                        )
                    }

                    // --- Protected Screens ---
                    composable("history") {
                        AlertHistoryScreen(
                            onBack = { navController.popBackStack() },
                            viewModel = authViewModel
                        )
                    }

                    composable("active_rules") {
                        ActiveRulesScreen(
                            onBack = { navController.popBackStack() },
                            viewModel = authViewModel
                        )
                    }

                    // --- Monitor Screens ---
                    composable("map/{userId}") { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                        MapScreen(
                            navController = navController,
                            userId = userId,
                            viewModel = authViewModel
                        )
                    }
                    composable("geofence/{userId}") { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                        GeofenceScreen(
                            navController = navController,
                            userId = userId,
                            viewModel = authViewModel
                        )
                    }
                    composable("rules/{userId}") { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                        RuleManagementScreen(
                            protectedId = userId,
                            onBack = { navController.popBackStack() },
                            viewModel = authViewModel
                        )
                    }
                    composable("alert/{alertId}") { backStackEntry ->
                        val alertId = backStackEntry.arguments?.getString("alertId") ?: return@composable
                        AlertDetailsScreen(
                            navController = navController,
                            alertId = alertId,
                            viewModel = authViewModel
                        )
                    }
                }
            }
        }
    }
}
package pt.isec.a2022136610.safetysec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import pt.isec.a2022136610.safetysec.ui.screens.*
import pt.isec.a2022136610.safetysec.ui.theme.SafetYSecTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startRoute = if (FirebaseAuth.getInstance().currentUser != null) "home" else "login"

        setContent {
            SafetYSecTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = startRoute) {

                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                },
                                onNavigateToRegister = { navController.navigate("register") }
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = { navController.popBackStack() },
                                onBackToLogin = { navController.popBackStack() }
                            )
                        }

                        composable("home") {
                            HomeScreen(navController = navController)
                        }

                        composable(
                            route = "map/{userId}",
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""
                            MapScreen(navController = navController, userId = userId)
                        }

                        composable(
                            route = "geofence/{userId}",
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""
                            GeofenceScreen(navController = navController, userId = userId)
                        }

                        // --- NOVAS ROTAS ---
                        composable(
                            route = "rules/{userId}",
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""
                            RuleManagementScreen(
                                protectedId = userId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("history") {
                            AlertHistoryScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
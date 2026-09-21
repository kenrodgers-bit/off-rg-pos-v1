package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.DarkBg
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PosViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val posViewModel: PosViewModel = viewModel()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    AppNavigation(viewModel = posViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: PosViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                viewModel = viewModel,
                onNavigateToSetup = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Setup.route) {
            SetupScreen(
                viewModel = viewModel,
                onSetupComplete = {
                    navController.navigate(Screen.MainHost.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                },
                onNavigateToRestore = {
                    navController.navigate(Screen.MainHost.route)
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.MainHost.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.MainHost.route) {
            MainScaffold(
                viewModel = viewModel,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

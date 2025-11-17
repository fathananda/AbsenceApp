package com.fathi.absenceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AbsensiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AbsensiApp()
                }
            }
        }
    }
}

@Composable
fun AbsensiApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = false)

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "login"
    ) {
        composable("login") {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }

        composable("register") {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = authViewModel
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToRiwayat = {
                    navController.navigate("riwayat")
                },
                onNavigateToKalender = {
                    navController.navigate("kalender")
                },
                onNavigateToPengajuan = {
                    navController.navigate("pengajuan")
                },
                onNavigateToTunjangan = {
                    navController.navigate("tunjangan")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable("riwayat") {
            RiwayatScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("kalender") {
            KalenderScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // NEW: Pengajuan
        composable("pengajuan") {
            PengajuanScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToForm = {
                    navController.navigate("pengajuan/form")
                }
            )
        }

        composable("pengajuan/form") {
            FormPengajuanScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // NEW: Tunjangan
        composable("tunjangan") {
            TunjanganScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
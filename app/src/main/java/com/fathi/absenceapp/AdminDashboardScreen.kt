package com.fathi.absenceapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToGuru: () -> Unit,
    onNavigateToAbsensi: () -> Unit,
    onNavigateToLaporan: () -> Unit,
    onNavigateToPengajuan: () -> Unit,
    onNavigateToKalender: () -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel()
) {
    val dashboardState by adminViewModel.dashboardState.collectAsState()
    val userName by authViewModel.userName.collectAsState(initial = "Admin")

    LaunchedEffect(Unit) {
        adminViewModel.loadDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dashboard Admin",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = {
                        authViewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Welcome Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Selamat Datang!",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = userName ?: "Admin",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Statistics
            when (dashboardState) {
                is AdminDashboardState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is AdminDashboardState.Success -> {
                    val data = (dashboardState as AdminDashboardState.Success).data

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Total Guru",
                                value = data.totalGuru.toString(),
                                icon = Icons.Default.People,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Absen Hari Ini",
                                value = data.absenHariIni.toString(),
                                icon = Icons.Default.CheckCircle,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Pengajuan Pending",
                                value = data.pengajuanPending.toString(),
                                icon = Icons.AutoMirrored.Filled.Assignment,
                                color = Color(0xFFFF9800),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Telat Bulan Ini",
                                value = data.telatBulanIni.toString(),
                                icon = Icons.Default.Schedule,
                                color = Color(0xFFEF5350),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                is AdminDashboardState.Error -> {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (dashboardState as AdminDashboardState.Error).message,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                else -> {}
            }

            // Menu Grid
            Text(
                text = "Menu Admin",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminMenuCard(
                        title = "Data Guru",
                        subtitle = "Kelola guru",
                        icon = Icons.Default.People,
                        color = Color(0xFF2196F3),
                        onClick = onNavigateToGuru,
                        modifier = Modifier.weight(1f)
                    )
                    AdminMenuCard(
                        title = "Absensi",
                        subtitle = "Lihat semua",
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50),
                        onClick = onNavigateToAbsensi,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminMenuCard(
                        title = "Laporan",
                        subtitle = "Kehadiran",
                        icon = Icons.Default.Assessment,
                        color = Color(0xFF9C27B0),
                        onClick = onNavigateToLaporan,
                        modifier = Modifier.weight(1f)
                    )
                    AdminMenuCard(
                        title = "Pengajuan",
                        subtitle = "Proses",
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        color = Color(0xFFFF9800),
                        onClick = onNavigateToPengajuan,
                        modifier = Modifier.weight(1f)
                    )
                }

                AdminMenuCard(
                    title = "Kalender",
                    subtitle = "Kelola event sekolah",
                    icon = Icons.Default.CalendarMonth,
                    color = Color(0xFFE91E63),
                    onClick = onNavigateToKalender,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun AdminMenuCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = color
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = color
            )
        }
    }
}
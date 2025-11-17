package com.fathi.absenceapp

import android.Manifest
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onNavigateToRiwayat: () -> Unit,
    onNavigateToKalender: () -> Unit,
    onNavigateToPengajuan: () -> Unit,
    onNavigateToTunjangan: () -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    absensiViewModel: AbsensiViewModel = viewModel()
) {
    val context = LocalContext.current
    val userName by absensiViewModel.userName.collectAsState(initial = "")
    val absensiState by absensiViewModel.absensiState.collectAsState()
    val konfigurasiState by absensiViewModel.konfigurasiState.collectAsState()

    var currentLocation by remember { mutableStateOf(LatLng(-6.200000, 106.816666)) }
    var jamMasuk by remember { mutableStateOf(absensiViewModel.getCurrentTime()) }
    var showJamDialog by remember { mutableStateOf(false) }
    var jarakDariKantor by remember { mutableStateOf<Double?>(null) }
    var lokasiValid by remember { mutableStateOf(true) }

    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val kantorLocation = LatLng(konfigurasiState.kantorLatitude, konfigurasiState.kantorLongitude)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 15f)
    }

    // Update lokasi dan hitung jarak
    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        currentLocation = LatLng(location.latitude, location.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLocation, 15f)

                        val jarak = absensiViewModel.hitungJarak(
                            location.latitude, location.longitude,
                            konfigurasiState.kantorLatitude, konfigurasiState.kantorLongitude
                        )
                        jarakDariKantor = jarak
                        lokasiValid = jarak <= konfigurasiState.radiusMaksimal
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    // Reset state setelah sukses
    LaunchedEffect(absensiState) {
        if (absensiState is AbsensiState.Success) {
            kotlinx.coroutines.delay(5000)
            absensiViewModel.cekAbsenHariIni()
        }
    }

    if (showJamDialog) {
        JamMasukDialog(
            jamMasuk = jamMasuk,
            jamSeharusnya = konfigurasiState.jamMasukDefault,
            lokasiValid = lokasiValid,
            jarak = jarakDariKantor,
            radiusMaksimal = konfigurasiState.radiusMaksimal,
            onJamChange = { jamMasuk = it },
            onDismiss = { showJamDialog = false },
            onConfirm = {
                showJamDialog = false
                absensiViewModel.presensi(jamMasuk, currentLocation.latitude, currentLocation.longitude)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Absensi Mahasiswa") },
                actions = {
                    IconButton(onClick = {
                        authViewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Card Info Mahasiswa
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Selamat Datang!",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = userName ?: "Mahasiswa",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Jam Masuk: ${konfigurasiState.jamMasukDefault}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = konfigurasiState.kantorNama,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Card Info Jarak
            if (jarakDariKantor != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (lokasiValid) {
                            Color(0xFFE8F5E9)
                        } else {
                            Color(0xFFFFEBEE)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (lokasiValid) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (lokasiValid) "✓ Lokasi Valid" else "✗ Lokasi Tidak Valid",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (lokasiValid) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Text(
                                text = "Jarak: ${jarakDariKantor!!.toInt()}m dari kantor (maks: ${konfigurasiState.radiusMaksimal.toInt()}m)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (lokasiValid) Color(0xFF388E3C) else Color(0xFFD32F2F)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Google Maps
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(horizontal = 16.dp),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = locationPermissionState.allPermissionsGranted)
            ) {
                if (locationPermissionState.allPermissionsGranted) {
                    val markerState = remember { MarkerState(position = currentLocation) }
                    Marker(state = markerState, title = "Lokasi Anda")
                }

                val kantorMarkerState = remember { MarkerState(position = kantorLocation) }
                Marker(
                    state = kantorMarkerState,
                    title = konfigurasiState.kantorNama,
                    snippet = "Lokasi Kantor"
                )

                Circle(
                    center = kantorLocation,
                    radius = konfigurasiState.radiusMaksimal,
                    strokeColor = Color(0xFF2196F3),
                    strokeWidth = 2f,
                    fillColor = Color(0x220288D1)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Menu Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MenuCard(
                    title = "Kalender",
                    icon = Icons.Default.CalendarMonth,
                    color = Color(0xFF2196F3),
                    onClick = onNavigateToKalender,
                    modifier = Modifier.weight(1f)
                )
                MenuCard(
                    title = "Pengajuan",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    color = Color(0xFFFF9800),
                    onClick = onNavigateToPengajuan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MenuCard(
                    title = "Riwayat",
                    icon = Icons.Default.History,
                    color = Color(0xFF9C27B0),
                    onClick = onNavigateToRiwayat,
                    modifier = Modifier.weight(1f)
                )
                MenuCard(
                    title = "Tunjangan",
                    icon = Icons.Default.Paid,
                    color = Color(0xFF4CAF50),
                    onClick = onNavigateToTunjangan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Lokasi
            Text(
                text = "Lat: ${String.format("%.6f", currentLocation.latitude)}, " +
                        "Lng: ${String.format("%.6f", currentLocation.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // BAGIAN TOMBOL ABSEN - YANG DIPERBAIKI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!locationPermissionState.allPermissionsGranted) {
                    Text(
                        text = "Izinkan akses lokasi untuk melakukan absen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { locationPermissionState.launchMultiplePermissionRequest() }) {
                        Text("Minta Izin Lokasi")
                    }
                } else {
                    when (absensiState) {
                        is AbsensiState.Loading -> {
                            CircularProgressIndicator()
                        }

                        // PERBAIKAN: Tampilkan card info tanpa tombol
                        is AbsensiState.AlreadyAbsen -> {
                            val data = (absensiState as AbsensiState.AlreadyAbsen).data
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "✓ Anda sudah melakukan presensi hari ini",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))

                                    InfoRow("Jam Seharusnya", data.jamSeharusnya ?: "-")
                                    InfoRow("Jam Masuk", data.jamMasukAktual ?: "-")
                                    InfoRow("Status", data.kategoriKeterlambatan ?: "-")

                                    if (data.keterlambatanMenit != null && data.keterlambatanMenit > 0) {
                                        InfoRow("Keterlambatan", "${data.keterlambatanMenit} menit")
                                    }
                                }
                            }
                        }

                        is AbsensiState.Success -> {
                            val successState = absensiState as AbsensiState.Success
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = successState.message,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center
                                    )

                                    successState.data?.let { data ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider()
                                        Spacer(modifier = Modifier.height(12.dp))

                                        InfoRow("Jam Seharusnya", data.jamSeharusnya ?: "-")
                                        InfoRow("Jam Masuk", data.jamMasukAktual ?: "-")

                                        if (data.keterlambatanMenit != null && data.keterlambatanMenit > 0) {
                                            InfoRow("Keterlambatan", "${data.keterlambatanMenit} menit")
                                        }
                                    }
                                }
                            }
                            // TIDAK ADA TOMBOL DI SINI JUGA
                        }

                        is AbsensiState.Error -> {
                            Text(
                                text = (absensiState as AbsensiState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    jamMasuk = absensiViewModel.getCurrentTime()
                                    showJamDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = lokasiValid
                            ) {
                                Text("Coba Lagi")
                            }
                        }

                        // HANYA TAMPILKAN TOMBOL SAAT Idle
                        else -> {
                            Button(
                                onClick = {
                                    jamMasuk = absensiViewModel.getCurrentTime()
                                    showJamDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = lokasiValid
                            ) {
                                Text("Presensi Sekarang")
                            }

                            if (!lokasiValid) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Anda harus berada dalam radius ${konfigurasiState.radiusMaksimal.toInt()}m dari kantor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamMasukDialog(
    jamMasuk: String,
    jamSeharusnya: String,
    lokasiValid: Boolean,
    jarak: Double?,
    radiusMaksimal: Double,
    onJamChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var jamInput by remember { mutableStateOf(jamMasuk) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Input Jam Masuk") },
        text = {
            Column {
                Text(
                    text = "Jam masuk seharusnya: $jamSeharusnya",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (jarak != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (lokasiValid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lokasiValid) "✓" else "✗",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (lokasiValid) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Jarak: ${jarak.toInt()}m (maks: ${radiusMaksimal.toInt()}m)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (lokasiValid) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = jamInput,
                    onValueChange = {
                        jamInput = it
                        onJamChange(it)
                    },
                    label = { Text("Jam Masuk (HH:mm)") },
                    placeholder = { Text("08:30") },
                    singleLine = true,
                    enabled = lokasiValid
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Format: HH:mm (contoh: 08:30)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!lokasiValid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Lokasi Anda terlalu jauh dari kantor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = lokasiValid
            ) {
                Text("Presensi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun MenuCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = color
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}
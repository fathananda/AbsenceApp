package com.fathi.absenceapp

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.draw.clip
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
import java.util.Locale


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
    var showLogoutDialog by remember { mutableStateOf(false) }


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
                title = {
                    Text(
                        "Absensi Guru",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = {
                        showLogoutDialog = true
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
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
                                text = userName?.takeIf { it.isNotBlank() } ?: "Guru",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoChip(
                            icon = Icons.Default.AccessTime,
                            label = "Jam Masuk",
                            value = konfigurasiState.jamMasukDefault
                        )
                        InfoChip(
                            icon = Icons.Default.LocationOn,
                            label = "Lokasi",
                            value = konfigurasiState.kantorNama
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = jarakDariKantor != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                jarakDariKantor?.let { jarak ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (lokasiValid) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (lokasiValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (lokasiValid) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lokasiValid) "Lokasi Valid" else "Lokasi Tidak Valid",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lokasiValid) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                                Text(
                                    text = "Jarak: ${jarak.toInt()}m dari kantor",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (lokasiValid) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                                Text(
                                    text = "Maksimal: ${konfigurasiState.radiusMaksimal.toInt()}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (lokasiValid) {
                                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Maps Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(MaterialTheme.shapes.large),
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
                        strokeColor = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2f,
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Menu Grid
            Text(
                text = "Menu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernMenuCard(
                    title = "Kalender",
                    subtitle = "Lihat jadwal",
                    icon = Icons.Default.CalendarMonth,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToKalender,
                    modifier = Modifier.weight(1f)
                )
                ModernMenuCard(
                    title = "Pengajuan",
                    subtitle = "Izin & Sakit",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToPengajuan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernMenuCard(
                    title = "Riwayat",
                    subtitle = "Lihat absensi",
                    icon = Icons.Default.History,
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToRiwayat,
                    modifier = Modifier.weight(1f)
                )
                ModernMenuCard(
                    title = "Tunjangan",
                    subtitle = "Laporan gaji",
                    icon = Icons.Default.Paid,
                    color = Color(0xFF4CAF50),
                    onClick = onNavigateToTunjangan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Attendance Button Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!locationPermissionState.allPermissionsGranted) {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.LocationOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Izin Lokasi Diperlukan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aktifkan akses lokasi untuk melakukan presensi",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { locationPermissionState.launchMultiplePermissionRequest() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Berikan Izin Lokasi")
                            }
                        }
                    }
                } else {
                    when (absensiState) {
                        is AbsensiState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                        }

                        is AbsensiState.AlreadyAbsen -> {
                            val data = (absensiState as AbsensiState.AlreadyAbsen).data
                            AttendanceStatusCard(
                                icon = Icons.Default.CheckCircle,
                                title = "Presensi Berhasil",
                                message = "Anda sudah melakukan presensi hari ini",
                                data = data,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        is AbsensiState.Success -> {
                            val successState = absensiState as AbsensiState.Success
                            AttendanceStatusCard(
                                icon = Icons.Default.CheckCircle,
                                title = "Berhasil!",
                                message = successState.message,
                                data = successState.data,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        is AbsensiState.Error -> {
                            ElevatedCard(
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
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = (absensiState as AbsensiState.Error).message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Medium
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
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Coba Lagi")
                                    }
                                }
                            }
                        }

                        else -> {
                            FilledTonalButton(
                                onClick = {
                                    jamMasuk = absensiViewModel.getCurrentTime()
                                    showJamDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp),
                                enabled = lokasiValid,
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(
                                    Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Presensi Sekarang",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!lokasiValid) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "⚠️ Anda harus berada dalam radius ${konfigurasiState.radiusMaksimal.toInt()}m dari kantor",
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

            // Coordinates
            Text(
                text = "Koordinat: ${String.format(Locale.US, "%.6f, %.6f", currentLocation.latitude, currentLocation.longitude)}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Konfirmasi Logout") },
            text = { Text("Yakin ingin logout dari aplikasi?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    authViewModel.logout()
                    onLogout()
                }) {
                    Text("Ya")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Tidak")
                }
            }
        )
    }

}

@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun AttendanceStatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    data: AbsensiData?,
    containerColor: Color,
    contentColor: Color
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                textAlign = TextAlign.Center
            )

            data?.let {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = contentColor.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow("Jam Seharusnya", it.jamSeharusnya ?: "-", contentColor)
                    InfoRow("Jam Masuk", it.jamMasukAktual ?: "-", contentColor)
                    it.kategoriKeterlambatan?.let { kategori ->
                        InfoRow("Status", kategori, contentColor)
                    }
                    if (it.keterlambatanMenit != null && it.keterlambatanMenit > 0) {
                        InfoRow("Keterlambatan", "${it.keterlambatanMenit} menit", contentColor)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = color.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
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
        title = {
            Text(
                "Konfirmasi Presensi",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Jam Seharusnya",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = jamSeharusnya,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                jarak?.let {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (lokasiValid) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (lokasiValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (lokasiValid) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (lokasiValid) "Lokasi Valid" else "Lokasi Tidak Valid",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lokasiValid) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                                Text(
                                    text = "Jarak: ${it.toInt()}m (maks: ${radiusMaksimal.toInt()}m)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (lokasiValid) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = jamInput,
                    onValueChange = {
                        jamInput = it
                        onJamChange(it)
                    },
                    label = { Text("Jam Masuk") },
                    placeholder = { Text("HH:mm") },
                    leadingIcon = {
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                    },
                    singleLine = true,
                    enabled = lokasiValid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    supportingText = {
                        Text("Format: HH:mm (contoh: 08:30)")
                    }
                )

                if (!lokasiValid) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lokasi Anda terlalu jauh dari kantor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onConfirm,
                enabled = lokasiValid
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Presensi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
fun ModernMenuCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.height(120.dp),
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
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
                        .padding(10.dp)
                        .size(24.dp)
                )
            }
            Column {
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
        }
    }
}
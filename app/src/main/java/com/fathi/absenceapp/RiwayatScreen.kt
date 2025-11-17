package com.fathi.absenceapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(
    onNavigateBack: () -> Unit,
    viewModel: AbsensiViewModel = viewModel()
) {
    val riwayatState by viewModel.riwayatState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getRiwayat()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Absensi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                riwayatState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                riwayatState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = riwayatState.error ?: "Error",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.getRiwayat() }) {
                            Text("Coba Lagi")
                        }
                    }
                }
                riwayatState.data.isEmpty() -> {
                    Text(
                        text = "Belum ada riwayat absensi",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(riwayatState.data) { absensi ->
                            RiwayatItemEnhanced(absensi)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RiwayatItemEnhanced(absensi: AbsensiData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (absensi.kategoriKeterlambatan) {
                "Tepat Waktu" -> MaterialTheme.colorScheme.primaryContainer
                "Telat Ringan" -> Color(0xFFFFF3CD)
                "Telat Sedang" -> Color(0xFFFFE0B2)
                "Telat Berat" -> Color(0xFFFFCDD2)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Tanggal & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatDate(absensi.tanggal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatTime(absensi.tanggal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badge Status
                    absensi.kategoriKeterlambatan?.let { kategori ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when (kategori) {
                                "Tepat Waktu" -> Color(0xFF4CAF50)
                                "Telat Ringan" -> Color(0xFFFFC107)
                                "Telat Sedang" -> Color(0xFFFF9800)
                                "Telat Berat" -> Color(0xFFF44336)
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        ) {
                            Text(
                                text = kategori,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // small space between left column and details
                Spacer(modifier = Modifier.width(12.dp))

                // Vertical divider
                HorizontalDivider(
                    modifier = Modifier
                        .height(80.dp)
                        .width(1.dp),
                    thickness = DividerDefaults.Thickness, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Detail column on the right
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    absensi.jamSeharusnya?.let { jamSeharusnya ->
                        DetailRow("Jam Seharusnya", jamSeharusnya)
                    }

                    absensi.jamMasukAktual?.let { jamAktual ->
                        DetailRow("Jam Masuk", jamAktual)
                    }

                    absensi.keterlambatanMenit?.let { menit ->
                        if (menit > 0) {
                            DetailRow(
                                "Keterlambatan",
                                "$menit menit",
                                valueColor = Color(0xFFD32F2F)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Lokasi
                    val lat = absensi.latitude
                    val lng = absensi.longitude
                    DetailRow(
                        "Lokasi",
                        "${String.format(Locale.getDefault(), "%.6f", lat)}, ${String.format(Locale.getDefault(), "%.6f", lng)}"
                    )

                    // Sanksi (jika ada)
                    absensi.sanksi?.let { sanksi ->
                        if (sanksi.isNotBlank() && sanksi != "Tidak ada sanksi") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                color = Color(0xFFFFEBEE)
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
                                    Column {
                                        Text(
                                            text = "Sanksi",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC62828)
                                        )
                                        Text(
                                            text = sanksi,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFD32F2F)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A simple two-column row for label + value used in the item.
 */
@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

/**
 * Date formatter: expects ISO-like string "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'".
 * If parsing fails, returns the original input.
 */
fun formatDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}

/**
 * Time formatter; returns "HH:mm:ss", or original input on failure.
 */
fun formatTime(dateString: String?): String {
    if (dateString.isNullOrBlank()) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}

package com.fathi.absenceapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

// ViewModel
sealed class TunjanganState {
    object Idle : TunjanganState()
    object Loading : TunjanganState()
    data class Success(val data: TunjanganData) : TunjanganState()
    data class Error(val message: String) : TunjanganState()
}

sealed class RingkasanState {
    object Idle : RingkasanState()
    object Loading : RingkasanState()
    data class Success(val data: List<TunjanganData>) : RingkasanState()
    data class Error(val message: String) : RingkasanState()
}

class TunjanganViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferences = UserPreferences(application)

    private val _tunjanganState = MutableStateFlow<TunjanganState>(TunjanganState.Idle)
    val tunjanganState: StateFlow<TunjanganState> = _tunjanganState

    private val _ringkasanState = MutableStateFlow<RingkasanState>(RingkasanState.Idle)
    val ringkasanState: StateFlow<RingkasanState> = _ringkasanState

    val userId = userPreferences.userId

    fun loadTunjangan(mahasiswaId: Int, bulan: Int? = null, tahun: Int? = null) {
        viewModelScope.launch {
            try {
                _tunjanganState.value = TunjanganState.Loading

                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.getTunjangan(token, mahasiswaId, bulan, tahun)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data
                    if (data != null) {
                        _tunjanganState.value = TunjanganState.Success(data)
                    } else {
                        _tunjanganState.value = TunjanganState.Error("Data tidak ditemukan")
                    }
                } else {
                    _tunjanganState.value = TunjanganState.Error("Gagal mengambil data tunjangan")
                }
            } catch (e: Exception) {
                _tunjanganState.value = TunjanganState.Error("Error: ${e.message}")
            }
        }
    }

    fun loadRingkasan(mahasiswaId: Int) {
        viewModelScope.launch {
            try {
                _ringkasanState.value = RingkasanState.Loading

                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.getRingkasanTunjangan(token, mahasiswaId)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data ?: emptyList()
                    _ringkasanState.value = RingkasanState.Success(data)
                } else {
                    _ringkasanState.value = RingkasanState.Error("Gagal mengambil ringkasan")
                }
            } catch (e: Exception) {
                _ringkasanState.value = RingkasanState.Error("Error: ${e.message}")
            }
        }
    }
}

// Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunjanganScreen(
    onNavigateBack: () -> Unit,
    viewModel: TunjanganViewModel = viewModel()
) {
    val tunjanganState by viewModel.tunjanganState.collectAsState()
    val ringkasanState by viewModel.ringkasanState.collectAsState()
    val userId by viewModel.userId.collectAsState(initial = null)

    var showRingkasan by remember { mutableStateOf(false) }

    val now = Calendar.getInstance()
    var selectedMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH) + 1) }
    var selectedYear by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }

    val namaBulan = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    LaunchedEffect(userId, selectedMonth, selectedYear) {
        userId?.let {
            if (!showRingkasan) {
                viewModel.loadTunjangan(it, selectedMonth, selectedYear)
            }
        }
    }

    LaunchedEffect(showRingkasan) {
        if (showRingkasan) {
            userId?.let {
                viewModel.loadRingkasan(it)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Gaji") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showRingkasan = !showRingkasan }) {
                        Icon(
                            if (showRingkasan) Icons.Default.CalendarMonth else Icons.Default.Assessment,
                            contentDescription = if (showRingkasan) "Detail" else "Ringkasan"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (showRingkasan) {
            // Tampilan Ringkasan 3 Bulan
            when (val state = ringkasanState) {
                is RingkasanState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is RingkasanState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Ringkasan 3 Bulan Terakhir",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(state.data) { data ->
                            RingkasanCard(data)
                        }
                    }
                }
                is RingkasanState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { userId?.let { viewModel.loadRingkasan(it) } }) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
                else -> {}
            }
        } else {
            // Tampilan Detail Bulanan
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Month/Year Selector
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedMonth == 1) {
                                    selectedMonth = 12
                                    selectedYear -= 1
                                } else {
                                    selectedMonth -= 1
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous")
                        }

                        Text(
                            text = "${namaBulan[selectedMonth - 1]} $selectedYear",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                if (selectedMonth == 12) {
                                    selectedMonth = 1
                                    selectedYear += 1
                                } else {
                                    selectedMonth += 1
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next")
                        }
                    }
                }

                when (val state = tunjanganState) {
                    is TunjanganState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is TunjanganState.Success -> {
                        DetailTunjanganContent(state.data)
                    }
                    is TunjanganState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = state.message, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = {
                                    userId?.let {
                                        viewModel.loadTunjangan(it, selectedMonth, selectedYear)
                                    }
                                }) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun DetailTunjanganContent(data: TunjanganData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Gaji Pokok
        SectionCard(
            title = "GAJI POKOK",
            icon = Icons.Default.AccountBalanceWallet,
            color = Color(0xFF2196F3)
        ) {
            Text(
                text = formatRupiah(data.gajiPokok),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Kehadiran
        SectionCard(
            title = "KEHADIRAN",
            subtitle = "dari ${data.kehadiran.totalHariKerja} hari kerja",
            icon = Icons.Default.CalendarMonth,
            color = Color(0xFF4CAF50)
        ) {
            KehadiranGrid(data.kehadiran)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tunjangan
        SectionCard(
            title = "TUNJANGAN",
            icon = Icons.Default.Add,
            color = Color(0xFF4CAF50)
        ) {
            Column {
                Text(
                    text = "Tunjangan Hadir",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = data.tunjangan.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatRupiah(data.tunjangan.tunjanganHadir),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Potongan
        if (data.potongan.total > 0) {
            SectionCard(
                title = "POTONGAN",
                icon = Icons.Default.Remove,
                color = Color(0xFFF44336)
            ) {
                Column {
                    data.potongan.detail.forEach { detail ->
                        Text(
                            text = "• $detail",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatRupiah(data.potongan.total),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Gaji Bersih
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Paid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "GAJI BERSIH",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = formatRupiah(data.gajiBersih),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SectionCard(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = color.copy(alpha = 0.1f)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun KehadiranGrid(kehadiran: KehadiranDetail) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KehadiranRow("Hadir", kehadiran.hadir.toString(), Color(0xFF2196F3))
        KehadiranRow("✓ Tepat Waktu", kehadiran.tepatWaktu.toString(), Color(0xFF4CAF50))
        if (kehadiran.telatRingan > 0) {
            KehadiranRow("⚠ Telat Ringan", kehadiran.telatRingan.toString(), Color(0xFFFF9800))
        }
        if (kehadiran.telatSedang > 0) {
            KehadiranRow("⚠ Telat Sedang", kehadiran.telatSedang.toString(), Color(0xFFFF5722))
        }
        if (kehadiran.telatBerat > 0) {
            KehadiranRow("⚠ Telat Berat", kehadiran.telatBerat.toString(), Color(0xFFF44336))
        }
        if (kehadiran.izin > 0) {
            KehadiranRow("ℹ Izin", kehadiran.izin.toString(), Color(0xFF9C27B0))
        }
        if (kehadiran.sakit > 0) {
            KehadiranRow("ℹ Sakit", kehadiran.sakit.toString(), Color(0xFF9C27B0))
        }
        if (kehadiran.dinas > 0) {
            KehadiranRow("ℹ Dinas", kehadiran.dinas.toString(), Color(0xFF9C27B0))
        }
        if (kehadiran.alpa > 0) {
            KehadiranRow("✗ Alpa", kehadiran.alpa.toString(), Color(0xFFF44336))
        }
    }
}

@Composable
fun KehadiranRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Surface(
            shape = MaterialTheme.shapes.small,
            color = color.copy(alpha = 0.1f)
        ) {
            Text(
                text = "$value hari",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun RingkasanCard(data: TunjanganData) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = data.periode,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${data.kehadiran.hadir}/${data.kehadiran.totalHariKerja} hari hadir",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Gaji Pokok",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatRupiah(data.gajiPokok),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Tunjangan",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "+ ${formatRupiah(data.totalTunjangan)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            if (data.totalPotongan > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Potongan",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF44336)
                        )
                        Text(
                            text = "- ${formatRupiah(data.totalPotongan)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFF44336)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gaji Bersih",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatRupiah(data.gajiBersih),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(amount).replace(",00", "")
}
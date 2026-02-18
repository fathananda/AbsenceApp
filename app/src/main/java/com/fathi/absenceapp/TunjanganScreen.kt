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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
                title = {
                    Text(
                        "Laporan Gaji",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = { showRingkasan = !showRingkasan }
                    ) {
                        Icon(
                            if (showRingkasan) Icons.Default.CalendarMonth else Icons.Default.Assessment,
                            contentDescription = if (showRingkasan) "Detail" else "Ringkasan"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            AnimatedContent(
                targetState = showRingkasan,
                transitionSpec = {
                    fadeIn() + slideInVertically() togetherWith
                            fadeOut() + slideOutVertically()
                },
                label = "view_switch"
            ) { isRingkasan ->
                if (isRingkasan) {
                    RingkasanView(ringkasanState, userId, viewModel)
                } else {
                    DetailView(
                        tunjanganState,
                        selectedMonth,
                        selectedYear,
                        namaBulan,
                        onMonthChange = { m, y ->
                            selectedMonth = m
                            selectedYear = y
                        },
                        userId,
                        viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun RingkasanView(
    state: RingkasanState,
    userId: Int?,
    viewModel: TunjanganViewModel
) {
    when (state) {
        is RingkasanState.Loading -> {
            LoadingState()
        }
        is RingkasanState.Success -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Ringkasan 3 Bulan",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(state.data) { data ->
                    RingkasanCardModern(data)
                }
            }
        }
        is RingkasanState.Error -> {
            ErrorState(state.message) {
                userId?.let { viewModel.loadRingkasan(it) }
            }
        }
        else -> {}
    }
}

@Composable
private fun DetailView(
    state: TunjanganState,
    month: Int,
    year: Int,
    monthNames: List<String>,
    onMonthChange: (Int, Int) -> Unit,
    userId: Int?,
    viewModel: TunjanganViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Month Selector
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val (newMonth, newYear) = if (month == 1) {
                            12 to year - 1
                        } else {
                            month - 1 to year
                        }
                        onMonthChange(newMonth, newYear)
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Bulan Sebelumnya",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthNames[month - 1],
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = {
                        val (newMonth, newYear) = if (month == 12) {
                            1 to year + 1
                        } else {
                            month + 1 to year
                        }
                        onMonthChange(newMonth, newYear)
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Bulan Berikutnya",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        when (state) {
            is TunjanganState.Loading -> {
                LoadingState()
            }
            is TunjanganState.Success -> {
                DetailTunjanganModern(state.data)
            }
            is TunjanganState.Error -> {
                ErrorState(state.message) {
                    userId?.let {
                        viewModel.loadTunjangan(it, month, year)
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "Memuat data...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            FilledTonalButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Coba Lagi")
            }
        }
    }
}

@Composable
fun DetailTunjanganModern(data: TunjanganData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        SectionCardModern(
            title = "Gaji Pokok",
            icon = Icons.Default.AccountBalanceWallet,
            iconColor = Color(0xFF2196F3)
        ) {
            Text(
                text = formatRupiah(data.gajiPokok),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionCardModern(
            title = "Kehadiran",
            subtitle = "${data.kehadiran.hadir} dari ${data.kehadiran.totalHariKerja} hari kerja",
            icon = Icons.Default.CalendarMonth,
            iconColor = Color(0xFF4CAF50)
        ) {
            KehadiranGridModern(data.kehadiran)
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionCardModern("Tunjangan Hadir", data.tunjangan.detail, Icons.Default.Add, Color(0xFF4CAF50)) {
            Text(formatRupiah(data.tunjangan.tunjanganHadir),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.height(8.dp))
            // REVISI: keterangan bahwa izin/sakit/dinas tidak dapat tunjangan
            Surface(modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small) {
                Text(
                    "ⓘ Izin, sakit, dan dinas tidak mendapat tunjangan hadir. " +
                            "Dinas dengan persetujuan admin dihitung sebagai hadir.",
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (data.potongan.total > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionCardModern(
                title = "Potongan",
                icon = Icons.Default.Remove,
                iconColor = Color(0xFFEF5350)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.potongan.detail.forEach { detail ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Circle,
                                null,
                                Modifier.size(6.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatRupiah(data.potongan.total),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF5350)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Paid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = "GAJI BERSIH",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = formatRupiah(data.gajiBersih),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SectionCardModern(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
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
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun KehadiranGridModern(kehadiran: KehadiranDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KehadiranRowModern("Hadir",       kehadiran.hadir,       Color(0xFF2196F3))
        KehadiranRowModern("Tepat Waktu", kehadiran.tepatWaktu,  Color(0xFF4CAF50))
        if (kehadiran.telatRingan > 0)
            KehadiranRowModern("Telat Ringan", kehadiran.telatRingan, Color(0xFFFF9800))
        if (kehadiran.telatSedang > 0)
            KehadiranRowModern("Telat Sedang", kehadiran.telatSedang, Color(0xFFFF5722))
        if (kehadiran.telatBerat > 0)
            KehadiranRowModern("Telat Berat",  kehadiran.telatBerat,  Color(0xFFEF5350))
        if (kehadiran.izin > 0)
            KehadiranRowModern("Izin (disetujui)",  kehadiran.izin,  Color(0xFF9C27B0))
        if (kehadiran.sakit > 0)
            KehadiranRowModern("Sakit (disetujui)", kehadiran.sakit, Color(0xFF2196F3))
        if (kehadiran.dinas > 0)
            KehadiranRowModern("Dinas (disetujui)", kehadiran.dinas, Color(0xFF009688))

        // REVISI: alfa ditampilkan terpisah dengan warna merah
        if (kehadiran.alpa > 0) {
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFEF5350),
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Alfa (no ket.)", style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFEBEE)) {
                    Text("${kehadiran.alpa} hari – potongan ${formatRupiah((kehadiran.alpa * 100_000).toDouble())}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                }
            }
        }
    }
}

@Composable
fun KehadiranRowModern(label: String, value: Int, color: Color) {
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
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = "$value hari",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
fun RingkasanCardModern(data: TunjanganData) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(data.periode, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${data.kehadiran.hadir}/${data.kehadiran.totalHariKerja} hari hadir",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // REVISI: tampilkan alfa jika ada
                    if (data.kehadiran.alpa > 0) {
                        Text("${data.kehadiran.alpa} hari alfa",
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFFD32F2F))
                    }
                }
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CalendarMonth, null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(20.dp))

            SalaryRow("Gaji Pokok", formatRupiah(data.gajiPokok))
            Spacer(Modifier.height(12.dp))
            SalaryRow("Tunjangan", "+ ${formatRupiah(data.totalTunjangan)}", Color(0xFF4CAF50))
            if (data.totalPotongan > 0) {
                Spacer(Modifier.height(12.dp))
                SalaryRow("Potongan", "- ${formatRupiah(data.totalPotongan)}", Color(0xFFEF5350))
            }
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Gaji Bersih", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(formatRupiah(data.gajiBersih), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SalaryRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(amount).replace(",00", "")
}
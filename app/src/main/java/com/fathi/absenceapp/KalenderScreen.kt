package com.fathi.absenceapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class KalenderState {
    object Idle : KalenderState()
    object Loading : KalenderState()
    data class Success(val data: List<KalenderData>) : KalenderState()
    data class Error(val message: String) : KalenderState()
}

class KalenderViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferences = UserPreferences(application)

    private val _kalenderState = MutableStateFlow<KalenderState>(KalenderState.Idle)
    val kalenderState: StateFlow<KalenderState> = _kalenderState

    init {
        loadKalender()
    }

    fun loadKalender(bulan: Int? = null, tahun: Int? = null) {
        viewModelScope.launch {
            try {
                _kalenderState.value = KalenderState.Loading

                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.getKalender(token, bulan, tahun)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data ?: emptyList()
                    _kalenderState.value = KalenderState.Success(data)
                } else {
                    _kalenderState.value = KalenderState.Error("Gagal mengambil kalender")
                }
            } catch (e: Exception) {
                _kalenderState.value = KalenderState.Error("Error: ${e.message}")
            }
        }
    }

    fun tambahEvent(tanggal: String, jenis: String, keterangan: String?) {
        viewModelScope.launch {
            try {
                val token = userPreferences.token.first() ?: ""
                val request = KalenderRequest(tanggal, jenis, keterangan)

                val response = RetrofitClient.apiService.tambahKalender(token, request)

                if (response.isSuccessful) {
                    loadKalender()  // refresh otomatis
                } else {
                    _kalenderState.value = KalenderState.Error("Gagal menambah event")
                }
            } catch (e: Exception) {
                _kalenderState.value = KalenderState.Error("Error: ${e.message}")
            }
        }
    }

    fun hapusEvent(id: Int) {
        viewModelScope.launch {
            try {
                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.hapusKalender(token, id)

                if (response.isSuccessful) {
                    loadKalender() // refresh otomatis
                } else {
                    _kalenderState.value = KalenderState.Error("Gagal menghapus event")
                }
            } catch (e: Exception) {
                _kalenderState.value = KalenderState.Error("Error: ${e.message}")
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KalenderScreen(
    onNavigateBack: () -> Unit,
    viewModel: KalenderViewModel = viewModel()
) {
    val kalenderState by viewModel.kalenderState.collectAsState()
    val now = Calendar.getInstance()
    var selectedMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH) + 1) }
    var selectedYear by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }

    val namaBulan = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Kalender Sekolah",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
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
                            viewModel.loadKalender(selectedMonth, selectedYear)
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            "Bulan sebelumnya",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = namaBulan[selectedMonth - 1],
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = selectedYear.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (selectedMonth == 12) {
                                selectedMonth = 1
                                selectedYear += 1
                            } else {
                                selectedMonth += 1
                            }
                            viewModel.loadKalender(selectedMonth, selectedYear)
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            "Bulan berikutnya",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // List kalender
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = kalenderState) {
                    is KalenderState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp),
                            strokeWidth = 4.dp
                        )
                    }
                    is KalenderState.Error -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    modifier = Modifier.padding(20.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            FilledTonalButton(
                                onClick = { viewModel.loadKalender(selectedMonth, selectedYear) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Coba Lagi")
                            }
                        }
                    }
                    is KalenderState.Success -> {
                        if (state.data.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EventBusy,
                                        contentDescription = null,
                                        modifier = Modifier.padding(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Tidak ada acara",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Belum ada jadwal untuk bulan ini",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.data) { event ->
                                    KalenderItemModern(event)
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
fun KalenderItemModern(event: KalenderData) {
    val (bgColor, iconColor, badgeColor, icon) = when (event.jenis) {
        "libur" -> listOf(
            Color(0xFFFFEBEE),
            Color(0xFFF44336),
            Color(0xFFEF5350),
            Icons.Default.BeachAccess
        )
        "ujian" -> listOf(
            Color(0xFFFFF3E0),
            Color(0xFFFF9800),
            Color(0xFFFFA726),
            Icons.AutoMirrored.Filled.MenuBook
        )
        "kegiatan" -> listOf(
            Color(0xFFE3F2FD),
            Color(0xFF2196F3),
            Color(0xFF42A5F5),
            Icons.Default.Event
        )
        else -> listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondary,
            Icons.Default.CalendarToday
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor as Color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = iconColor as Color,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    icon as androidx.compose.ui.graphics.vector.ImageVector,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTanggal(event.tanggal),
                    style = MaterialTheme.typography.labelLarge,
                    color = iconColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.keterangan ?: event.jenis.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (badgeColor as Color).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = event.jenis.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }
        }
    }
}

fun formatTanggal(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (_: Exception) {
        dateString
    }
}
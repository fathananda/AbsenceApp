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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ViewModel
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
}

// Screen
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
                title = { Text("Kalender Sekolah") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
        ) {
            // Filter bulan/tahun
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
                            viewModel.loadKalender(selectedMonth, selectedYear)
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
                            viewModel.loadKalender(selectedMonth, selectedYear)
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next")
                    }
                }
            }

            // List kalender
            when (val state = kalenderState) {
                is KalenderState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is KalenderState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadKalender(selectedMonth, selectedYear) }) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
                is KalenderState.Success -> {
                    if (state.data.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Tidak ada acara bulan ini",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.data) { event ->
                                KalenderItem(event)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun KalenderItem(event: KalenderData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (event.jenis) {
                "libur" -> Color(0xFFFFEBEE)
                "ujian" -> Color(0xFFFFF3E0)
                "kegiatan" -> Color(0xFFE3F2FD)
                else -> MaterialTheme.colorScheme.surface
            }
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
                color = when (event.jenis) {
                    "libur" -> Color(0xFFF44336)
                    "ujian" -> Color(0xFFFF9800)
                    "kegiatan" -> Color(0xFF2196F3)
                    else -> MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (event.jenis) {
                            "libur" -> Icons.Default.BeachAccess
                            "ujian" -> Icons.AutoMirrored.Filled.MenuBook
                            "kegiatan" -> Icons.Default.Event
                            else -> Icons.Default.CalendarToday
                        },
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTanggal(event.tanggal),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.keterangan ?: event.jenis.capitalize(Locale.ROOT),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (event.jenis) {
                        "libur" -> Color(0x20F44336)
                        "ujian" -> Color(0x20FF9800)
                        "kegiatan" -> Color(0x202196F3)
                        else -> Color(0x20000000)
                    }
                ) {
                    Text(
                        text = event.jenis.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (event.jenis) {
                            "libur" -> Color(0xFFD32F2F)
                            "ujian" -> Color(0xFFF57C00)
                            "kegiatan" -> Color(0xFF1976D2)
                            else -> Color.Gray
                        }
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
    } catch (e: Exception) {
        dateString
    }
}
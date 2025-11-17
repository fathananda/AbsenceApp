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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import java.text.SimpleDateFormat
import java.util.*

// ViewModel
sealed class PengajuanState {
    object Idle : PengajuanState()
    object Loading : PengajuanState()
    data class Success(val data: List<PengajuanData>) : PengajuanState()
    data class Error(val message: String) : PengajuanState()
}

class PengajuanViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferences = UserPreferences(application)

    private val _pengajuanState = MutableStateFlow<PengajuanState>(PengajuanState.Idle)
    val pengajuanState: StateFlow<PengajuanState> = _pengajuanState

    val userId = userPreferences.userId

    fun loadPengajuan(mahasiswaId: Int) {
        viewModelScope.launch {
            try {
                _pengajuanState.value = PengajuanState.Loading

                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.getPengajuan(token, mahasiswaId = mahasiswaId)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data ?: emptyList()
                    _pengajuanState.value = PengajuanState.Success(data)
                } else {
                    _pengajuanState.value = PengajuanState.Error("Gagal mengambil pengajuan")
                }
            } catch (e: Exception) {
                _pengajuanState.value = PengajuanState.Error("Error: ${e.message}")
            }
        }
    }
}

// Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengajuanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToForm: () -> Unit,
    viewModel: PengajuanViewModel = viewModel()
) {
    val pengajuanState by viewModel.pengajuanState.collectAsState()
    val userId by viewModel.userId.collectAsState(initial = null)

    LaunchedEffect(userId) {
        userId?.let {
            viewModel.loadPengajuan(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengajuan Izin/Sakit/Dinas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToForm,
                icon = { Icon(Icons.Default.Add, "Tambah") },
                text = { Text("Ajukan") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = pengajuanState) {
                is PengajuanState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is PengajuanState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { userId?.let { viewModel.loadPengajuan(it) } }) {
                            Text("Coba Lagi")
                        }
                    }
                }
                is PengajuanState.Success -> {
                    if (state.data.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Belum ada pengajuan",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.data) { pengajuan ->
                                PengajuanItem(pengajuan)
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
fun PengajuanItem(pengajuan: PengajuanData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (pengajuan.status) {
                "pending" -> Color(0xFFFFF3CD)
                "disetujui" -> Color(0xFFE8F5E9)
                "ditolak" -> Color(0xFFFFEBEE)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (pengajuan.jenis) {
                                "izin" -> Icons.Default.EventBusy
                                "sakit" -> Icons.Default.LocalHospital
                                "dinas" -> Icons.Default.Work
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pengajuan.jenis.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${formatDate(pengajuan.tanggalMulai)} - ${formatDate(pengajuan.tanggalSelesai)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (pengajuan.status) {
                        "pending" -> Color(0xFFFFB300)
                        "disetujui" -> Color(0xFF4CAF50)
                        "ditolak" -> Color(0xFFF44336)
                        else -> Color.Gray
                    }
                ) {
                    Text(
                        text = pengajuan.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (pengajuan.keterangan != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pengajuan.keterangan,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (pengajuan.alasanDitolak != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color(0x20F44336)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Alasan Ditolak:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pengajuan.alasanDitolak,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}
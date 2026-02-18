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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import java.text.SimpleDateFormat
import java.util.*

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

    fun loadPengajuan(guruId: Int) {
        viewModelScope.launch {
            try {
                _pengajuanState.value = PengajuanState.Loading

                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.getPengajuan(token, guruId = guruId)

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
                title = {
                    Text(
                        "Pengajuan Izin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToForm,
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        "Ajukan Baru",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            when (val state = pengajuanState) {
                is PengajuanState.Loading -> {
                    LoadingState()
                }
                is PengajuanState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { userId?.let { viewModel.loadPengajuan(it) } }
                    )
                }
                is PengajuanState.Success -> {
                    if (state.data.isEmpty()) {
                        EmptyState()
                    } else {
                        PengajuanList(data = state.data)
                    }
                }
                else -> {}
            }
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
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
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
            FilledTonalButton(
                onClick = onRetry,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Coba Lagi")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Text(
                text = "Belum Ada Pengajuan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Ajukan izin, sakit, atau dinas\nmelalui tombol di bawah",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun PengajuanList(data: List<PengajuanData>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(data) { pengajuan ->
            PengajuanItemModern(pengajuan)
        }
    }
}

@Composable
fun PengajuanItemModern(pengajuan: PengajuanData) {

    val statusColor = when (pengajuan.status) {
        "pending" -> Color(0xFFFFA726)
        "disetujui" -> Color(0xFF66BB6A)
        "ditolak" -> Color(0xFFEF5350)
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when (pengajuan.jenis) {
                                    "izin" -> Icons.Default.EventBusy
                                    "sakit" -> Icons.Default.LocalHospital
                                    "dinas" -> Icons.Default.Work
                                    else -> Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = pengajuan.jenis.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase() else it.toString()
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatDateRange(pengajuan.tanggalMulai, pengajuan.tanggalSelesai),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = pengajuan.status.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            if (pengajuan.keterangan != null && pengajuan.keterangan.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pengajuan.keterangan,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (pengajuan.alasanDitolak != null && pengajuan.alasanDitolak.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEF5350).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFEF5350)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Alasan Ditolak",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF5350)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pengajuan.alasanDitolak,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}

fun formatDateRange(startDate: String, endDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM", Locale("id", "ID"))
        val start = inputFormat.parse(startDate)
        val end = inputFormat.parse(endDate)
        "${outputFormat.format(start ?: Date())} - ${outputFormat.format(end ?: Date())}"
    } catch (_: Exception) {
        "$startDate - $endDate"
    }
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (_: Exception) {
        dateString
    }
}
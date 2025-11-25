package com.fathi.absenceapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun AdminPengajuanScreen(
    onNavigateBack: () -> Unit,
    adminViewModel: AdminViewModel = viewModel()
) {
    val pengajuanState by adminViewModel.pengajuanState.collectAsState()
    val actionState by adminViewModel.actionState.collectAsState()

    var selectedPengajuan by remember { mutableStateOf<PengajuanData?>(null) }
    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        adminViewModel.loadPengajuanPending()
    }

    LaunchedEffect(actionState) {
        if (actionState is AdminActionState.Success) {
            kotlinx.coroutines.delay(1500)
            adminViewModel.loadPengajuanPending()
            adminViewModel.resetActionState()
            selectedPengajuan = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pengajuan Pending",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { adminViewModel.loadPengajuanPending() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Action State Display
            when (actionState) {
                is AdminActionState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is AdminActionState.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = (actionState as AdminActionState.Success).message,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
                is AdminActionState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = (actionState as AdminActionState.Error).message,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                else -> {}
            }

            when (pengajuanState) {
                is PengajuanState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is PengajuanState.Success -> {
                    val data = (pengajuanState as PengajuanState.Success).data

                    if (data.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.CheckCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Tidak ada pengajuan pending",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Semua pengajuan sudah diproses",
                                    style = MaterialTheme.typography.bodyMedium,
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
                            items(data) { pengajuan ->
                                AdminPengajuanItemCard(
                                    pengajuan = pengajuan,
                                    onApprove = {
                                        selectedPengajuan = pengajuan
                                        showApproveDialog = true
                                    },
                                    onReject = {
                                        selectedPengajuan = pengajuan
                                        showRejectDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
                is PengajuanState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = (pengajuanState as PengajuanState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { adminViewModel.loadPengajuanPending() }) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        // Approve Dialog
        if (showApproveDialog && selectedPengajuan != null) {
            AlertDialog(
                onDismissRequest = { showApproveDialog = false },
                title = { Text("Setujui Pengajuan?") },
                text = {
                    Column {
                        Text("Anda akan menyetujui pengajuan ${selectedPengajuan!!.jenis} dari:")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedPengajuan!!.nama ?: "N/A",
                            fontWeight = FontWeight.Bold
                        )
                        Text("${selectedPengajuan!!.tanggalMulai} s/d ${selectedPengajuan!!.tanggalSelesai}")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            adminViewModel.prosesPengajuan(
                                selectedPengajuan!!.id,
                                "disetujui"
                            )
                            showApproveDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Setujui")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showApproveDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Reject Dialog
        if (showRejectDialog && selectedPengajuan != null) {
            var alasanDitolak by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showRejectDialog = false },
                title = { Text("Tolak Pengajuan?") },
                text = {
                    Column {
                        Text("Pengajuan ${selectedPengajuan!!.jenis} dari:")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedPengajuan!!.nama ?: "N/A",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = alasanDitolak,
                            onValueChange = { alasanDitolak = it },
                            label = { Text("Alasan Ditolak") },
                            placeholder = { Text("Masukkan alasan penolakan") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            adminViewModel.prosesPengajuan(
                                selectedPengajuan!!.id,
                                "ditolak",
                                alasanDitolak
                            )
                            showRejectDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = alasanDitolak.isNotBlank()
                    ) {
                        Text("Tolak")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRejectDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun AdminPengajuanItemCard(
    pengajuan: PengajuanData,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = when (pengajuan.jenis) {
                            "izin" -> Color(0xFFE3F2FD)
                            "sakit" -> Color(0xFFFFF3E0)
                            "dinas" -> Color(0xFFE8F5E9)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
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
                                tint = when (pengajuan.jenis) {
                                    "izin" -> Color(0xFF2196F3)
                                    "sakit" -> Color(0xFFFF9800)
                                    "dinas" -> Color(0xFF4CAF50)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = pengajuan.nama ?: "N/A",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "NIP: ${pengajuan.nim ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = pengajuan.jenis.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tanggal Mulai",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = pengajuan.tanggalMulai,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Tanggal Selesai",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = pengajuan.tanggalSelesai,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (pengajuan.keterangan != null && pengajuan.keterangan.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
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
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tolak")
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Setujui")
                }
            }
        }
    }
}
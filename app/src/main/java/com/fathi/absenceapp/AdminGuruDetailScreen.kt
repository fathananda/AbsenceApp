package com.fathi.absenceapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun AdminGuruDetailScreen(
    guruId: Int,
    onNavigateBack: () -> Unit,
    adminViewModel: AdminViewModel = viewModel()
) {
    val guruDetailState by adminViewModel.guruDetailState.collectAsState()
    val actionState by adminViewModel.actionState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(guruId) {
        adminViewModel.loadGuruDetail(guruId)
    }

    LaunchedEffect(actionState) {
        when (actionState) {
            is AdminActionState.Success -> {
                kotlinx.coroutines.delay(1500)
                if ((actionState as AdminActionState.Success).message.contains("dihapus")) {
                    onNavigateBack()
                } else {
                    adminViewModel.loadGuruDetail(guruId)
                    adminViewModel.resetActionState()
                }
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Detail Guru",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (guruDetailState) {
                is AdminGuruDetailState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is AdminGuruDetailState.Success -> {
                    val guru = (guruDetailState as AdminGuruDetailState.Success).data

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profile Card
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = guru.nama,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "NIP: ${guru.nim}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = guru.role.uppercase(),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        // Gaji & Tunjangan
                        Text(
                            text = "Informasi Gaji",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DetailRowWithIcon(
                                    icon = Icons.Default.Paid,
                                    label = "Gaji Pokok",
                                    value = formatRupiah(guru.gajiPokok),
                                    iconColor = Color(0xFF4CAF50)
                                )
                                HorizontalDivider()
                                DetailRowWithIcon(
                                    icon = Icons.Default.Add,
                                    label = "Tunjangan Hadir/Hari",
                                    value = formatRupiah(guru.tunjanganHadir),
                                    iconColor = Color(0xFF2196F3)
                                )
                            }
                        }

                        // Potongan
                        Text(
                            text = "Informasi Potongan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DetailRowWithIcon(
                                    icon = Icons.Default.Remove,
                                    label = "Potongan Telat Sedang",
                                    value = formatRupiah(guru.potonganTelatSedang),
                                    iconColor = Color(0xFFFF9800)
                                )
                                HorizontalDivider()
                                DetailRowWithIcon(
                                    icon = Icons.Default.Remove,
                                    label = "Potongan Telat Berat",
                                    value = formatRupiah(guru.potonganTelatBerat),
                                    iconColor = Color(0xFFEF5350)
                                )
                            }
                        }

                        // Action States
                        when (actionState) {
                            is AdminActionState.Loading -> {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            is AdminActionState.Success -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFFE8F5E9),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
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
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
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

                        // Edit Button
                        Button(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = actionState !is AdminActionState.Loading
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Data Guru")
                        }
                    }

                    // Edit Dialog
                    if (showEditDialog) {
                        EditGuruDialog(
                            guru = guru,
                            onDismiss = { showEditDialog = false },
                            onConfirm = { request ->
                                adminViewModel.updateGuru(guruId, request)
                                showEditDialog = false
                            }
                        )
                    }

                    // Delete Dialog
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Hapus Guru?") },
                            text = {
                                Text("Apakah Anda yakin ingin menghapus ${guru.nama}? Data absensi dan pengajuan akan ikut terhapus.")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        adminViewModel.deleteGuru(guruId)
                                        showDeleteDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Hapus")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Batal")
                                }
                            }
                        )
                    }
                }
                is AdminGuruDetailState.Error -> {
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
                                text = (guruDetailState as AdminGuruDetailState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun DetailRowWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGuruDialog(
    guru: GuruData,
    onDismiss: () -> Unit,
    onConfirm: (UpdateGuruRequest) -> Unit
) {
    var nama by remember { mutableStateOf(guru.nama) }
    var nim by remember { mutableStateOf(guru.nim) }
    var gajiPokok by remember { mutableStateOf(guru.gajiPokok.toString()) }
    var tunjanganHadir by remember { mutableStateOf(guru.tunjanganHadir.toString()) }
    var potonganTelatSedang by remember { mutableStateOf(guru.potonganTelatSedang.toString()) }
    var potonganTelatBerat by remember { mutableStateOf(guru.potonganTelatBerat.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Data Guru") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nim,
                    onValueChange = { nim = it },
                    label = { Text("NIP") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gajiPokok,
                    onValueChange = { gajiPokok = it },
                    label = { Text("Gaji Pokok") },
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tunjanganHadir,
                    onValueChange = { tunjanganHadir = it },
                    label = { Text("Tunjangan Hadir/Hari") },
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = potonganTelatSedang,
                    onValueChange = { potonganTelatSedang = it },
                    label = { Text("Potongan Telat Sedang") },
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = potonganTelatBerat,
                    onValueChange = { potonganTelatBerat = it },
                    label = { Text("Potongan Telat Berat") },
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        UpdateGuruRequest(
                            nama = nama,
                            nim = nim,
                            gajiPokok = gajiPokok.toDoubleOrNull() ?: guru.gajiPokok,
                            tunjanganHadir = tunjanganHadir.toDoubleOrNull() ?: guru.tunjanganHadir,
                            potonganTelatSedang = potonganTelatSedang.toDoubleOrNull() ?: guru.potonganTelatSedang,
                            potonganTelatBerat = potonganTelatBerat.toDoubleOrNull() ?: guru.potonganTelatBerat
                        )
                    )
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
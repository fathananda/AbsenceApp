package com.fathi.absenceapp


import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ViewModel
sealed class FormPengajuanState {
    object Idle : FormPengajuanState()
    object Loading : FormPengajuanState()
    data class Success(val message: String) : FormPengajuanState()
    data class Error(val message: String) : FormPengajuanState()
}

class FormPengajuanViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferences = UserPreferences(application)
    private val context = application.applicationContext

    private val _formState = MutableStateFlow<FormPengajuanState>(FormPengajuanState.Idle)
    val formState: StateFlow<FormPengajuanState> = _formState

    val userId = userPreferences.userId

    fun submitPengajuan(
        mahasiswaId: Int,
        jenis: String,
        tanggalMulai: String,
        tanggalSelesai: String,
        keterangan: String,
        fotoBuktiUri: Uri?
    ) {
        viewModelScope.launch {
            try {
                _formState.value = FormPengajuanState.Loading

                val token = userPreferences.token.first() ?: ""

                // Prepare request bodies
                val mahasiswaIdBody = mahasiswaId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val jenisBody = jenis.toRequestBody("text/plain".toMediaTypeOrNull())
                val tanggalMulaiBody = tanggalMulai.toRequestBody("text/plain".toMediaTypeOrNull())
                val tanggalSelesaiBody = tanggalSelesai.toRequestBody("text/plain".toMediaTypeOrNull())
                val keteranganBody = keterangan.toRequestBody("text/plain".toMediaTypeOrNull())

                // Prepare foto bukti
                var fotoPart: MultipartBody.Part? = null
                if (fotoBuktiUri != null) {
                    val file = getFileFromUri(context, fotoBuktiUri)
                    if (file != null) {
                        val mimeType = context.contentResolver.getType(fotoBuktiUri)

                        val requestFile = file.asRequestBody(mimeType?.toMediaTypeOrNull())
                        fotoPart = MultipartBody.Part.createFormData(
                            "foto_bukti",
                            file.name,
                            requestFile
                        )
                    }
                }

                val response = RetrofitClient.apiService.ajukanPengajuan(
                    token = token,
                    mahasiswaId = mahasiswaIdBody,
                    jenis = jenisBody,
                    tanggalMulai = tanggalMulaiBody,
                    tanggalSelesai = tanggalSelesaiBody,
                    keterangan = keteranganBody,
                    fotoBukti = fotoPart
                )

                if (response.isSuccessful) {
                    _formState.value = FormPengajuanState.Success("Pengajuan berhasil dikirim!")
                } else {
                    _formState.value = FormPengajuanState.Error("Gagal mengirim pengajuan")
                }
            } catch (e: Exception) {
                _formState.value = FormPengajuanState.Error("Error: ${e.message}")
            }
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Get file name
            var fileName = "upload_${System.currentTimeMillis()}"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }

            // Create temp file
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resetState() {
        _formState.value = FormPengajuanState.Idle
    }
}

// Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormPengajuanScreen(
    onNavigateBack: () -> Unit,
    viewModel: FormPengajuanViewModel = viewModel()
) {
    LocalContext.current
    val formState by viewModel.formState.collectAsState()
    val userId by viewModel.userId.collectAsState(initial = null)

    var selectedJenis by remember { mutableStateOf("izin") }
    var tanggalMulai by remember { mutableStateOf("") }
    var tanggalSelesai by remember { mutableStateOf("") }
    var keterangan by remember { mutableStateOf("") }
    var fotoBuktiUri by remember { mutableStateOf<Uri?>(null) }

    // Date pickers
    var showDatePickerMulai by remember { mutableStateOf(false) }
    var showDatePickerSelesai by remember { mutableStateOf(false) }

    val datePickerStateMulai = rememberDatePickerState()
    val datePickerStateSelesai = rememberDatePickerState()

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        fotoBuktiUri = uri
    }

    // Handle success
    LaunchedEffect(formState) {
        if (formState is FormPengajuanState.Success) {
            kotlinx.coroutines.delay(2000)
            onNavigateBack()
        }
    }

    // Date picker dialogs
    if (showDatePickerMulai) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerMulai = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerStateMulai.selectedDateMillis?.let { millis ->
                        tanggalMulai = formatDateToString(Date(millis))
                    }
                    showDatePickerMulai = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerMulai = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerStateMulai)
        }
    }

    if (showDatePickerSelesai) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerSelesai = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerStateSelesai.selectedDateMillis?.let { millis ->
                        tanggalSelesai = formatDateToString(Date(millis))
                    }
                    showDatePickerSelesai = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerSelesai = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerStateSelesai)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Form Pengajuan") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Jenis Pengajuan
            Text(
                text = "Jenis Pengajuan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JenisPengajuanChip(
                    label = "Izin",
                    icon = Icons.Default.EventBusy,
                    selected = selectedJenis == "izin",
                    onClick = { selectedJenis = "izin" }
                )
                JenisPengajuanChip(
                    label = "Sakit",
                    icon = Icons.Default.LocalHospital,
                    selected = selectedJenis == "sakit",
                    onClick = { selectedJenis = "sakit" }
                )
                JenisPengajuanChip(
                    label = "Dinas",
                    icon = Icons.Default.Work,
                    selected = selectedJenis == "dinas",
                    onClick = { selectedJenis = "dinas" }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tanggal Mulai
            OutlinedTextField(
                value = tanggalMulai,
                onValueChange = {},
                label = { Text("Tanggal Mulai") },
                placeholder = { Text("Pilih tanggal") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePickerMulai = true }) {
                        Icon(Icons.Default.CalendarToday, "Pilih tanggal")
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tanggal Selesai
            OutlinedTextField(
                value = tanggalSelesai,
                onValueChange = {},
                label = { Text("Tanggal Selesai") },
                placeholder = { Text("Pilih tanggal") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePickerSelesai = true }) {
                        Icon(Icons.Default.CalendarToday, "Pilih tanggal")
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Keterangan
            OutlinedTextField(
                value = keterangan,
                onValueChange = { keterangan = it },
                label = { Text("Keterangan") },
                placeholder = { Text("Alasan pengajuan...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Upload Foto
            Text(
                text = "Foto Bukti (Opsional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (fotoBuktiUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = rememberAsyncImagePainter(fotoBuktiUri),
                            contentDescription = "Foto bukti",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        IconButton(
                            onClick = { fotoBuktiUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Hapus foto",
                                    tint = Color.White,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap untuk pilih foto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Surat dokter, undangan, dll",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Submit Button
            when (formState) {
                is FormPengajuanState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is FormPengajuanState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        )
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (formState as FormPengajuanState.Success).message,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
                is FormPengajuanState.Error -> {
                    Column {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFF44336)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = (formState as FormPengajuanState.Error).message,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                userId?.let {
                                    viewModel.submitPengajuan(
                                        mahasiswaId = it,
                                        jenis = selectedJenis,
                                        tanggalMulai = tanggalMulai,
                                        tanggalSelesai = tanggalSelesai,
                                        keterangan = keterangan,
                                        fotoBuktiUri = fotoBuktiUri
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = tanggalMulai.isNotEmpty() && tanggalSelesai.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kirim Pengajuan")
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = {
                            userId?.let {
                                viewModel.submitPengajuan(
                                    mahasiswaId = it,
                                    jenis = selectedJenis,
                                    tanggalMulai = tanggalMulai,
                                    tanggalSelesai = tanggalSelesai,
                                    keterangan = keterangan,
                                    fotoBuktiUri = fotoBuktiUri
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = tanggalMulai.isNotEmpty() && tanggalSelesai.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kirim Pengajuan")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun JenisPengajuanChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

fun formatDateToString(date: Date): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return format.format(date)
}
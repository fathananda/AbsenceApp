package com.fathi.absenceapp


import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import java.util.UUID

sealed class FormPengajuanState {
    object Idle : FormPengajuanState()
    object Loading : FormPengajuanState()
    data class Success(val message: String) : FormPengajuanState()
    data class Error(val message: String) : FormPengajuanState()
}

class FormPengajuanViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferences = UserPreferences(application)

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

                val mahasiswaIdBody = mahasiswaId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val jenisBody = jenis.toRequestBody("text/plain".toMediaTypeOrNull())
                val tanggalMulaiBody = tanggalMulai.toRequestBody("text/plain".toMediaTypeOrNull())
                val tanggalSelesaiBody = tanggalSelesai.toRequestBody("text/plain".toMediaTypeOrNull())
                val keteranganBody = keterangan.toRequestBody("text/plain".toMediaTypeOrNull())

                var fotoPart: MultipartBody.Part? = null
                if (fotoBuktiUri != null) {
                    val file = getFileFromUri(getApplication(), fotoBuktiUri)
                    if (file != null) {
                        val mimeType = getApplication<Application>().contentResolver.getType(fotoBuktiUri)
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

            val originalFileName = getOriginalFileName(context, uri)

            val sanitizedFileName = sanitizeFileName(originalFileName)

            val uniqueFileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}_$sanitizedFileName"

            val file = File(context.cacheDir, uniqueFileName)
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
    private fun getOriginalFileName(context: Context, uri: Uri): String {
        var fileName = "upload"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }


    private fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(100)
            .ifEmpty { "upload" }
    }
}

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

    var showDatePickerMulai by remember { mutableStateOf(false) }
    var showDatePickerSelesai by remember { mutableStateOf(false) }

    val datePickerStateMulai = rememberDatePickerState()
    val datePickerStateSelesai = rememberDatePickerState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        fotoBuktiUri = uri
    }

    LaunchedEffect(formState) {
        if (formState is FormPengajuanState.Success) {
            kotlinx.coroutines.delay(2000)
            onNavigateBack()
        }
    }

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
                title = {
                    Text(
                        "Form Pengajuan",
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Ajukan Permohonan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Isi form dengan lengkap dan benar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Jenis Pengajuan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    JenisPengajuanChipModern(
                        label = "Izin",
                        icon = Icons.Default.EventBusy,
                        selected = selectedJenis == "izin",
                        onClick = { selectedJenis = "izin" },
                        modifier = Modifier.weight(1f)
                    )
                    JenisPengajuanChipModern(
                        label = "Sakit",
                        icon = Icons.Default.LocalHospital,
                        selected = selectedJenis == "sakit",
                        onClick = { selectedJenis = "sakit" },
                        modifier = Modifier.weight(1f)
                    )
                    JenisPengajuanChipModern(
                        label = "Dinas",
                        icon = Icons.Default.Work,
                        selected = selectedJenis == "dinas",
                        onClick = { selectedJenis = "dinas" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Periode Waktu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = tanggalMulai,
                    onValueChange = {},
                    label = { Text("Tanggal Mulai") },
                    placeholder = { Text("Pilih tanggal mulai") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePickerMulai = true }) {
                            Icon(
                                Icons.Default.CalendarToday,
                                "Pilih tanggal",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = tanggalSelesai,
                    onValueChange = {},
                    label = { Text("Tanggal Selesai") },
                    placeholder = { Text("Pilih tanggal selesai") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePickerSelesai = true }) {
                            Icon(
                                Icons.Default.CalendarToday,
                                "Pilih tanggal",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Keterangan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    label = { Text("Alasan pengajuan") },
                    placeholder = { Text("Jelaskan alasan pengajuan Anda...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // Upload Foto Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Foto Bukti",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Opsional - Lampirkan dokumen pendukung",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AnimatedVisibility(
                    visible = fotoBuktiUri != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = rememberAsyncImagePainter(fotoBuktiUri),
                                contentDescription = "Foto bukti",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.3f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            FilledTonalIconButton(
                                onClick = { fotoBuktiUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(Icons.Default.Close, "Hapus foto")
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = fotoBuktiUri == null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.padding(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tap untuk pilih foto",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Surat dokter, undangan, dll",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (formState) {
                    is FormPengajuanState.Loading -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Mengirim pengajuan...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    is FormPengajuanState.Success -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Berhasil!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        text = (formState as FormPengajuanState.Success).message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF388E3C)
                                    )
                                }
                            }
                        }
                    }
                    is FormPengajuanState.Error -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFEBEE)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = Color(0xFFF44336),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Gagal",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC62828)
                                        )
                                        Text(
                                            text = (formState as FormPengajuanState.Error).message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFD32F2F)
                                        )
                                    }
                                }
                            }

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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = tanggalMulai.isNotEmpty() && tanggalSelesai.isNotEmpty(),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 6.dp
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Kirim Pengajuan",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = tanggalMulai.isNotEmpty() && tanggalSelesai.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 6.dp
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Kirim Pengajuan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun JenisPengajuanChipModern(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(
            2.dp,
            MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatDateToString(date: Date): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return format.format(date)
}
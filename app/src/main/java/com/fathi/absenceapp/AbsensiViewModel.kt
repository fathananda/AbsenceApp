package com.fathi.absenceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

sealed class AbsensiState {
    object Idle : AbsensiState()
    object Loading : AbsensiState()
    data class Success(val message: String, val data: AbsensiData? = null) : AbsensiState()
    data class Error(val message: String) : AbsensiState()
    data class AlreadyAbsen(val data: AbsensiData) : AbsensiState() // NEW
}

data class RiwayatState(
    val isLoading: Boolean = false,
    val data: List<AbsensiData> = emptyList(),
    val error: String? = null
)

data class KonfigurasiState(
    val jamMasukDefault: String = "08:00",
    val kantorLatitude: Double = 37.421998,
    val kantorLongitude: Double = -122.084000,
    val kantorNama: String = "Kantor Pusat",
    val radiusMaksimal: Double = 1000.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AbsensiViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)

    private val _absensiState = MutableStateFlow<AbsensiState>(AbsensiState.Idle)
    val absensiState: StateFlow<AbsensiState> = _absensiState

    private val _riwayatState = MutableStateFlow(RiwayatState())
    val riwayatState: StateFlow<RiwayatState> = _riwayatState

    private val _konfigurasiState = MutableStateFlow(KonfigurasiState())
    val konfigurasiState: StateFlow<KonfigurasiState> = _konfigurasiState

    val userName = userPreferences.userName
    val userId = userPreferences.userId

    init {
        loadKonfigurasi()
        cekAbsenHariIni()
    }

    // NEW: Cek apakah sudah absen hari ini
    fun cekAbsenHariIni() {
        viewModelScope.launch {
            try {
                val token = userPreferences.token.first() ?: ""
                val mahasiswaId = userPreferences.userId.first() ?: 0

                val response = RetrofitClient.apiService.cekAbsenHariIni(token, mahasiswaId)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.sudahAbsen && body.data != null) {
                        _absensiState.value = AbsensiState.AlreadyAbsen(body.data)
                    } else {
                        _absensiState.value = AbsensiState.Idle
                    }
                }
            } catch (e: Exception) {
                // Silent fail, tetap tampilkan tombol absen
                _absensiState.value = AbsensiState.Idle
            }
        }
    }

    // Load konfigurasi jam masuk default
    fun loadKonfigurasi() {
        viewModelScope.launch {
            try {
                _konfigurasiState.value = _konfigurasiState.value.copy(isLoading = true)

                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.getKonfigurasi(token)

                if (response.isSuccessful && response.body() != null) {
                    val config = response.body()!!.data
                    _konfigurasiState.value = KonfigurasiState(
                        jamMasukDefault = config?.jamMasukDefault ?: "08:00",
                        kantorLatitude = config?.kantorLatitude?.toDoubleOrNull() ?: -6.360427,
                        kantorLongitude = config?.kantorLongitude?.toDoubleOrNull() ?: 107.095709,
                        kantorNama = config?.kantorNama ?: "SMK Al-Luthfah",
                        radiusMaksimal = config?.radiusMaksimal?.toDoubleOrNull() ?: 1000.0,
                        isLoading = false
                    )
                } else {
                    _konfigurasiState.value = _konfigurasiState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat konfigurasi"
                    )
                }
            } catch (e: Exception) {
                _konfigurasiState.value = _konfigurasiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    // NEW: Hitung jarak dari kantor (Haversine formula)
    fun hitungJarak(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3 // Radius bumi dalam meter
        val φ1 = Math.toRadians(lat1)
        val φ2 = Math.toRadians(lat2)
        val Δφ = Math.toRadians(lat2 - lat1)
        val Δλ = Math.toRadians(lon2 - lon1)

        val a = sin(Δφ / 2) * sin(Δφ / 2) +
                cos(φ1) * cos(φ2) *
                sin(Δλ / 2) * sin(Δλ / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c // Jarak dalam meter
    }

    // NEW: Validasi lokasi sebelum presensi
    fun validasiLokasi(latitude: Double, longitude: Double): Pair<Boolean, String> {
        val config = _konfigurasiState.value
        val jarak = hitungJarak(
            latitude, longitude,
            config.kantorLatitude, config.kantorLongitude
        )

        val valid = jarak <= config.radiusMaksimal
        val pesan = if (valid) {
            "Lokasi valid (${jarak.toInt()}m dari kantor)"
        } else {
            "Lokasi terlalu jauh! Anda berada ${jarak.toInt()}m dari kantor (maksimal ${config.radiusMaksimal.toInt()}m)"
        }

        return Pair(valid, pesan)
    }

    // Presensi dengan input jam masuk
    fun presensi(jamMasukAktual: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                // Validasi lokasi terlebih dahulu
                val (lokasiValid, pesanLokasi) = validasiLokasi(latitude, longitude)

                if (!lokasiValid) {
                    _absensiState.value = AbsensiState.Error(pesanLokasi)
                    return@launch
                }

                _absensiState.value = AbsensiState.Loading

                val token = userPreferences.token.first() ?: ""
                val mahasiswaId = userPreferences.userId.first() ?: 0

                val response = RetrofitClient.apiService.presensi(
                    token = token,
                    request = PresensiRequest(
                        mahasiswaId = mahasiswaId,
                        jamMasukAktual = jamMasukAktual,
                        latitude = latitude,
                        longitude = longitude
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data
                    val message = buildPresensiMessage(data)
                    _absensiState.value = AbsensiState.Success(
                        message = message,
                        data = data
                    )
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = if (errorBody != null && errorBody.contains("sudah melakukan presensi")) {
                        "Anda sudah melakukan presensi hari ini"
                    } else {
                        response.body()?.message ?: "Presensi gagal"
                    }
                    _absensiState.value = AbsensiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _absensiState.value = AbsensiState.Error("Error: ${e.message}")
            }
        }
    }

    // Build pesan hasil presensi
    private fun buildPresensiMessage(data: AbsensiData?): String {
        if (data == null) return "Presensi berhasil!"

        val sb = StringBuilder("Presensi berhasil!\n\n")

        data.kategoriKeterlambatan?.let { kategori ->
            sb.append("Status: $kategori\n")
        }

        data.keterlambatanMenit?.let { menit ->
            if (menit > 0) {
                sb.append("Keterlambatan: $menit menit\n")
            }
        }

        data.sanksi?.let { sanksi ->
            if (sanksi != "Tidak ada sanksi") {
                sb.append("Sanksi: $sanksi")
            }
        }

        return sb.toString().trim()
    }

    // Mendapatkan jam saat ini dalam format HH:mm:ss
    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getRiwayat() {
        viewModelScope.launch {
            try {
                _riwayatState.value = _riwayatState.value.copy(isLoading = true)

                val token = userPreferences.token.first() ?: ""
                val mahasiswaId = userPreferences.userId.first() ?: 0

                val response = RetrofitClient.apiService.getRiwayat(token, mahasiswaId)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data ?: emptyList()
                    _riwayatState.value = RiwayatState(
                        isLoading = false,
                        data = data,
                        error = null
                    )
                } else {
                    _riwayatState.value = _riwayatState.value.copy(
                        isLoading = false,
                        error = "Gagal mengambil riwayat"
                    )
                }
            } catch (e: Exception) {
                _riwayatState.value = _riwayatState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

}
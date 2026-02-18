package com.fathi.absenceapp

import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
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
    data class AlreadyAbsen(val data: AbsensiData) : AbsensiState()
    data class FakeGpsDetected(val reason: String) : AbsensiState()
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
    val kantorNama: String = "SMK Al-Luthfah",
    val radiusMaksimal: Double = 1000.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class FakeGpsResult(
    val isFake: Boolean,
    val reason: String = "",
    val confidence: Int = 0
)

class AbsensiViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val context = application

    private val _absensiState = MutableStateFlow<AbsensiState>(AbsensiState.Idle)
    val absensiState: StateFlow<AbsensiState> = _absensiState

    private val _riwayatState = MutableStateFlow(RiwayatState())
    val riwayatState: StateFlow<RiwayatState> = _riwayatState

    private val _konfigurasiState = MutableStateFlow(KonfigurasiState())
    val konfigurasiState: StateFlow<KonfigurasiState> = _konfigurasiState

    val userName = userPreferences.userName

    init {
        loadKonfigurasi()
        cekAbsenHariIni()
    }

    fun deteksiFakeGps(location: Location): FakeGpsResult {

        // Lapisan 1: Mock Provider API (Android native check)
        @Suppress("DEPRECATION")
        if (location.isFromMockProvider) {
            return FakeGpsResult(
                isFake     = true,
                reason     = "Terdeteksi sebagai mock location (lokasi palsu)",
                confidence = 100
            )
        }

        // Lapisan 2: Blacklist aplikasi fake GPS yang umum
        val fakeGpsPackages = listOf(
            "com.lexa.fakegps",
            "com.incorporateapps.fakegps.fre",
            "com.blogspot.newapphorizons.fakegps",
            "com.theappninjas.fakegpsjoystick",
            "com.fly.gps",
            "com.fake.location",
            "com.gps.mock",
            "com.route4me.routeoptimizer",
            "com.fakegps.mock"
        )
        val packageManager = context.packageManager
        for (pkg in fakeGpsPackages) {
            try {
                packageManager.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
                return FakeGpsResult(
                    isFake     = true,
                    reason     = "Aplikasi fake GPS terdeteksi terinstal di perangkat",
                    confidence = 90
                )
            } catch (_: PackageManager.NameNotFoundException) {
                // Tidak ditemukan, lanjut
            }
        }

        // Lapisan 3: Validasi akurasi GPS
        if (location.hasAccuracy()) {
            val accuracy = location.accuracy
            if (accuracy < 1.0f) {
                return FakeGpsResult(
                    isFake     = true,
                    reason     = "Akurasi GPS tidak natural (${accuracy}m) — kemungkinan lokasi palsu",
                    confidence = 75
                )
            }
            if (accuracy > 500.0f) {
                return FakeGpsResult(
                    isFake     = true,
                    reason     = "Sinyal GPS tidak valid (akurasi ${accuracy.toInt()}m)",
                    confidence = 60
                )
            }
        } else {
            return FakeGpsResult(
                isFake     = true,
                reason     = "Data akurasi GPS tidak tersedia",
                confidence = 50
            )
        }

        // Lapisan 4: Kecepatan tidak masuk akal
        if (location.hasSpeed() && location.speed > 50f) {
            return FakeGpsResult(
                isFake     = true,
                reason     = "Kecepatan pergerakan tidak masuk akal (${location.speed.toInt()} m/s)",
                confidence = 80
            )
        }

        // Lapisan 5: Altitude anomali
        if (location.hasAltitude()) {
            val altitude = location.altitude
            if (altitude < -100 || altitude > 5000) {
                return FakeGpsResult(
                    isFake     = true,
                    reason     = "Altitude tidak valid (${altitude.toInt()} mdpl)",
                    confidence = 70
                )
            }
        }

        return FakeGpsResult(isFake = false, confidence = 0)
    }

    fun cekAbsenHariIni() {
        viewModelScope.launch {
            try {
                val token = userPreferences.token.first() ?: ""
                val guruId = userPreferences.userId.first() ?: 0

                val response = RetrofitClient.apiService.cekAbsenHariIni(token, guruId)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.sudahAbsen && body.data != null) {
                        _absensiState.value = AbsensiState.AlreadyAbsen(body.data)
                    } else {
                        _absensiState.value = AbsensiState.Idle
                    }
                }
            } catch (_: Exception) {
                _absensiState.value = AbsensiState.Idle
            }
        }
    }

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

    fun hitungJarak(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3
        val φ1 = Math.toRadians(lat1)
        val φ2 = Math.toRadians(lat2)
        val Δφ = Math.toRadians(lat2 - lat1)
        val Δλ = Math.toRadians(lon2 - lon1)

        val a = sin(Δφ / 2) * sin(Δφ / 2) +
                cos(φ1) * cos(φ2) *
                sin(Δλ / 2) * sin(Δλ / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

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

    fun presensi(jamMasukAktual: String, latitude: Double, longitude: Double, location: Location? = null) {
        viewModelScope.launch {
            try {
                val (lokasiValid, pesanLokasi) = validasiLokasi(latitude, longitude)

                if (!lokasiValid) {
                    _absensiState.value = AbsensiState.Error(pesanLokasi)
                    return@launch
                }

                var isMock = false
                if (location != null) {
                    val fakeResult = deteksiFakeGps(location)
                    if (fakeResult.isFake) {
                        _absensiState.value = AbsensiState.FakeGpsDetected(fakeResult.reason)
                        return@launch
                    }
                    isMock = fakeResult.isFake
                }

                _absensiState.value = AbsensiState.Loading

                val token = userPreferences.token.first() ?: ""
                val guruId = userPreferences.userId.first() ?: 0

                val response = RetrofitClient.apiService.presensi(
                    token = token,
                    request = PresensiRequest(
                        guruId = guruId,
                        jamMasukAktual = jamMasukAktual,
                        latitude = latitude,
                        longitude = longitude,
                        isMockLocation = isMock,
                        gpsAccuracy = location?.accuracy
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

    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getRiwayat() {
        viewModelScope.launch {
            try {
                _riwayatState.value = _riwayatState.value.copy(isLoading = true)

                val token = userPreferences.token.first() ?: ""
                val guruId = userPreferences.userId.first() ?: 0

                val response = RetrofitClient.apiService.getRiwayat(token, guruId)

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
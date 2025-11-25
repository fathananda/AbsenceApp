package com.fathi.absenceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AdminDashboardState {
    object Idle : AdminDashboardState()
    object Loading : AdminDashboardState()
    data class Success(val data: DashboardData) : AdminDashboardState()
    data class Error(val message: String) : AdminDashboardState()
}

sealed class AdminGuruState {
    object Idle : AdminGuruState()
    object Loading : AdminGuruState()
    data class Success(val data: List<GuruData>) : AdminGuruState()
    data class Error(val message: String) : AdminGuruState()
}

sealed class AdminGuruDetailState {
    object Idle : AdminGuruDetailState()
    object Loading : AdminGuruDetailState()
    data class Success(val data: GuruData) : AdminGuruDetailState()
    data class Error(val message: String) : AdminGuruDetailState()
}

sealed class AdminActionState {
    object Idle : AdminActionState()
    object Loading : AdminActionState()
    data class Success(val message: String) : AdminActionState()
    data class Error(val message: String) : AdminActionState()
}

sealed class AdminAbsensiState {
    object Idle : AdminAbsensiState()
    object Loading : AdminAbsensiState()
    data class Success(val data: List<AbsensiData>) : AdminAbsensiState()
    data class Error(val message: String) : AdminAbsensiState()
}

sealed class AdminLaporanState {
    object Idle : AdminLaporanState()
    object Loading : AdminLaporanState()
    data class Success(val data: List<LaporanKehadiranData>) : AdminLaporanState()
    data class Error(val message: String) : AdminLaporanState()
}

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferences = UserPreferences(application)

    private val _dashboardState = MutableStateFlow<AdminDashboardState>(AdminDashboardState.Idle)
    val dashboardState: StateFlow<AdminDashboardState> = _dashboardState

    private val _guruState = MutableStateFlow<AdminGuruState>(AdminGuruState.Idle)
    val guruState: StateFlow<AdminGuruState> = _guruState

    private val _guruDetailState = MutableStateFlow<AdminGuruDetailState>(AdminGuruDetailState.Idle)
    val guruDetailState: StateFlow<AdminGuruDetailState> = _guruDetailState

    private val _actionState = MutableStateFlow<AdminActionState>(AdminActionState.Idle)
    val actionState: StateFlow<AdminActionState> = _actionState

    private val _absensiState = MutableStateFlow<AdminAbsensiState>(AdminAbsensiState.Idle)
    val absensiState: StateFlow<AdminAbsensiState> = _absensiState

    private val _laporanState = MutableStateFlow<AdminLaporanState>(AdminLaporanState.Idle)
    val laporanState: StateFlow<AdminLaporanState> = _laporanState

    private val _pengajuanState = MutableStateFlow<PengajuanState>(PengajuanState.Idle)
    val pengajuanState: StateFlow<PengajuanState> = _pengajuanState

    // Dashboard
    fun loadDashboard() {
        viewModelScope.launch {
            try {
                _dashboardState.value = AdminDashboardState.Loading
                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.getAdminDashboard(token)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data
                    if (data != null) {
                        _dashboardState.value = AdminDashboardState.Success(data)
                    } else {
                        _dashboardState.value = AdminDashboardState.Error("Data tidak tersedia")
                    }
                } else {
                    _dashboardState.value = AdminDashboardState.Error("Gagal memuat dashboard")
                }
            } catch (e: Exception) {
                _dashboardState.value = AdminDashboardState.Error("Error: ${e.message}")
            }
        }
    }

    // Guru List
    fun loadAllGuru() {
        viewModelScope.launch {
            try {
                _guruState.value = AdminGuruState.Loading
                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.getAllGuru(token)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data ?: emptyList()
                    _guruState.value = AdminGuruState.Success(data)
                } else {
                    _guruState.value = AdminGuruState.Error("Gagal memuat data guru")
                }
            } catch (e: Exception) {
                _guruState.value = AdminGuruState.Error("Error: ${e.message}")
            }
        }
    }

    // Guru Detail
    fun loadGuruDetail(guruId: Int) {
        viewModelScope.launch {
            try {
                _guruDetailState.value = AdminGuruDetailState.Loading
                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.getGuruDetail(token, guruId)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data
                    if (data != null) {
                        _guruDetailState.value = AdminGuruDetailState.Success(data)
                    } else {
                        _guruDetailState.value = AdminGuruDetailState.Error("Data tidak ditemukan")
                    }
                } else {
                    _guruDetailState.value = AdminGuruDetailState.Error("Gagal memuat detail guru")
                }
            } catch (e: Exception) {
                _guruDetailState.value = AdminGuruDetailState.Error("Error: ${e.message}")
            }
        }
    }

    // Update Guru
    fun updateGuru(guruId: Int, request: UpdateGuruRequest) {
        viewModelScope.launch {
            try {
                _actionState.value = AdminActionState.Loading
                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.updateGuru(token, guruId, request)

                if (response.isSuccessful) {
                    _actionState.value = AdminActionState.Success("Data guru berhasil diupdate")
                } else {
                    _actionState.value = AdminActionState.Error("Gagal mengupdate guru")
                }
            } catch (e: Exception) {
                _actionState.value = AdminActionState.Error("Error: ${e.message}")
            }
        }
    }

    // Delete Guru
    fun deleteGuru(guruId: Int) {
        viewModelScope.launch {
            try {
                _actionState.value = AdminActionState.Loading
                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.deleteGuru(token, guruId)

                if (response.isSuccessful) {
                    _actionState.value = AdminActionState.Success("Guru berhasil dihapus")
                } else {
                    _actionState.value = AdminActionState.Error("Gagal menghapus guru")
                }
            } catch (e: Exception) {
                _actionState.value = AdminActionState.Error("Error: ${e.message}")
            }
        }
    }

    // Create Guru
    fun createGuru(request: CreateGuruRequest) {
        viewModelScope.launch {
            try {
                _actionState.value = AdminActionState.Loading
                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.createGuru(token, request)

                if (response.isSuccessful) {
                    _actionState.value = AdminActionState.Success("Guru berhasil dibuat")
                } else {
                    val errorMsg = response.body()?.message ?: "Gagal membuat guru"
                    _actionState.value = AdminActionState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _actionState.value = AdminActionState.Error("Error: ${e.message}")
            }
        }
    }

    // Load All Absensi
    fun loadAllAbsensi(tanggal: String? = null, mahasiswaId: Int? = null) {
        viewModelScope.launch {
            try {
                _absensiState.value = AdminAbsensiState.Loading
                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.getAllAbsensi(token, tanggal, mahasiswaId)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data ?: emptyList()
                    _absensiState.value = AdminAbsensiState.Success(data)
                } else {
                    _absensiState.value = AdminAbsensiState.Error("Gagal memuat absensi")
                }
            } catch (e: Exception) {
                _absensiState.value = AdminAbsensiState.Error("Error: ${e.message}")
            }
        }
    }

    // Load Laporan Kehadiran
    fun loadLaporanKehadiran(bulan: Int? = null, tahun: Int? = null) {
        viewModelScope.launch {
            try {
                _laporanState.value = AdminLaporanState.Loading
                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.getLaporanKehadiran(token, bulan, tahun)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data ?: emptyList()
                    _laporanState.value = AdminLaporanState.Success(data)
                } else {
                    _laporanState.value = AdminLaporanState.Error("Gagal memuat laporan")
                }
            } catch (e: Exception) {
                _laporanState.value = AdminLaporanState.Error("Error: ${e.message}")
            }
        }
    }

    // Load Pengajuan Pending
    fun loadPengajuanPending() {
        viewModelScope.launch {
            try {
                _pengajuanState.value = PengajuanState.Loading
                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.getPengajuanPending(token)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data ?: emptyList()
                    _pengajuanState.value = PengajuanState.Success(data)
                } else {
                    _pengajuanState.value = PengajuanState.Error("Gagal memuat pengajuan")
                }
            } catch (e: Exception) {
                _pengajuanState.value = PengajuanState.Error("Error: ${e.message}")
            }
        }
    }

    // Proses Pengajuan
    fun prosesPengajuan(pengajuanId: Int, status: String, alasanDitolak: String? = null) {
        viewModelScope.launch {
            try {
                _actionState.value = AdminActionState.Loading
                val token = userPreferences.token.first() ?: ""
                val response = RetrofitClient.apiService.prosesPengajuan(
                    token,
                    pengajuanId,
                    ProsesPengajuanRequest(status, alasanDitolak)
                )

                if (response.isSuccessful) {
                    _actionState.value = AdminActionState.Success("Pengajuan berhasil diproses")
                } else {
                    _actionState.value = AdminActionState.Error("Gagal memproses pengajuan")
                }
            } catch (e: Exception) {
                _actionState.value = AdminActionState.Error("Error: ${e.message}")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = AdminActionState.Idle
    }
}
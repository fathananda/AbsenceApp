package com.fathi.absenceapp

import retrofit2.Response
import retrofit2.http.*
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody


// Data Models
data class RegisterRequest(
    val nama: String,
    val nim: String,
    val password: String,
    val role: String? = null
)

data class LoginRequest(
    val nim: String,
    val password: String
)

data class AbsenRequest(
    @SerializedName("mahasiswa_id")
    val mahasiswaId: Int,
    val latitude: Double,
    val longitude: Double
)

// NEW: Request untuk presensi dengan jam masuk
data class PresensiRequest(
    @SerializedName("mahasiswa_id")
    val mahasiswaId: Int,
    @SerializedName("jam_seharusnya")
    val jamSeharusnya: String? = null,
    @SerializedName("jam_masuk_aktual")// Optional, default dari server
    val jamMasukAktual: String,
    val latitude: Double,
    val longitude: Double
)

data class MahasiswaData(
    val id: Int,
    val nama: String,
    val nim: String,
    val role: String? = null
)

// UPDATED: Tambah field keterlambatan
data class AbsensiData(
    val id: Int,
    @SerializedName("mahasiswa_id")
    val mahasiswaId: Int,
    val tanggal: String,
    @SerializedName("jam_seharusnya")
    val jamSeharusnya: String? = null,
    @SerializedName("jam_masuk_aktual")
    val jamMasukAktual: String? = null,
    @SerializedName("keterlambatan_menit")
    val keterlambatanMenit: Int? = null,
    @SerializedName("kategori_keterlambatan")
    val kategoriKeterlambatan: String? = null,
    val sanksi: String? = null,
    val latitude: Double,
    val longitude: Double,
    val nama: String? = null,
    val nim: String? = null,
    val status: String? = null,
    @SerializedName("jarak_dari_kantor")
    val jarakDariKantor: Double? = null
)

// NEW: Kalender
data class KalenderData(
    val id: Int,
    val tanggal: String,
    val jenis: String,
    val keterangan: String?
)

data class KalenderRequest(
    val tanggal: String,
    val jenis: String,
    val keterangan: String?
)

// NEW: Pengajuan
data class PengajuanData(
    val id: Int,
    @SerializedName("mahasiswa_id")
    val mahasiswaId: Int,
    val jenis: String,
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String,
    @SerializedName("tanggal_selesai")
    val tanggalSelesai: String,
    val keterangan: String?,
    @SerializedName("foto_bukti")
    val fotoBukti: String?,
    val status: String,
    @SerializedName("alasan_ditolak")
    val alasanDitolak: String?,
    @SerializedName("diproses_oleh")
    val diprosesOleh: Int?,
    @SerializedName("diproses_pada")
    val diprosesPada: String?,
    @SerializedName("created_at")
    val createdAt: String,
    val nama: String? = null,
    val nim: String? = null
)

data class ProsesPengajuanRequest(
    val status: String,
    @SerializedName("alasan_ditolak")
    val alasanDitolak: String? = null
)

// NEW: Tunjangan
data class KehadiranDetail(
    @SerializedName("total_hari_kerja")
    val totalHariKerja: Int,
    val hadir: Int,
    @SerializedName("tepat_waktu")
    val tepatWaktu: Int,
    @SerializedName("telat_ringan")
    val telatRingan: Int,
    @SerializedName("telat_sedang")
    val telatSedang: Int,
    @SerializedName("telat_berat")
    val telatBerat: Int,
    val izin: Int,
    val sakit: Int,
    val dinas: Int,
    val alpa: Int
)

data class TunjanganDetail(
    @SerializedName("tunjangan_hadir")
    val tunjanganHadir: Double,
    val detail: String
)

data class PotonganDetail(
    @SerializedName("telat_sedang")
    val telatSedang: Double,
    @SerializedName("telat_berat")
    val telatBerat: Double,
    val alpa: Double,
    val total: Double,
    val detail: List<String>
)

data class TunjanganData(
    val periode: String,
    val bulan: Int,
    val tahun: Int,
    @SerializedName("gaji_pokok")
    val gajiPokok: Double,
    val kehadiran: KehadiranDetail,
    val tunjangan: TunjanganDetail,
    val potongan: PotonganDetail,
    @SerializedName("total_tunjangan")
    val totalTunjangan: Double,
    @SerializedName("total_potongan")
    val totalPotongan: Double,
    @SerializedName("gaji_bersih")
    val gajiBersih: Double
)

data class ApiResponse<T>(
    val message: String,
    val data: T? = null,
    val token: String? = null
)

// NEW: Konfigurasi
data class KonfigurasiData(
    @SerializedName("jam_masuk_default") val jamMasukDefault: String,
    @SerializedName("kantor_latitude") val kantorLatitude: String? = null,
    @SerializedName("kantor_longitude") val kantorLongitude: String? = null,
    @SerializedName("kantor_nama") val kantorNama: String? = null,
    @SerializedName("radius_maksimal") val radiusMaksimal: String? = null
)

// NEW: Response cek absen hari ini
data class CekAbsenResponse(
    @SerializedName("sudah_absen")
    val sudahAbsen: Boolean,
    val data: AbsensiData? = null
)

// Retrofit API Service
interface ApiService {

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<MahasiswaData>>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<MahasiswaData>>

    // Endpoint lama (backward compatibility)
    @POST("absen")
    suspend fun absen(
        @Header("Authorization") token: String,
        @Body request: AbsenRequest
    ): Response<ApiResponse<AbsensiData>>

    // NEW: Endpoint presensi dengan jam masuk
    @POST("presensi")
    suspend fun presensi(
        @Header("Authorization") token: String,
        @Body request: PresensiRequest
    ): Response<ApiResponse<AbsensiData>>

    @GET("riwayat/{id_mahasiswa}")
    suspend fun getRiwayat(
        @Header("Authorization") token: String,
        @Path("id_mahasiswa") idMahasiswa: Int
    ): Response<ApiResponse<List<AbsensiData>>>

    @GET("kalender")
    suspend fun getKalender(
        @Header("Authorization") token: String,
        @Query("bulan") bulan: Int? = null,
        @Query("tahun") tahun: Int? = null
    ): Response<ApiResponse<List<KalenderData>>>

    @POST("kalender")
    suspend fun tambahKalender(
        @Header("Authorization") token: String,
        @Body request: KalenderRequest
    ): Response<ApiResponse<KalenderData>>

    @DELETE("kalender/{id}")
    suspend fun hapusKalender(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<ApiResponse<Unit>>

    // Pengajuan
    @Multipart
    @POST("pengajuan")
    suspend fun ajukanPengajuan(
        @Header("Authorization") token: String,
        @Part("mahasiswa_id") mahasiswaId: RequestBody,
        @Part("jenis") jenis: RequestBody,
        @Part("tanggal_mulai") tanggalMulai: RequestBody,
        @Part("tanggal_selesai") tanggalSelesai: RequestBody,
        @Part("keterangan") keterangan: RequestBody?,
        @Part fotoBukti: MultipartBody.Part?
    ): Response<ApiResponse<PengajuanData>>

    @GET("pengajuan")
    suspend fun getPengajuan(
        @Header("Authorization") token: String,
        @Query("mahasiswa_id") mahasiswaId: Int? = null,
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<PengajuanData>>>

    @PUT("pengajuan/{id}/proses")
    suspend fun prosesPengajuan(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: ProsesPengajuanRequest
    ): Response<ApiResponse<Unit>>

    // Tunjangan
    @GET("tunjangan/{mahasiswa_id}")
    suspend fun getTunjangan(
        @Header("Authorization") token: String,
        @Path("mahasiswa_id") mahasiswaId: Int,
        @Query("bulan") bulan: Int? = null,
        @Query("tahun") tahun: Int? = null
    ): Response<ApiResponse<TunjanganData>>

    @GET("tunjangan/{mahasiswa_id}/ringkasan")
    suspend fun getRingkasanTunjangan(
        @Header("Authorization") token: String,
        @Path("mahasiswa_id") mahasiswaId: Int
    ): Response<ApiResponse<List<TunjanganData>>>

    // NEW: Get konfigurasi
    @GET("konfigurasi")
    suspend fun getKonfigurasi(
        @Header("Authorization") token: String
    ): Response<ApiResponse<KonfigurasiData>>

    // NEW: Update konfigurasi
    @PUT("konfigurasi")
    suspend fun updateKonfigurasi(
        @Header("Authorization") token: String,
        @Body config: Map<String, String>
    ): Response<ApiResponse<KonfigurasiData>>

    // NEW: Cek apakah sudah absen hari ini
    @GET("cek-absen-hari-ini/{id_mahasiswa}")
    suspend fun cekAbsenHariIni(
        @Header("Authorization") token: String,
        @Path("id_mahasiswa") idMahasiswa: Int
    ): Response<CekAbsenResponse>
}
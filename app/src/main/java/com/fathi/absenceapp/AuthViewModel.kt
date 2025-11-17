package com.fathi.absenceapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val isLoggedIn = userPreferences.isLoggedIn

    fun register(nama: String, nim: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val response = RetrofitClient.apiService.register(
                    RegisterRequest(nama, nim, password)
                )

                if (response.isSuccessful) {
                    _authState.value = AuthState.Success("Registrasi berhasil! Silakan login.")
                } else {
                    val errorMsg = response.body()?.message ?: "Registrasi gagal"
                    _authState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error: ${e.message}")
            }
        }
    }

    fun login(nim: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val response = RetrofitClient.apiService.login(
                    LoginRequest(nim, password)
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val token = body.token ?: ""
                    val userData = body.data

                    if (userData != null) {
                        userPreferences.saveUserData(
                            token = "Bearer $token",
                            id = userData.id,
                            nama = userData.nama,
                            nim = userData.nim
                        )
                        _authState.value = AuthState.Success("Login berhasil!")
                    } else {
                        _authState.value = AuthState.Error("Data user tidak lengkap")
                    }
                } else {
                    val errorMsg = response.body()?.message ?: "Login gagal"
                    _authState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearUserData()
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}
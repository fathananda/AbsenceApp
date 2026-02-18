package com.fathi.absenceapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("token")
        private val USER_ID_KEY = intPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_EMAIL_KEY   = stringPreferencesKey("user_email")   // REVISI: email

        private val USER_NIP_KEY = stringPreferencesKey("user_nip")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
    }

    suspend fun saveUserData(token: String, id: Int, nama: String, email: String, nip: String? = null, role: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_ID_KEY] = id
            preferences[USER_NAME_KEY] = nama
            preferences[USER_EMAIL_KEY]   = email
            if (nip != null) preferences[USER_NIP_KEY] = nip
            preferences[USER_ROLE_KEY] = role
            preferences[IS_LOGGED_IN_KEY] = true
        }
    }

    suspend fun clearUserData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    val token: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    val userId: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    val userName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY]
    }


    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL_KEY]
    }

    val userNip: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_NIP_KEY]
    }

    val userRole: Flow<String?> = context.dataStore.data.map { preferences -> // TAMBAHAN
        preferences[USER_ROLE_KEY]
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN_KEY] ?: false
    }

    val isAdmin: Flow<Boolean> = context.dataStore.data.map { preferences -> // TAMBAHAN
        preferences[USER_ROLE_KEY] == "admin"
    }
}
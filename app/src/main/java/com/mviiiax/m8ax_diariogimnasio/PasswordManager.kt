package com.mviiiax.m8ax_diariogimnasio

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PasswordManager(context: Context) {
    private val masterKey =
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "M8AX-Clave_De_App",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePassword(password: String) {
        prefs.edit().putString("app_password", password).apply()
    }

    fun getPassword(): String? = prefs.getString("app_password", null)
    fun hasPassword(): Boolean = prefs.contains("app_password")
}
package com.silverlingo.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class UserProfile(
    val name: String = "",
    val birthDate: String = "",
    val address: String = "",
    val accountInfo: String = ""
) {
    val isRegistered: Boolean get() = name.isNotBlank()

    fun calculateAge(): Int {
        if (birthDate.length < 2) return 70
        val yy = birthDate.substring(0, 2).toIntOrNull() ?: 50
        val fullYear = if (yy > 30) (1900 + yy) else (2000 + yy)
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return currentYear - fullYear
    }
}

class UserProfileManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "silverlingo_user_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveProfile(profile: UserProfile) {
        prefs.edit()
            .putString("user_name", profile.name)
            .putString("user_birth", profile.birthDate)
            .putString("user_address", profile.address)
            .putString("user_account", profile.accountInfo)
            .apply()
    }

    fun getProfile(): UserProfile {
        return UserProfile(
            name = prefs.getString("user_name", "") ?: "",
            birthDate = prefs.getString("user_birth", "") ?: "",
            address = prefs.getString("user_address", "") ?: "",
            accountInfo = prefs.getString("user_account", "") ?: ""
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

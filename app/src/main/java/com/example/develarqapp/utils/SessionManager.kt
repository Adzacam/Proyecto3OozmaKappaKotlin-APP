package com.example.develarqapp.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "DevelArqSession"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_APELLIDO = "user_apellido"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROL = "user_rol"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SAVED_EMAIL = "saved_email"
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUserData(
        id: Long,
        name: String,
        apellido: String,
        email: String,
        rol: String
    ) {
        prefs.edit().apply {
            putLong(KEY_USER_ID, id)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_APELLIDO, apellido)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ROL, rol)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun saveRememberMePreference(email: String, remember: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_REMEMBER_ME, remember)
            if (remember) {
                putString(KEY_SAVED_EMAIL, email)
            } else {
                remove(KEY_SAVED_EMAIL)
            }
            apply()
        }
    }

    fun getRememberedEmail(): String? {
        // Solo devuelve el email si la opción "Recordarme" estaba marcada
        return if (prefs.getBoolean(KEY_REMEMBER_ME, false)) {
            prefs.getString(KEY_SAVED_EMAIL, null)
        } else {
            null
        }
    }

    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, -1)
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "") ?: ""
    }

    fun getUserApellido(): String {
        return prefs.getString(KEY_USER_APELLIDO, "") ?: ""
    }

    fun getUserEmail(): String {
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun getUserRol(): String {
        return prefs.getString(KEY_USER_ROL, "") ?: ""
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun clearSession() {
        // Guardar el estado de "Recordarme" antes de borrar todo
        val rememberedEmail = getRememberedEmail()

        // Borrar toda la sesión
        prefs.edit().clear().apply()

        // Si el usuario quería ser recordado, restauramos esa preferencia
        if (rememberedEmail != null) {
            saveRememberMePreference(rememberedEmail, true)
        }
    }
}


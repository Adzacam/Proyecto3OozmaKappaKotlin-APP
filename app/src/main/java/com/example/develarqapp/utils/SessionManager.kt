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

    // ========================================
    // TOKEN METHODS
    // ========================================

    /**
     * Guardar token de autenticación
     */
    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * Obtener token de autenticación (alias de getAuthToken)
     */
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    /**
     * Obtener token de autenticación (método original)
     */
    fun getAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    // ========================================
    // USER DATA METHODS
    // ========================================

    /**
     * Guardar datos completos del usuario
     */
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

    /**
     * Obtener ID del usuario
     */
    fun getUserId(): Long? {
        val id = prefs.getLong(KEY_USER_ID, -1)
        return if (id != -1L) id else null
    }

    /**
     * Obtener nombre del usuario
     */
    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "") ?: ""
    }

    /**
     * Obtener apellido del usuario
     */
    fun getUserApellido(): String {
        return prefs.getString(KEY_USER_APELLIDO, "") ?: ""
    }

    /**
     * Obtener email del usuario
     */
    fun getUserEmail(): String {
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    /**
     * Obtener rol del usuario
     */
    fun getUserRol(): String {
        return prefs.getString(KEY_USER_ROL, "") ?: ""
    }

    /**
     * Alias de getUserRol() para compatibilidad
     */
    fun getUserRole(): String {
        return getUserRol()
    }

    /**
     * Obtener nombre completo del usuario
     */
    fun getUserFullName(): String {
        val name = getUserName()
        val apellido = getUserApellido()
        return "$name $apellido".trim()
    }

    /**
     * Verificar si el usuario está autenticado
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getToken() != null
    }

    /**
     * Actualizar solo el nombre del usuario
     */
    fun saveUserName(newName: String) {
        prefs.edit().putString(KEY_USER_NAME, newName).apply()
    }

    // ========================================
    // REMEMBER ME METHODS
    // ========================================

    /**
     * Guardar preferencia de "Recordarme"
     */
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

    /**
     * Obtener email guardado si "Recordarme" está activo
     */
    fun getRememberedEmail(): String? {
        return if (prefs.getBoolean(KEY_REMEMBER_ME, false)) {
            prefs.getString(KEY_SAVED_EMAIL, null)
        } else {
            null
        }
    }

    /**
     * Verificar si "Recordarme" está activo
     */
    fun isRememberMeEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_ME, false)
    }

    // ========================================
    // SESSION MANAGEMENT
    // ========================================

    /**
     * Limpiar toda la sesión
     */
    fun clearSession() {
        // Guardar el estado de "Recordarme" antes de borrar todo
        val rememberedEmail = getRememberedEmail()
        val rememberMeEnabled = isRememberMeEnabled()

        // Borrar toda la sesión
        prefs.edit().clear().apply()

        // Si el usuario quería ser recordado, restauramos esa preferencia
        if (rememberMeEnabled && rememberedEmail != null) {
            saveRememberMePreference(rememberedEmail, true)
        }
    }

    /**
     * Verificar si la sesión es válida
     */
    fun isSessionValid(): Boolean {
        return isLoggedIn() &&
                getToken() != null &&
                getUserId() != null &&
                getUserRol().isNotEmpty()
    }

    // ========================================
    // DEBUGGING METHODS (Solo para desarrollo)
    // ========================================

    /**
     * Imprimir información de la sesión actual (solo debug)
     */
    fun printSessionInfo() {
        println("========== SESSION INFO ==========")
        println("Is Logged In: ${isLoggedIn()}")
        println("Token: ${getToken()?.take(20)}...")
        println("User ID: ${getUserId()}")
        println("User Name: ${getUserName()}")
        println("User Apellido: ${getUserApellido()}")
        println("User Email: ${getUserEmail()}")
        println("User Rol: ${getUserRol()}")
        println("Remember Me: ${isRememberMeEnabled()}")
        println("==================================")
    }
}
package com.example.develarqapp.utils

import android.util.Patterns

object Validator {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.(com|net|org|edu|bo|gov|info|biz|io|co)\$")
    private val PASSWORD_REGEX = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,20}\$")

    fun isValidEmail(email: String): Boolean {
        // No debe contener espacios
        if (email.contains(" ")) return false
        // Debe cumplir la expresión regular
        return EMAIL_REGEX.matches(email)
    }

    fun isValidPassword(password: String): Boolean {
        // Longitud mínima y máxima
        if (password.length < 8 || password.length > 20) return false

        // No permitir más de un espacio seguido
        if (password.contains("  ")) return false

        // No permitir espacios al inicio o al final
        if (password.startsWith(" ") || password.endsWith(" ")) return false

        return true
    }

    fun validateLogin(email: String, password: String): ValidationResult {
        return when {
            email.isEmpty() -> ValidationResult(false, "El correo electrónico es requerido")
            !isValidEmail(email) -> ValidationResult(false, "Correo electrónico inválido o con símbolos no permitidos")
            password.isEmpty() -> ValidationResult(false, "La contraseña es requerida")
            !isValidPassword(password) -> ValidationResult(false, "La contraseña debe tener entre 8 y 20 caracteres y no contener espacios inválidos")
            else -> ValidationResult(true, "")
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String
)

package com.example.develarqapp.utils

import android.util.Patterns

object Validator {

    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun validateLogin(email: String, password: String): ValidationResult {
        return when {
            email.isEmpty() -> ValidationResult(false, "El correo electrónico es requerido")
            !isValidEmail(email) -> ValidationResult(false, "Correo electrónico inválido")
            password.isEmpty() -> ValidationResult(false, "La contraseña es requerida")
            !isValidPassword(password) -> ValidationResult(false, "La contraseña debe tener al menos 6 caracteres")
            else -> ValidationResult(true, "")
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String
)

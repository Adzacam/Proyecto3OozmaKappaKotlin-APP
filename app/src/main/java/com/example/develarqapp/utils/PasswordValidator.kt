package com.example.develarqapp.utils

object PasswordValidator {

    data class ValidationRule(
        val regex: Regex,
        val errorMessage: String,
        val isValid: Boolean = false
    )

    fun validatePassword(password: String): PasswordValidationResult {
        val rules = listOf(
            ValidationRule(
                Regex("^.{8,20}$"),
                "Debe tener entre 8 y 20 caracteres",
                password.matches(Regex("^.{8,20}$"))
            ),
            ValidationRule(
                Regex(".*[A-Z].*"),
                "Debe contener al menos una mayúscula",
                password.contains(Regex("[A-Z]"))
            ),
            ValidationRule(
                Regex(".*[a-z].*"),
                "Debe contener al menos una minúscula",
                password.contains(Regex("[a-z]"))
            ),
            ValidationRule(
                Regex(".*\\d.*"),
                "Debe contener al menos un número",
                password.contains(Regex("\\d"))
            ),
            ValidationRule(
                Regex(".*[@\$!%*?&].*"),
                "Debe contener al menos un símbolo (@\$!%*?&)",
                password.contains(Regex("[@\$!%*?&]"))
            ),
            ValidationRule(
                Regex("^\\S(?!.*\\s\\s).*?\\S$|^\\S$|^$"),
                "No puede tener espacios al inicio, final o dobles espacios",
                password.matches(Regex("^\\S(?!.*\\s\\s).*?\\S$|^\\S$|^$"))
            )
        )

        val allValid = rules.all { it.isValid }
        val errors = rules.filter { !it.isValid }.map { it.errorMessage }

        return PasswordValidationResult(
            isValid = allValid,
            rules = rules,
            errors = errors
        )
    }

    data class PasswordValidationResult(
        val isValid: Boolean,
        val rules: List<ValidationRule>,
        val errors: List<String>
    )
}

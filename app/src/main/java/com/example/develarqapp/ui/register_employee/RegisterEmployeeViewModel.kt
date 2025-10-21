// ==========================================
// RegisterEmployeeViewModel.kt
// Ubicación: ui/register_employee/RegisterEmployeeViewModel.kt
// ==========================================
package com.example.develarqapp.ui.register_employee

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.CreateUserRequest
import com.example.develarqapp.utils.Validator
import kotlinx.coroutines.launch

class RegisterEmployeeViewModel : ViewModel() {

    private val apiService = ApiConfig.getApiService()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _registerResult = MutableLiveData<RegisterResult>()
    val registerResult: LiveData<RegisterResult> = _registerResult

    fun registerEmployee(
        name: String,
        apellido: String,
        email: String,
        phone: String?,
        password: String,
        rol: String
    ) {
        // Validaciones
        val validation = validateInputs(name, apellido, email, password, rol)
        if (!validation.isValid) {
            _registerResult.value = RegisterResult.Error(validation.message)
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val request = CreateUserRequest(
                    name = name.trim(),
                    apellido = apellido.trim(),
                    email = email.trim(),
                    password = password,
                    telefono = phone?.trim(),
                    rol = rol
                )

                val response = apiService.createUser(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    _registerResult.value = RegisterResult.Success(
                        response.body()?.message ?: "Usuario creado exitosamente"
                    )
                } else {
                    _registerResult.value = RegisterResult.Error(
                        response.body()?.message ?: "Error al crear usuario"
                    )
                }
            } catch (e: Exception) {
                _registerResult.value = RegisterResult.Error(
                    "Error de conexión: ${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun validateInputs(
        name: String,
        apellido: String,
        email: String,
        password: String,
        rol: String
    ): ValidationResult {
        return when {
            name.trim().isEmpty() ->
                ValidationResult(false, "El nombre es requerido")
            apellido.trim().isEmpty() ->
                ValidationResult(false, "El apellido es requerido")
            email.trim().isEmpty() ->
                ValidationResult(false, "El email es requerido")
            !Validator.isValidEmail(email) ->
                ValidationResult(false, "Email inválido")
            password.isEmpty() ->
                ValidationResult(false, "La contraseña es requerida")
            password.length < 6 ->
                ValidationResult(false, "La contraseña debe tener al menos 6 caracteres")
            rol.isEmpty() ->
                ValidationResult(false, "Debe seleccionar un rol")
            else -> ValidationResult(true, "")
        }
    }

    fun resetResult() {
        _registerResult.value = RegisterResult.Idle
    }

    data class ValidationResult(val isValid: Boolean, val message: String)
}

sealed class RegisterResult {
    object Idle : RegisterResult()
    data class Success(val message: String) : RegisterResult()
    data class Error(val message: String) : RegisterResult()
}


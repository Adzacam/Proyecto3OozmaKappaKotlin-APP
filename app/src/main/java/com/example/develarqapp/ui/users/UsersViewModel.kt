package com.example.develarqapp.ui.users

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.*
import com.example.develarqapp.utils.PasswordValidator
import com.example.develarqapp.utils.Validator
import kotlinx.coroutines.launch

class UsersViewModel : ViewModel() {

    private val apiService = ApiConfig.getApiService()

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val _deletedUsers = MutableLiveData<List<User>>()
    val deletedUsers: LiveData<List<User>> = _deletedUsers

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _operationSuccess = MutableLiveData<String?>()
    val operationSuccess: LiveData<String?> = _operationSuccess

    fun loadUsers() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = apiService.getUsers()

                if (response.isSuccessful && response.body()?.success == true) {
                    _users.value = response.body()?.data ?: emptyList()
                } else {
                    _error.value = response.body()?.message ?: "Error al cargar usuarios"
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadDeletedUsers() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = apiService.getDeletedUsers()

                if (response.isSuccessful && response.body()?.success == true) {
                    _deletedUsers.value = response.body()?.data ?: emptyList()
                } else {
                    _error.value = response.body()?.message ?: "Error al cargar usuarios eliminados"
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUser(
        id: Long,
        name: String,
        apellido: String,
        email: String,
        phone: String?,
        rol: String,
        password: String? = null
    ) {
        // Validaciones
        val validation = validateUpdateInputs(name, apellido, email, rol, password)
        if (!validation.isValid) {
            _error.value = validation.message
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val request = UpdateUserRequest(
                    id = id,
                    name = name.trim(),
                    apellido = apellido.trim(),
                    email = email.trim(),
                    telefono = phone?.trim(),
                    rol = rol,
                    password = password?.trim()
                )

                val response = apiService.updateUser(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    _operationSuccess.value = "Usuario actualizado exitosamente"
                    loadUsers() // Recargar lista
                } else {
                    _error.value = response.body()?.message ?: "Error al actualizar usuario"
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteUser(userId: Long) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val request = DeleteUserRequest(userId)
                val response = apiService.deleteUser(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    _operationSuccess.value = "Usuario eliminado exitosamente"
                    loadUsers()
                } else {
                    _error.value = response.body()?.message ?: "Error al eliminar usuario"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun restoreUser(userId: Long) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val request = DeleteUserRequest(userId)
                val response = apiService.restoreUser(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    _operationSuccess.value = "Usuario restaurado exitosamente"
                    loadDeletedUsers()
                } else {
                    _error.value = response.body()?.message ?: "Error al restaurar usuario"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleUserStatus(userId: Long) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val request = DeleteUserRequest(userId)
                val response = apiService.toggleUserStatus(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    _operationSuccess.value = response.body()?.message
                    loadUsers()
                } else {
                    _error.value = response.body()?.message ?: "Error al cambiar estado"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    private fun validateUpdateInputs(
        name: String,
        apellido: String,
        email: String,
        rol: String,
        password: String?
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
            rol.isEmpty() ->
                ValidationResult(false, "Debe seleccionar un rol")
            password != null && password.isNotEmpty() && !PasswordValidator.validatePassword(password).isValid -> {
                val errors = PasswordValidator.validatePassword(password).errors
                ValidationResult(false, errors.firstOrNull() ?: "Contraseña inválida")
            }
            else -> ValidationResult(true, "")
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _operationSuccess.value = null
    }

    data class ValidationResult(val isValid: Boolean, val message: String)
}
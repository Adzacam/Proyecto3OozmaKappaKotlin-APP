package com.example.develarqapp.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.repository.AuthRepository
import com.example.develarqapp.utils.SessionManager
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private lateinit var sessionManager: SessionManager

    fun loadUserProfile(sessionManager: SessionManager) {
        this.sessionManager = sessionManager
        _userName.value = sessionManager.getUserName()
    }

    fun updateUserName(newName: String) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val userId = sessionManager.getUserId()
                val token = sessionManager.getAuthToken()

                if (userId != null && token != null) {
                    val result = repository.updateUserProfile(
                        id = TODO(),
                        name = TODO(),
                        apellido = TODO(),
                        email = TODO(),
                        telefono = TODO(),
                        rol = TODO(),
                        password = TODO(),
                        token = TODO()
                    )

                    if (result.isSuccess) {
                        // Actualizar sesión
                        sessionManager.saveUserData(
                            id = TODO(),
                            name = TODO(),
                            apellido = TODO(),
                            email = TODO(),
                            rol = TODO()
                        )
                        _userName.value = newName
                        _successMessage.value = "Perfil actualizado exitosamente"
                    } else {
                        _errorMessage.value = result.exceptionOrNull()?.message
                            ?: "Error al actualizar el perfil"
                    }
                } else {
                    _errorMessage.value = "Sesión inválida"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val userId = sessionManager.getUserId()
                val token = sessionManager.getAuthToken()

                if (userId != null && token != null) {
                    val result = repository.updatePassword(
                        userId = userId,
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        token = token
                    )

                    if (result.isSuccess) {
                        _successMessage.value = "Contraseña actualizada exitosamente"
                    } else {
                        _errorMessage.value = result.exceptionOrNull()?.message
                            ?: "Error al actualizar la contraseña"
                    }
                } else {
                    _errorMessage.value = "Sesión inválida"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSuccess() {
        _successMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
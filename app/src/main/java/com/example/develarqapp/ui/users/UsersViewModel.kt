package com.example.develarqapp.ui.users

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.User
import com.example.develarqapp.data.model.DeleteUserRequest
import kotlinx.coroutines.launch

class UsersViewModel : ViewModel() {

    private val apiService = ApiConfig.getApiService()

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

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

    fun deleteUser(userId: Long) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val request = DeleteUserRequest(userId)
                val response = apiService.deleteUser(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    _operationSuccess.value = "Usuario eliminado exitosamente"
                    loadUsers() // Recargar lista
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

    fun toggleUserStatus(userId: Long) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val request = DeleteUserRequest(userId)
                val response = apiService.toggleUserStatus(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    _operationSuccess.value = response.body()?.message
                    loadUsers() // Recargar lista
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

    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _operationSuccess.value = null
    }
}
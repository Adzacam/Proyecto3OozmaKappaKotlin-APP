package com.example.develarqapp.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.ForgotPasswordRequest
import kotlinx.coroutines.launch

sealed class ForgotPasswordState {
    object Idle : ForgotPasswordState()
    data class Success(val message: String) : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}

class ForgotPasswordViewModel : ViewModel() {

    private val apiService = ApiConfig.getApiService()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _resetState = MutableLiveData<ForgotPasswordState>()
    val resetState: LiveData<ForgotPasswordState> = _resetState

    fun sendResetLink(email: String) {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _resetState.value = ForgotPasswordState.Error("Por favor, ingresa un correo válido.")
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = apiService.forgotPassword(ForgotPasswordRequest(email))
                if (response.isSuccessful && response.body()?.success == true) {
                    _resetState.value = ForgotPasswordState.Success(response.body()?.message ?: "Enlace enviado.")
                } else {
                    _resetState.value = ForgotPasswordState.Error(response.body()?.message ?: "Ocurrió un error.")
                }
            } catch (_: Exception) {
                _resetState.value = ForgotPasswordState.Error("Error de conexión. Inténtalo de nuevo.")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
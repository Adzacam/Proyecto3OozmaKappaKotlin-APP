package com.example.develarqapp.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.api.ApiConfig
import com.example.develarqapp.data.model.LoginRequest
import com.example.develarqapp.data.model.UserData
import com.example.develarqapp.utils.DeviceInfoUtil // ✅ Importar esto
import com.example.develarqapp.utils.Validator
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val apiService = ApiConfig.getApiService()

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, password: String) {
        val validation = Validator.validateLogin(email, password)
        if (!validation.isValid) {
            _loginState.value = LoginState.Error(validation.errorMessage)
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                // ✅ MODIFICADO: Incluir información del dispositivo
                val request = LoginRequest(
                    email = email,
                    password = password,
                    deviceModel = DeviceInfoUtil.getDeviceModel(),
                    androidVersion = DeviceInfoUtil.getAndroidVersion(),
                    sdkVersion = DeviceInfoUtil.getSdkVersion()
                )

                val response = apiService.login(request)
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.success == true && loginResponse.data != null) {
                        _loginState.value = LoginState.Success(
                            loginResponse.data,
                            loginResponse.token ?: ""
                        )
                    } else {
                        _loginState.value = LoginState.Error(loginResponse?.message ?: "Error desconocido")
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Credenciales incorrectas"
                        403 -> "Usuario inactivo"
                        422 -> "Datos inválidos"
                        else -> "Error del servidor: ${response.code()}"
                    }
                    _loginState.value = LoginState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Error de conexión: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
}

sealed class LoginState {
    object Idle : LoginState()
    data class Success(val userData: UserData, val token: String) : LoginState()
    data class Error(val message: String) : LoginState()
}
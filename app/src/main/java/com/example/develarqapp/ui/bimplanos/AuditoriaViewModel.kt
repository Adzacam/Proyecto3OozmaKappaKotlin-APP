package com.example.develarqapp.ui.audit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.model.AuditoriaLog
import com.example.develarqapp.data.repository.AuditoriaRepository
import kotlinx.coroutines.launch

class AuditoriaViewModel : ViewModel() {

    private val repository = AuditoriaRepository()

    private val _auditLogs = MutableLiveData<List<AuditoriaLog>>()
    val auditLogs: LiveData<List<AuditoriaLog>> = _auditLogs

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage


    fun loadAuditLogs(token: String) {
        _isLoading.value = true

        viewModelScope.launch {
            try {

                val result = repository.getAuditLogs(token)

                if (result.isSuccess) {
                    _auditLogs.value = result.getOrNull() ?: emptyList()
                } else {
                    _errorMessage.value = "Error al cargar registros de auditoría"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
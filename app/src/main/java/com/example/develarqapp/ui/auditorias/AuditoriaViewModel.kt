package com.example.develarqapp.ui.auditorias

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.model.AuditoriaLog
import com.example.develarqapp.data.repository.AuditoriaRepository
import kotlinx.coroutines.launch

class AuditoriaViewModel : ViewModel() {

    private val repository = AuditoriaRepository()

    private val _allLogs = MutableLiveData<List<AuditoriaLog>>()

    private val _filteredLogs = MutableLiveData<List<AuditoriaLog>>()
    val filteredLogs: LiveData<List<AuditoriaLog>> = _filteredLogs

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadAuditLogs(token: String) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = repository.getAuditLogs(token)

                if (result.isSuccess) {
                    val logs = result.getOrNull() ?: emptyList()
                    _allLogs.value = logs
                    _filteredLogs.value = logs
                    _isEmpty.value = logs.isEmpty()
                } else {
                    _errorMessage.value = "Error al cargar registros de auditoría"
                    _isEmpty.value = true
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                _isEmpty.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterLogs(query: String) {
        val allLogs = _allLogs.value ?: return

        if (query.isBlank()) {
            _filteredLogs.value = allLogs
            _isEmpty.value = allLogs.isEmpty()
            return
        }

        val filtered = allLogs.filter { log ->
            log.usuario.contains(query, ignoreCase = true) ||
                    log.accion.contains(query, ignoreCase = true) ||
                    log.registro.contains(query, ignoreCase = true)
        }

        _filteredLogs.value = filtered
        _isEmpty.value = filtered.isEmpty()
    }

    fun clearError() {
        _errorMessage.value = ""
    }
}
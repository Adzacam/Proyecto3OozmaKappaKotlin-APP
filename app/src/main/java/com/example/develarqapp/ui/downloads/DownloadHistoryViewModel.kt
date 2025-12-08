package com.example.develarqapp.ui.downloads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.develarqapp.data.model.DownloadRecord
import com.example.develarqapp.data.repository.DownloadHistoryRepository
import kotlinx.coroutines.launch

class DownloadHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadHistoryRepository(application)

    private val _downloads = MutableLiveData<List<DownloadRecord>>()
    val downloads: LiveData<List<DownloadRecord>> = _downloads

    private val _filteredDownloads = MutableLiveData<List<DownloadRecord>>()
    val filteredDownloads: LiveData<List<DownloadRecord>> = _filteredDownloads

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    fun loadDownloadHistory() {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.getDownloadHistory()

            result.onSuccess { downloadList ->
                _downloads.value = downloadList
                _filteredDownloads.value = downloadList
                _isEmpty.value = downloadList.isEmpty()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Error al cargar historial"
                _downloads.value = emptyList()
                _filteredDownloads.value = emptyList()
                _isEmpty.value = true
            }

            _isLoading.value = false
        }
    }

    fun filterDownloads(
        userId: Long? = null,
        projectId: Long? = null,
        searchQuery: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            // 1. Obtener datos filtrados por ID desde el servidor
            val result = if (userId != null || projectId != null) {
                repository.getFilteredHistory(userId = userId, projectId = projectId)
            } else {
                // Si no hay filtros de ID, usamos la lista completa (o recargamos)
                repository.getDownloadHistory()
            }

            // 2. Aplicar filtro de búsqueda de texto localmente
            result.onSuccess { list ->
                val filtered = if (searchQuery.isNotBlank()) {
                    list.filter { doc ->
                        doc.documento.contains(searchQuery, ignoreCase = true) ||
                                doc.usuario.contains(searchQuery, ignoreCase = true) ||
                                doc.proyecto?.contains(searchQuery, ignoreCase = true) == true
                    }
                } else {
                    list
                }

                _filteredDownloads.value = filtered
                _isEmpty.value = filtered.isEmpty()

                // Actualizamos la lista base solo si no filtramos por ID (para mantener consistencia)
                if (userId == null && projectId == null) {
                    _downloads.value = list
                }

            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Error al filtrar"
                _filteredDownloads.value = emptyList()
                _isEmpty.value = true
            }

            _isLoading.value = false
        }
    }

    fun clearFilters() {
        loadDownloadHistory() // Recargar
    }

    fun clearError() {
        _errorMessage.value = ""
    }
}